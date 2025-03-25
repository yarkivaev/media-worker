ThisBuild / organization := "hirus"
ThisBuild / name := "pak-media-worker"
ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

enablePlugins(DockerPlugin)

lazy val root = (project in file("."))
  .settings(
    name := (ThisBuild / name).value,
    organization := (ThisBuild / organization).value,
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
      "io.minio" % "minio" % "8.3.4",
      "io.projectreactor" % "reactor-core" % "3.7.3",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.typelevel" %% "cats-effect" % "3.5.7" % Test,
      "org.scalatestplus" %% "mockito-5-10" % "3.2.18.0" % Test,
      "org.testcontainers" % "rabbitmq" % "1.20.5" % Test,
      "org.testcontainers" % "testcontainers" % "1.20.5" % Test,
      "org.junit.jupiter" % "junit-jupiter" % "5.8.1" % Test
    ),
    scalacOptions += "-Ykind-projector",
    docker / dockerfile := {
      val jarFile: File = (Compile / packageBin / sbt.Keys.`package`).value
      val classpath = (Compile / managedClasspath).value
      val mainclass = (Compile / packageBin / mainClass).value.getOrElse(sys.error("Expected exactly one main class"))
      val jarTarget = s"/app/${jarFile.getName}"
      val classpathString = classpath.files
        .map("/app/" + _.getName)
        .mkString(":") + ":" + jarTarget
      new Dockerfile {
        from("openjdk:17-jdk-slim")
        runRaw("apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*")
        add(classpath.files, "/app/")
        add(jarFile, jarTarget)
        entryPoint("java", "-cp", classpathString, mainclass)
      }
    },
    docker / imageNames := Seq(
      ImageName(s"${organization.value}/${name.value}:latest"),
      ImageName(
        namespace = Some(organization.value),
        repository = name.value,
        tag = Some("v" + version.value)
      )
    ),
    publishTo := Some(("Nexus Repository" at "http://212.67.12.16:8081/repository/maven-snapshots/").withAllowInsecureProtocol(true)),
    credentials += Credentials("Sonatype Nexus Repository Manager", "212.67.12.16", "yaroslav", "ii4aP7Du"), //"pak-service", "uFc7Fy6bCXQQ"),
    publishMavenStyle := true
  )

lazy val buildDockerBeforeTests = taskKey[Unit]("Build Docker image before running tests in integration module")

buildDockerBeforeTests := {
  println("Building Docker image before running tests in the integration module...")
  (root / docker).value
}

lazy val integration = (project in file("integration"))
  .dependsOn(root)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.dimafeng" %% "testcontainers-scala-core" % "0.43.0",
      "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.43.0",
      "com.dimafeng" %% "testcontainers-scala-rabbitmq" % "0.43.0",
      "org.scalatest" %% "scalatest" % "3.2.19",
      "com.github.kokorin.jaffree" % "jaffree" % "2024.08.29"
    ),
    (Test / test) := ((Test / test) dependsOn (root / buildDockerBeforeTests)).value,
    Compile / sourceGenerators += Def.task {
      val file = (Compile / sourceManaged).value / "generated" / "ProjectBuildInfo.scala"
      val content =
        s"""|package generated
            |
            |object ProjectBuildInfo {
            |  val organization: String = "${(ThisBuild / organization).value}"
            |  val name: String = "${(ThisBuild / name).value}"
            |  val version: String = "${if (version.value == "latest") version.value else "v" + version.value}"
            |}
            |""".stripMargin
      IO.write(file, content)
      Seq(file)
    }.taskValue
  )
