package hellodixa.math

import org.scalatest.funsuite.AnyFunSuite

class PrimeSpec extends AnyFunSuite {
  test("generate Primes") {
    assert(Prime.primes.takeWhile(_ < 20) === List(2, 3, 5, 7, 11, 13, 17, 19))
    assert(Prime.primes.take(1000).forall(Prime.is))
  }
}
