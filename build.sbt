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
      "com.lihaoyi" %% "os-lib" % "0.9.0",
    ),
    scalacOptions += "-Ykind-projector"
  )
