package hellogrpc.proxyservice

import akka.http.scaladsl.model.{ HttpEntity, ContentTypes }
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.util.ByteString
import com.google.protobuf.wrappers._

import hellogrpc.grpc._

object ProxyRoute {
  def apply(primeGenerator: PrimeGeneratorService) = {
    val primes = (max: Long) =>
      primeGenerator
        .primes(UInt64Value(max))
        .map(_.value.toString)
        .intersperse(",")

    Route.seal(
      pathPrefix("prime" / LongNumber) { max =>
        get {
          complete(
            HttpEntity(
              ContentTypes.`text/plain(UTF-8)`,
              primes(max)
                // a streaming response can't set the status code so we're at least emitting the error
                // message before closing the stream
                .recover { case e: RuntimeException => e.getMessage }
                .map(ByteString(_))
            )
          )
        }
      }
    )
  }
}
