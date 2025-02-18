ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

lazy val root = (project in file("."))
  .settings(
    name := "pak-media-worker",
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-dsl" % "0.23.30",
      "org.http4s" %% "http4s-blaze-server" % "0.23.17",
      "org.http4s" %% "http4s-circe" % "0.23.30",
      "org.http4s" %% "http4s-ember-client" % "0.23.30",
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "com.github.nscala-time" %% "nscala-time" % "3.0.0",
      "com.lihaoyi" %% "os-lib" % "0.11.4",

      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.typelevel" %% "cats-effect" % "3.5.7" % Test,
      "org.scalatestplus" %% "mockito-5-10" % "3.2.18.0" % Test,
    ),
    scalacOptions += "-Ykind-projector"
  )
