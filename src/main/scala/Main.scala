import java.io.File

import com.twitter.finagle.ssl.KeyCredentials
import com.twitter.finagle.ssl.server.SslServerConfiguration
import com.twitter.finagle.{Http, Service, http}
import com.twitter.util.{Await, Future}

object Main extends App {
  val keyCredentials =
    KeyCredentials.CertKeyAndChain(
      new File("src/main/resources/localhost.crt"),
      new File("src/main/resources/localhost.key"),
      new File("src/main/resources/RootCA.pem"))

  val sslConfiguration = SslServerConfiguration(keyCredentials)

  val service = new Service[http.Request, http.Response] {
    def apply(req: http.Request): Future[http.Response] = {
      Future.value(http.Response(req.version, http.Status.Ok))
    }
  }

  val server = Http.server.withTransport.tls(sslConfiguration).serve(":8443", service)
  Await.ready(server)
}
