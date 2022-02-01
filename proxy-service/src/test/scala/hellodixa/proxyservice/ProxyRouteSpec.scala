package hellodixa.proxyservice

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.testkit.ScalatestRouteTest
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import hellodixa.grpc._

class ProxyRouteSpec extends AnyWordSpec with Matchers with ScalatestRouteTest {
  val testKit = ActorTestKit()
  val route   = ProxyRoute(new PrimeGeneratorServiceImpl)

  override def afterAll: Unit = {
    testKit.shutdownTestKit()
  }

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
    "handle prime service errors" in {
      val faultyRoute = ProxyRoute(new PrimeGeneratorService {
        override def primes(
            in: com.google.protobuf.wrappers.UInt64Value
        ) = akka.stream.scaladsl.Source.failed(new RuntimeException("BOOM!"))
      })
      Get("/prime/20") ~> Route.seal(faultyRoute) ~> check {
        status shouldEqual StatusCodes.OK // because of streaming response
        responseAs[String] shouldEqual "BOOM!"
      }
    }
  }
}
