package hellogrpc.primenumberserver

import scala.concurrent.duration._

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors
import akka.grpc.GrpcClientSettings
import akka.stream.scaladsl._
import com.google.protobuf.wrappers._
import com.typesafe.config.ConfigFactory
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import hellogrpc.grpc._

class PrimeGeneratorSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  val conf = ConfigFactory
    .parseString("akka.http.server.preview.enable-http2 = on")
    .withFallback(ConfigFactory.defaultApplication())
    .resolve

  val testKit = ActorTestKit(conf)

  val server = new PrimeGeneratorServer(testKit.system).run()
  // make sure server is bound before using client
  server.futureValue

  implicit val clientSystem: ActorSystem[_] = ActorSystem(Behaviors.empty, "PrimeGeneratorClient")
  val clientSettings =
    GrpcClientSettings
      .connectToServiceAt("127.0.0.1", 8080)
      .withTls(false)
  val client = PrimeGeneratorServiceClient(clientSettings)

  override def afterAll: Unit = {
    ActorTestKit.shutdown(clientSystem)
    testKit.shutdownTestKit()
  }

  "PrimeGeneratorService" should {
    "reply to primes request" in {
      val reply = client.primes(UInt64Value(20)).map(_.value).runWith(Sink.seq)
      reply.futureValue should ===(List(2, 3, 5, 7, 11, 13, 17, 19))
    }
  }
}
