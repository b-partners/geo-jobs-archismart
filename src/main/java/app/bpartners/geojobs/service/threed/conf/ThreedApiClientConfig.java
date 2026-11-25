package app.bpartners.geojobs.service.threed.conf;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties
public class ThreedApiClientConfig {
  private final ThreedApiProperties properties;

  @Bean("threedRestTemplate")
  public RestTemplate threedRestTemplate() {
    return new RestTemplate(threedRequestFactory(properties));
  }

  @SneakyThrows
  static HttpComponentsClientHttpRequestFactory threedRequestFactory(
      ThreedApiProperties properties) {
    var socketFactories =
        RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", new KeepAliveSockets.PlainSocketFactory())
            .register(
                "https", new KeepAliveSockets.SslSocketFactory(SSLContexts.createSystemDefault()))
            .build();

    var connectionManager = new PoolingHttpClientConnectionManager(socketFactories);
    // the operator applies SocketConfig after the socket is created: keep SO_KEEPALIVE on
    connectionManager.setDefaultSocketConfig(SocketConfig.custom().setSoKeepAlive(true).build());
    connectionManager.setDefaultConnectionConfig(
        ConnectionConfig.custom()
            .setConnectTimeout(properties.getConnectTimeoutMs(), MILLISECONDS)
            .setSocketTimeout(properties.getReadTimeoutMs(), MILLISECONDS)
            // threed's Tomcat closes idle connections: check a pooled one before reusing it
            .setValidateAfterInactivity(2, SECONDS)
            .build());

    var httpClient =
        HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setResponseTimeout(Timeout.ofMilliseconds(properties.getReadTimeoutMs()))
                    .build())
            // a PUT /lrg resent after a network error would generate (and bill) the model twice
            .disableAutomaticRetries()
            .build();

    return new HttpComponentsClientHttpRequestFactory(httpClient);
  }
}
