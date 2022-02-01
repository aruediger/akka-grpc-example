package hellodixa.proxyservice

import akka.http.scaladsl.model.{ HttpEntity, ContentTypes }
import akka.http.scaladsl.server.Directives._
import akka.util.ByteString
import com.google.protobuf.wrappers._

import hellodixa.grpc._

object ProxyRoute {
  def apply(primeGenerator: PrimeGeneratorService) = {
    val primes = (max: Long) =>
      primeGenerator
        .primes(UInt64Value(max))
        .map(_.value.toString)
        .intersperse(",")

    pathPrefix("prime" / LongNumber) { max =>
      get {
        complete(
          HttpEntity(
            ContentTypes.`text/plain(UTF-8)`,
            primes(max)
              .recover { case e: RuntimeException => e.getMessage }
              .map(ByteString(_))
          )
        )
      }
    }
  }
}
