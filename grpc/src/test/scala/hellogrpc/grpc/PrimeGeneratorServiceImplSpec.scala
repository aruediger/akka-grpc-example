package hellogrpc.grpc

import scala.concurrent.duration._

import akka.actor.testkit.typed.scaladsl.ActorTestKit
import akka.actor.typed.ActorSystem
import akka.stream.scaladsl._
import com.google.protobuf.wrappers._
import org.scalatest.BeforeAndAfterAll
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

class PrimeGeneratorServiceImplSpec
    extends AnyWordSpec
    with BeforeAndAfterAll
    with Matchers
    with ScalaFutures {

  val testKit = ActorTestKit()

  implicit val patience: PatienceConfig = PatienceConfig(scaled(5.seconds), scaled(100.millis))

  implicit val system: ActorSystem[_] = testKit.system

  val service = new PrimeGeneratorServiceImpl

  override def afterAll(): Unit = {
    testKit.shutdownTestKit()
  }

  "PrimeGeneratorServiceImpl" should {
    val primes =
      (n: Long) => service.primes(UInt64Value(n)).map(_.value).runWith(Sink.seq).futureValue

    "reply to zero upper bound" in {
      primes(0) should ===(List.empty)
    }
    "reply to negative upper bound" in {
      primes(-1) should ===(List.empty)
    }
    "reply to inclusive upper bound" in {
      primes(11) should ===(List(2, 3, 5, 7, 11))
    }
    "reply to exclusive upper bound" in {
      primes(12) should ===(List(2, 3, 5, 7, 11))
    }
  }
}
