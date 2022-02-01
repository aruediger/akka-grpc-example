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

lazy val root = (project in file("."))

addCommandAlias("fmt", ";scalafmtAll;scalafmtSbt")
addCommandAlias("validate", ";clean;scalafmtCheckAll;scalafmtSbtCheck;test")
