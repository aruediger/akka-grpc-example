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
  .settings(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor-typed"         % akkaVersion,
      "com.typesafe.akka" %% "akka-stream"              % akkaVersion,
      "com.typesafe.akka" %% "akka-discovery"           % akkaVersion,
      "com.typesafe.akka" %% "akka-pki"                 % akkaVersion,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "com.typesafe.akka" %% "akka-stream-testkit"      % akkaVersion % Test
    )
  )
lazy val root = (project in file("."))
  .aggregate(
    math,
    grpc,
    primeNumberServer
  )

addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt")
addCommandAlias("validate", ";clean;scalafmtCheckAll;scalafmtSbtCheck;test")
