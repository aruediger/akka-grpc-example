ThisBuild / version      := "SNAPSHOT"
ThisBuild / scalaVersion := "2.12.15"
ThisBuild / scalacOptions ++= Seq(
  "-Xlint",
  "-Xfatal-warnings",
  "-feature",
  "-language:postfixOps",
  "-deprecation:true"
)
ThisBuild / libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest"       % "3.2.10" % Test
)

lazy val akkaVersion     = "2.6.18"
lazy val akkaHttpVersion = "10.2.7"

lazy val math = (project in file("math"))

lazy val grpc = (project in file("grpc"))
  .dependsOn(math)
  .enablePlugins(AkkaGrpcPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery"           % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
    )
  )

lazy val primeNumberServer = (project in file("prime-number-server"))
  .dependsOn(grpc)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test
    ),
    Docker / dockerBaseImage := "openjdk:8-slim",
    Docker / packageName     := "hellogrpc/prime-number-server",
    Docker / version         := "latest",
    Docker / dockerExposedPorts += 8080
  )

lazy val proxyService = (project in file("proxy-service"))
  .dependsOn(grpc)
  .enablePlugins(JavaAppPackaging)
  .enablePlugins(DockerPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe"       % "config"                   % "1.4.0",
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-http"                % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-pki"                 % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion     % Test,
      "com.typesafe.akka" %% "akka-http-testkit"        % akkaHttpVersion % Test
    ),
    Docker / dockerBaseImage := "openjdk:8-slim",
    Docker / packageName     := "hellogrpc/proxy-service",
    Docker / version         := "latest",
    Docker / dockerExposedPorts += 8080
  )

lazy val root = (project in file("."))
  .aggregate(
    math,
    grpc,
    primeNumberServer,
    proxyService
  )

addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt")
addCommandAlias("validate", ";clean;scalafmtCheckAll;scalafmtSbtCheck;test")
