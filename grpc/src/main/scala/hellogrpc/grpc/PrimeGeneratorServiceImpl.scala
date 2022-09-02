package hellogrpc.grpc

import akka.stream.scaladsl._
import com.google.protobuf.wrappers._

import hellogrpc.math.Prime

class PrimeGeneratorServiceImpl extends hellogrpc.grpc.PrimeGeneratorService {

  override def primes(in: UInt64Value): Source[UInt64Value, akka.NotUsed] =
    Source(Prime.primes).takeWhile(_ <= in.value).map(n => UInt64Value(n))
}
