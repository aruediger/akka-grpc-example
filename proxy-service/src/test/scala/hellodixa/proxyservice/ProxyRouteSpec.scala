package hellodixa.proxyservice

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.typesafe.config.ConfigFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import hellodixa.grpc._

class ProxyRouteSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  val route = ProxyRoute(new PrimeGeneratorServiceImpl)

  def clientConfig(conf: String) =
    ConfigFactory
      .parseString(conf)
      .withFallback(
        ConfigFactory
          .defaultApplication()
          .getConfig("akka.grpc.client.\"hellodixa.PrimeGeneratorService\"")
      )
      .withFallback(
        ConfigFactory
          .defaultReference()
          .getConfig("akka.grpc.client.\"*\"")
      )
      .resolve()

  "The service" should {
    "return prime numbers for GET requests to the prime path" in {
      Get("/prime/20") ~> route ~> check {
        responseAs[String] shouldEqual "2,3,5,7,11,13,17,19"
      }
    }
    "return errors for ivalid numbers" in {
      Get("/prime/x") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "return errors for ivalid paths" in {
      Get("/x") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.NotFound
      }
    }
    "handle unknown prime service host" in {
      val conf              = clientConfig("host = nowhere")
      val settings          = akka.grpc.GrpcClientSettings.fromConfig(conf)
      val unavailableClient = PrimeGeneratorServiceClient(settings)
      val route             = ProxyRoute(unavailableClient)
      Get("/prime/0") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.OK // because of streaming response
        responseAs[
          String
        ] shouldEqual "UNKNOWN: nowhere: nodename nor servname provided, or not known"
      }
    }
    "handle unbound prime service port" in {
      val conf              = clientConfig("host = localhost,port = 6666")
      val settings          = akka.grpc.GrpcClientSettings.fromConfig(conf)
      val unavailableClient = PrimeGeneratorServiceClient(settings)
      val route             = ProxyRoute(unavailableClient)
      Get("/prime/0") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.OK // because of streaming response
        responseAs[String] shouldEqual "UNAVAILABLE: io exception"
      }

    }
    "handle prime service errors" in {
      val faultyService = new PrimeGeneratorService {
        override def primes(
            in: com.google.protobuf.wrappers.UInt64Value
        ) = akka.stream.scaladsl.Source.failed(new RuntimeException("BOOM!"))
      }
      val route = ProxyRoute(faultyService)
      Get("/prime/0") ~> Route.seal(route) ~> check {
        status shouldEqual StatusCodes.OK // because of streaming response
        responseAs[String] shouldEqual "BOOM!"
      }
    }
  }
}
