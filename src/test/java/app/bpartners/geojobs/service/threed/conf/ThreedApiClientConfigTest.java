package app.bpartners.geojobs.service.threed.conf;

import static java.nio.charset.StandardCharsets.UTF_8;
import static jdk.net.ExtendedSocketOptions.TCP_KEEPIDLE;
import static jdk.net.ExtendedSocketOptions.TCP_KEEPINTERVAL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.http.HttpMethod.PUT;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.hc.core5.ssl.SSLContexts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

class ThreedApiClientConfigTest {
  private HttpServer server;
  private final AtomicInteger received = new AtomicInteger();
  private RestTemplate subject;

  @BeforeEach
  void setUp() throws Exception {
    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    server.createContext(
        "/lrg/ok",
        exchange -> {
          received.incrementAndGet();
          var body = "{\"status\":\"NORMAL\"}".getBytes(UTF_8);
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.createContext(
        "/lrg/dropped",
        exchange -> {
          // closes the connection without an answer, like a connection dropped on the network
          received.incrementAndGet();
          exchange.close();
        });
    server.start();
    subject =
        new RestTemplate(
            ThreedApiClientConfig.threedRequestFactory(
                new ThreedApiProperties("http://localhost", "key")));
  }

  @AfterEach
  void tearDown() {
    server.stop(0);
  }

  @Test
  void plain_sockets_send_keepalive_probes_every_minute() throws Exception {
    try (var socket = new KeepAliveSockets.PlainSocketFactory().createSocket(null)) {
      assertTrue(socket.getKeepAlive());
      assertEquals(60, socket.getOption(TCP_KEEPIDLE));
      assertEquals(30, socket.getOption(TCP_KEEPINTERVAL));
    }
  }

  @Test
  void tls_sockets_send_keepalive_probes_every_minute() throws Exception {
    try (var socket =
        new KeepAliveSockets.SslSocketFactory(SSLContexts.createSystemDefault())
            .createSocket(null)) {
      assertTrue(socket.getKeepAlive());
      assertEquals(60, socket.getOption(TCP_KEEPIDLE));
    }
  }

  @Test
  void puts_and_reads_the_answer() {
    var actual =
        subject.exchange(url("/lrg/ok"), PUT, new HttpEntity<>("{}"), String.class).getBody();

    assertEquals("{\"status\":\"NORMAL\"}", actual);
    assertEquals(1, received.get());
  }

  @Test
  void does_not_resend_a_generation_when_the_connection_drops() {
    assertThrows(
        ResourceAccessException.class,
        () -> subject.exchange(url("/lrg/dropped"), PUT, new HttpEntity<>("{}"), String.class));

    assertEquals(1, received.get());
  }

  private String url(String path) {
    return "http://localhost:" + server.getAddress().getPort() + path;
  }
}
