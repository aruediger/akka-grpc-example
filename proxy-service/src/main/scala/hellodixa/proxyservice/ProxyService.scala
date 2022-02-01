package hellodixa.proxyservice

import java.security.{ KeyStore, SecureRandom }
import java.security.cert.{ Certificate, CertificateFactory }

import scala.io.Source
import scala.concurrent.{ ExecutionContext }
import scala.util.{ Failure, Success, Try }

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
  val log = org.slf4j.LoggerFactory.getLogger(getClass)

  def run(): Unit = {
    implicit val ec: ExecutionContext = system.executionContext

    val route = ProxyRoute(primeGenerator)
    val bound: Try[Http.ServerBinding] => Unit = {
      case Success(binding) =>
        val address = binding.localAddress
        log.info(
          s"ProxyService bound to ${address.getHostString}:${address.getPort}"
        )
      case Failure(ex) =>
        log.error(s"Failed to bind ProxyService endpoint, terminating system: $ex")
        system.terminate()
    }
    Http().newServerAt("0.0.0.0", 8080).bind(route).onComplete(bound)
    Http().newServerAt("0.0.0.0", 443).enableHttps(connectionCtx).bind(route).onComplete(bound)
  }

  private def connectionCtx: HttpsConnectionContext = {
    val privateKey = DERPrivateKeyLoader.load(PEMDecoder.decode(readPrivateKeyPem()))
    val fact       = CertificateFactory.getInstance("X.509")
    val cer = fact.generateCertificate(
      classOf[ProxyService].getResourceAsStream("/certs/server1.pem")
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

  private def readPrivateKeyPem(): String = Source.fromResource("certs/server1.key").mkString
}
