# Dixa Backend Engineer test

## TLDR

Run

```sh
$ sbt docker:publishLocal && docker compose up
```

to start the server. (This requires that the docker service is running.)

It can be tested by

```sh
$ curl http://localhost/prime/20
```
or

```sh
$ curl -k https://localhost/prime/20
````

respectively.

## Project structure

The project build is defined with [SBT](https://www.scala-sbt.org/) and consists of the following subprojects:

### `math`

Contains the "business logic" for generating a lazy stream of prime numbers.

### `grpc`

Defines the gRPC service [interface](grpc/src/main/protobuf/primes.proto) from which the Akka gRPC plugin generates client and server stubs. Also contains the service implementation and therefore depends on `math`. Akka gRPC provides the gRPC runtime.

### `prime-number-server`

Serves the gRPC service via Akka HTTP. Includes a test that spawns the server and instantiates a client.

Can be run with

```sh
$ sbt "project prime-number-server" "runMain hellodixa.primenumberserver.PrimeGeneratorServer"
```

To run via docker:
```sh
$ sbt docker:publishLocal && docker run -p 8080:8080 hellodixa/prime-number-server
```

#### Configration

The service can be configured by setting environment variables:

- `BIND_HOST`: the host that the service is bound to
- `BIND_PORT`: the port that the service is bound to

### `proxy-service`

Provides an Akka HTTP endpoint which forwards calls to the `prime-number-server`.

Includes tests for the route that uses an in-process prime server implementation instead of the gRPC client as well as failure tests with stubed clients referencing non-existing endpoints.

 Can be run with

```sh
$ sbt "project proxy-service" "runMain hellodixa.proxyservice.ProxyService"
```

To run via docker:
```sh
$ sbt docker:publishLocal && docker run -p 8080:8080 hellodixa/proxy-server
```

#### Configration

The service can be configured by setting environment variables:

- `BIND_HOST`: the host that the service is bound to
- `BIND_PORT`: the port that the service is bound to
- `BIND_PORT_TLS`: the TLS port that the service is bound to
- `PRIMEGENERATOR_GRPC_HOST`: host of the target gRPC server
- `PRIMEGENERATOR_GRPC_PORT`: port of the target gRPC server

## Testing

Unit tests can be run with

```sh
$ sbt test
```

A full validation which also checks formatting can be run with

```sh
$ sbt validate
```
