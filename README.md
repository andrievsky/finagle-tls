Significant TLS performance degradation after switching to AdoptOpenJDK 11 or above. Checked on Debian and Mac. Reproducible both locally and using k8s cluster with official AdoptOpenJDK images.

Platform: Debian, Mac
Sbt: 1.3.12
Scala: 2.13.2, 2.12.8, 2.11.12
Finagle: 20.5.0

### Expected behavior

Similar CPU utilization in Java 8 and Java 11

### Actual behavior

CPU utilization was about **3 times** higher which also increase request processing time.
A profiler shows the issue directly related to TLS handling.

### Steps to reproduce the behavior

A service code processing TLS conection
```
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
```
Please checkout complete project from [https://github.com/andrievsky/finagle-tls](https://github.com/andrievsky/finagle-tls )

#### Java 8
`sdk use java 8.0.222.hs-adpt`
`sbt run`
`ab -n 10000 -c 10 https://localhost:8443/`

> Percentage of the requests served within a certain time (ms)
>   50%     43
>   66%     44
>   75%     45
>   80%     46
>   90%     49
>   95%     52
>   98%     56
>   99%     60
>  100%     81 (longest request)

<img width="1292" alt="Screenshot 2020-06-09 at 17 14 07" src="https://user-images.githubusercontent.com/3027200/84167327-5bb9fd80-aa76-11ea-93c5-9c2f4512612c.png">

#### Java 11
`sdk use java 11.0.7.hs-adpt`
`sbt run`
`ab -n 10000 -c 10 https://localhost:8443/`

> Percentage of the requests served within a certain time (ms)
>   50%    194
>   66%    200
>   75%    203
>   80%    205
>   90%    211
>   95%    216
>   98%    222
>   99%    227
>  100%    539 (longest request)

<img width="1291" alt="Screenshot 2020-06-09 at 17 19 30" src="https://user-images.githubusercontent.com/3027200/84167329-5ceb2a80-aa76-11ea-8433-5144452ef8c6.png">
