ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "pak-media-worker",
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "com.github.nscala-time" %% "nscala-time" % "3.0.0",
      "com.lihaoyi" %% "os-lib" % "0.11.4",
      "io.circe" %% "circe-core" % "0.14.10",
      "io.circe" %% "circe-generic" % "0.14.10",
      "io.circe" %% "circe-parser" % "0.14.10",
      "dev.hnaderi" %% "lepus-client" % "0.5.4",
      "dev.hnaderi" %% "lepus-std" % "0.5.4",
      "dev.hnaderi" %% "lepus-circe" % "0.5.4",
      "software.amazon.awssdk" % "s3" % "2.30.29",

      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.typelevel" %% "cats-effect" % "3.5.7" % Test,
      "org.scalatestplus" %% "mockito-5-10" % "3.2.18.0" % Test,
      "org.testcontainers" % "rabbitmq" % "1.20.5" % Test,
      "org.testcontainers" % "testcontainers" % "1.20.5" % Test,

),
    scalacOptions += "-Ykind-projector"
  )
