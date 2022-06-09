lazy val akkaHttpVersion = "10.2.9"
lazy val akkaVersion = "2.6.19"

lazy val common = ProjectRef(base = file("../common"), id = "common")

lazy val root = (project in file("."))
  .dependsOn(common)
  .settings(
    inThisBuild(List(
      organization := "ru.misis",
      scalaVersion := "2.13.4"
    )),
    name := "orders",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
      "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream" % akkaVersion,
      "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
      "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0",
      "ch.qos.logback" % "logback-classic" % "1.2.3",
      "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "7.17.2",
      "io.scalaland" %% "chimney" % "0.6.1",

      "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
      "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
      "org.scalatest" %% "scalatest" % "3.1.4" % Test
    )
  )

enablePlugins(JavaAppPackaging)
