package hellodixa.proxyservice

import java.security.{ KeyStore, SecureRandom }
import java.security.cert.{ Certificate, CertificateFactory }

import scala.io.Source
import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.http.scaladsl._
import akka.pki.pem.{ DERPrivateKeyLoader, PEMDecoder }
import javax.net.ssl.{ KeyManagerFactory, SSLContext }

import hellodixa.grpc._

object ProxyService extends App {
  implicit val system = ActorSystem(Behaviors.empty, "ProxyService")
  val settings        = GrpcClientSettings.fromConfig("hellodixa.PrimeGeneratorService")
  val client          = PrimeGeneratorServiceClient(settings)
  new ProxyService(client).run()
}

class ProxyService(primeGenerator: PrimeGeneratorService)(implicit system: ActorSystem[_]) {
  val log  = org.slf4j.LoggerFactory.getLogger(getClass)
  val conf = system.settings.config.getConfig("hellodixa.proxy-service")

  def run(): Future[Seq[Http.ServerBinding]] = {
    implicit val ec: ExecutionContext = system.executionContext

    val route = ProxyRoute(primeGenerator)
    val bindings = for {
      http <- Http()
        .newServerAt(conf.getString("bind-host"), conf.getInt("bind-port"))
        .bind(route)
      https <- Http()
        .newServerAt(conf.getString("bind-host"), conf.getInt("bind-port-tls"))
        .enableHttps(connectionCtx(conf.getString("cert-file"), conf.getString("key-file")))
        .bind(route)
    } yield Seq(http, https)
    bindings.onComplete {
      case Success(x) =>
        val addresses = x
          .map({ case Http.ServerBinding(addr) => s"${addr.getHostString}:${addr.getPort}" })
          .mkString(", ")
        log.info(
          s"ProxyService bound to ${addresses}."
        )
      case Failure(ex) =>
        log.error(s"Failed to bind ProxyService endpoint, terminating system: $ex")
        system.terminate()
    }
    bindings
  }

  private def connectionCtx(certFile: String, keyFile: String): HttpsConnectionContext = {
    val privateKey =
      DERPrivateKeyLoader.load(PEMDecoder.decode(Source.fromResource(keyFile).mkString))
    val fact = CertificateFactory.getInstance("X.509")
    val cer = fact.generateCertificate(
      classOf[ProxyService].getResourceAsStream(s"/$certFile")
    )
    val ks = KeyStore.getInstance("PKCS12")
    ks.load(null)
    ks.setKeyEntry("private", privateKey, new Array[Char](0), Array[Certificate](cer))
    val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
    keyManagerFactory.init(ks, null)
    val context = SSLContext.getInstance("TLS")
    context.init(keyManagerFactory.getKeyManagers, null, new SecureRandom)
    ConnectionContext.httpsServer(context)
  }
}
