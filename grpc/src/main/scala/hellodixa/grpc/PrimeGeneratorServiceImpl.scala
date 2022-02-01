package hellodixa.grpc

import akka.actor.typed.ActorSystem
import akka.stream.scaladsl._
import com.google.protobuf.wrappers._

import hellodixa.math.Prime

class PrimeGeneratorServiceImpl(system: ActorSystem[_])
    extends hellodixa.grpc.PrimeGeneratorService {

  override def primes(in: UInt64Value): Source[UInt64Value, akka.NotUsed] =
    Source(Prime.primes).takeWhile(_ <= in.value).map(n => UInt64Value(n))
}
