package hellodixa.grpc

import akka.stream.scaladsl._
import com.google.protobuf.wrappers._

import hellodixa.math.Prime

class PrimeGeneratorServiceImpl extends hellodixa.grpc.PrimeGeneratorService {

  override def primes(in: UInt64Value): Source[UInt64Value, akka.NotUsed] =
    Source(Prime.primes).takeWhile(_ <= in.value).map(n => UInt64Value(n))
}
