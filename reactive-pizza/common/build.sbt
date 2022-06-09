
lazy val akkaVersion = "2.6.19"
lazy val akkaHttpVersion = "10.2.9"

lazy val common = (project in file("."))
    .settings(
        inThisBuild(List(
            organization := "ru.misis",
            scalaVersion := "2.13.4"
        )),
        name := "common",
        libraryDependencies ++= Seq(
            "com.typesafe.akka" %% "akka-stream" % akkaVersion,
            "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
            "com.typesafe.akka" %% "akka-stream-kafka" % "3.0.0",
            "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
            "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % "7.17.2",
        )
    )
