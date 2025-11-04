package app.bpartners.geojobs.endpoint.rest.readme.webhook;

import static java.lang.Long.parseLong;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochMilli;
import static java.util.Objects.isNull;
import static javax.crypto.Mac.getInstance;

import app.bpartners.geojobs.endpoint.rest.readme.webhook.exception.WebhookVerificationException;
import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.CreateWebhook;
import app.bpartners.geojobs.endpoint.rest.readme.webhook.model.SignatureWithTime;
import app.bpartners.geojobs.model.exception.ForbiddenException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReadmeWebhookValidator
    implements TriConsumer<CreateWebhook, HttpServletRequest, ReadmeWebhookConf> {
  private static final String SIGNATURE_HEADER_NAME = "readme-signature";
  private static final String WEBHOOK_EXPECTED_SCHEME = "v0";
  private static final String WEBHOOK_SIGNATURE_SEPARATOR = ",";
  private static final String WEBHOOK_ITEM_TIME_PREFIX = "t";
  private static final String WEBHOOK_ITEM_SEPARATOR = "=";
  private static final String CRYPTO_SHA_TYPE = "HmacSHA256";

  private final ObjectMapper objectMapper;
  private final ReadmeWebhookConf readmeWebhookConf;

  @Override
  public void accept(
      CreateWebhook createWebhook, HttpServletRequest request, ReadmeWebhookConf conf) {
    String signature = request.getHeader(SIGNATURE_HEADER_NAME);
    try {
      verifyWebhook(createWebhook, signature, readmeWebhookConf.getSecret());
    } catch (WebhookVerificationException e) {
      log.error(e.getMessage());
      throw new ForbiddenException("Webhook not valid");
    }
  }

  /**
   * docs: <a
   * href="https://github.com/readmeio/metrics-sdks/blob/main/packages/node/src/lib/verify-webhook.ts">...</a>
   */
  private void verifyWebhook(CreateWebhook body, String signature, String secret)
      throws WebhookVerificationException {
    if (isNull(signature) || signature.isEmpty()) {
      throw new WebhookVerificationException("Missing signature");
    }

    var now = now();
    var signatureValue = retrieveSignatureValueWithTime(signature);
    var signatureDateTime = ofEpochMilli(signatureValue.time());
    var thirtyMinutes = Duration.ofMinutes(30).toSeconds();

    if (signatureDateTime.isBefore(now)
        && Duration.between(signatureDateTime, now).getSeconds() > thirtyMinutes) {
      throw new WebhookVerificationException("Expired Signature");
    }

    try {
      String unsigned = signatureValue.time() + "." + objectMapper.writeValueAsString(body);
      String encryptedObject = calculateHmacSHA256(unsigned, secret);
      if (!encryptedObject.equals(signatureValue.signature())) {
        throw new WebhookVerificationException("Invalid Signature");
      }
    } catch (JsonProcessingException | NoSuchAlgorithmException | InvalidKeyException e) {
      throw new WebhookVerificationException(e.getMessage());
    }
  }

  public static String calculateHmacSHA256(String data, String secret)
      throws NoSuchAlgorithmException, InvalidKeyException {
    var mac = getInstance(CRYPTO_SHA_TYPE);
    var secretKeySpec = new SecretKeySpec(secret.getBytes(UTF_8), CRYPTO_SHA_TYPE);
    mac.init(secretKeySpec);

    byte[] hmacBytes = mac.doFinal(data.getBytes(UTF_8));
    var result = new StringBuilder();
    for (byte b : hmacBytes) {
      result.append(String.format("%02x", b)); // to hexadecimal with 2 characters
    }
    return result.toString();
  }

  private SignatureWithTime retrieveSignatureValueWithTime(String signature) {
    return Arrays.stream(signature.split(WEBHOOK_SIGNATURE_SEPARATOR))
        .map(item -> SignatureWithTime.builder().signature(item).build())
        .reduce(
            SignatureWithTime.builder().signature("").time(-1).build(), this::processSignatureItem);
  }

  private SignatureWithTime processSignatureItem(SignatureWithTime accum, SignatureWithTime item) {
    final var kv = item.signature().split(WEBHOOK_ITEM_SEPARATOR);
    long time = accum.time();
    String readmeSignature = accum.signature();

    if (kv[0].equals(WEBHOOK_ITEM_TIME_PREFIX)) {
      time = parseLong(kv[1]);
    } else if (kv[0].equals(WEBHOOK_EXPECTED_SCHEME)) {
      readmeSignature = kv[1];
    }

    return SignatureWithTime.builder().time(time).signature(readmeSignature).build();
  }
}
