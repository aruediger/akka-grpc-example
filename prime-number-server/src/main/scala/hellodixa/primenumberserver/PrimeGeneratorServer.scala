package hellodixa.primenumberserver

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.{ Failure, Success }
import scala.concurrent.duration._

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.http.scaladsl._
import com.typesafe.config.ConfigFactory

import hellodixa.grpc._

object PrimeGeneratorServer extends App {
  // important to enable HTTP/2 in ActorSystem's config
  val conf = ConfigFactory
    .parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())
    .resolve
  val system = ActorSystem[Nothing](Behaviors.empty, "PrimeGeneratorServer", conf)
  new PrimeGeneratorServer(system).run()
}

class PrimeGeneratorServer(system: ActorSystem[_]) {
  val log = org.slf4j.LoggerFactory.getLogger(getClass)

  def run(): Future[Http.ServerBinding] = {
    implicit val sys                  = system
    implicit val ec: ExecutionContext = system.executionContext

    val service = PrimeGeneratorServiceHandler(new PrimeGeneratorServiceImpl)
    val conf    = system.settings.config.getConfig("hellodixa.prime-number-server")
    val binding = Http(system)
      .newServerAt(conf.getString("bind-host"), conf.getInt("bind-port"))
      .bind(service)
      .map(_.addToCoordinatedShutdown(hardTerminationDeadline = 10.seconds))

    binding.onComplete {
      case Success(binding) =>
        val address = binding.localAddress
        log.info(
          s"PrimeGeneratorServer bound to ${address.getHostString}:${address.getPort}"
        )
      case Failure(ex) =>
        log.error(s"Failed to bind PrimeGeneratorServer gRPC endpoint, terminating system: $ex")
        system.terminate()
    }
    binding
  }
}
