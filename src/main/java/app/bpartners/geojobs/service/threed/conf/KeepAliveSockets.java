package app.bpartners.geojobs.service.threed.conf;

import static jdk.net.ExtendedSocketOptions.TCP_KEEPCOUNT;
import static jdk.net.ExtendedSocketOptions.TCP_KEEPIDLE;
import static jdk.net.ExtendedSocketOptions.TCP_KEEPINTERVAL;

import java.io.IOException;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketOption;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * TCP keepalive on the connections to threed. A 3D generation can run for minutes without a byte on
 * the connection, and the network of a Lambda attached to a VPC drops a connection idle for ~350s
 * without telling either side: threed finishes and answers, but the answer never reaches geo-jobs,
 * which waits until its read timeout. Keepalive probes every minute keep the connection alive.
 *
 * <p>Set per socket because a Lambda can not change the kernel defaults (sysctl), whose first probe
 * only comes after 2 hours.
 */
@Slf4j
final class KeepAliveSockets {
  static final int IDLE_SECONDS = 60;
  static final int INTERVAL_SECONDS = 30;
  static final int PROBES = 4;

  private KeepAliveSockets() {}

  static Socket configure(Socket socket) throws IOException {
    socket.setKeepAlive(true);
    setIfSupported(socket, TCP_KEEPIDLE, IDLE_SECONDS);
    setIfSupported(socket, TCP_KEEPINTERVAL, INTERVAL_SECONDS);
    setIfSupported(socket, TCP_KEEPCOUNT, PROBES);
    return socket;
  }

  private static void setIfSupported(Socket socket, SocketOption<Integer> option, int value)
      throws IOException {
    if (socket.supportedOptions().contains(option)) {
      socket.setOption(option, value);
    } else {
      log.warn("{} not supported on this platform, keepalive uses the system default", option);
    }
  }

  static final class PlainSocketFactory extends PlainConnectionSocketFactory {
    @Override
    public Socket createSocket(HttpContext context) throws IOException {
      return configure(super.createSocket(context));
    }

    @Override
    public Socket createSocket(Proxy proxy, HttpContext context) throws IOException {
      return configure(super.createSocket(proxy, context));
    }
  }

  /** TLS is layered over the plain socket created here, so the options apply to https too. */
  static final class SslSocketFactory extends SSLConnectionSocketFactory {
    SslSocketFactory(SSLContext sslContext) {
      super(sslContext);
    }

    @Override
    public Socket createSocket(HttpContext context) throws IOException {
      return configure(super.createSocket(context));
    }

    @Override
    public Socket createSocket(Proxy proxy, HttpContext context) throws IOException {
      return configure(super.createSocket(proxy, context));
    }
  }
}
