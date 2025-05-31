ThisBuild / organization := "hirus"
ThisBuild / name := "pak-media-worker"
ThisBuild / version := "0.1.1-SNAPSHOT"

ThisBuild / scalaVersion := "3.3.5"

enablePlugins(DockerPlugin)

inThisBuild(
   List(
     scalaVersion := "3.3.5", // 2.13.16, 3.3.5 or 3.6.4
     organization := "hirus",
     name := "pak-media-worker",
     version := "0.1.1-SNAPSHOT",
     semanticdbEnabled := true,
   )
 )

lazy val domain = (project in file("domain"))
  .settings(
    name := (ThisBuild / name).value,
    organization := (ThisBuild / organization).value,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "com.lihaoyi" %% "os-lib" % "0.11.4",
      "io.circe" %% "circe-core" % "0.14.10",
      "io.circe" %% "circe-generic" % "0.14.10",
      "io.circe" %% "circe-parser" % "0.14.10",
      "dev.hnaderi" %% "lepus-client" % "0.5.4",
      "dev.hnaderi" %% "lepus-std" % "0.5.4",
      "dev.hnaderi" %% "lepus-circe" % "0.5.4",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.typelevel" %% "cats-effect" % "3.5.7" % Test,
    )
  )

lazy val server = (project in file("server"))
  .enablePlugins(DockerPlugin)
  .dependsOn(domain)
  .settings(
    name := (ThisBuild / name).value,
    organization := (ThisBuild / organization).value,
    Compile / mainClass := Some("medwork.server.Server"),
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
      "io.projectreactor" % "reactor-core" % "3.7.3", // Why do I need reactor?
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.8",
      "io.github.kirill5k" %% "mongo4cats-core" % "0.7.12",
      "io.github.kirill5k" %% "mongo4cats-circe" % "0.7.12",
      "io.github.kirill5k" %% "mongo4cats-embedded" % "0.7.12",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.typelevel" %% "cats-effect" % "3.5.7" % Test,
      "org.scalatestplus" %% "mockito-5-10" % "3.2.18.0" % Test,
      "org.testcontainers" % "rabbitmq" % "1.20.5" % Test,
      "org.testcontainers" % "mongodb" % "1.20.5" % Test,
      "org.testcontainers" % "testcontainers" % "1.20.5" % Test,
      "org.junit.jupiter" % "junit-jupiter" % "5.8.1" % Test
    ),
    scalacOptions ++= Seq(
      "-Ykind-projector",
      "-Wunused:imports"
    ),
    docker / dockerfile := {
      val domainJarFile: File = (domain / Compile / packageBin / sbt.Keys.`package`).value
      val jarFile: File = (Compile / packageBin / sbt.Keys.`package`).value
      val classpath = (Compile / managedClasspath).value
      val mainclass = (Compile / packageBin / mainClass).value.getOrElse(sys.error("Expected exactly one main class"))
      val domainJarTarget = s"/app/domain.jar"
      val jarTarget = s"/app/${jarFile.getName}"
      val classpathString = classpath.files
        .map("/app/" + _.getName)
        .mkString(":") + ":" + jarTarget + ":" + domainJarTarget
      new Dockerfile {
        from("openjdk:17-jdk-slim")
        runRaw("apt-get update && apt-get install -y --no-install-recommends ffmpeg && rm -rf /var/lib/apt/lists/*")
        add(classpath.files, "/app/")
        add(jarFile, jarTarget)
        add(domainJarFile, domainJarTarget)
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
    publishTo := Some(
      ("Nexus Repository" at "http://212.67.12.16:8081/repository/maven-snapshots/").withAllowInsecureProtocol(true)
    ),
    credentials += Credentials("Sonatype Nexus Repository Manager", "212.67.12.16", "pak-service", "uFc7Fy6bCXQQ"),
    publishMavenStyle := true,
    wartremoverErrors ++= Warts.allBut(
      Wart.Overloading, 
      Wart.Nothing, 
      Wart.MutableDataStructures,
      Wart.IterableOps,
      Wart.Throw,
      Wart.EitherProjectionPartial,
      Wart.PlatformDefault,
      Wart.Recursion,
      Wart.Var,
      Wart.AsInstanceOf,
      Wart.Null,
      Wart.OptionPartial,
      Wart.AutoUnboxing
      )
  )

lazy val client = (project in file("client"))
  .dependsOn(domain)
  .settings(
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.19" % Test,
      "org.junit.jupiter" % "junit-jupiter" % "5.8.1" % Test,
      "org.testcontainers" % "testcontainers" % "1.20.5" % Test,
      "org.testcontainers" % "rabbitmq" % "1.20.5" % Test,
      "org.scalatestplus" %% "mockito-5-10" % "3.2.18.0" % Test,
    ),
    publishTo := Some(
      ("Nexus Repository" at "http://212.67.12.16:8081/repository/maven-snapshots/").withAllowInsecureProtocol(true)
    ),
    credentials += Credentials("Sonatype Nexus Repository Manager", "212.67.12.16", "pak-service", "uFc7Fy6bCXQQ"),
    publishMavenStyle := true,
  )

lazy val integration = (project in file("integration"))
  .dependsOn(domain, server, client)
  .settings(
    publish / skip := true,
    libraryDependencies ++= Seq(
      "com.dimafeng" %% "testcontainers-scala-core" % "0.43.0",
      "com.dimafeng" %% "testcontainers-scala-scalatest" % "0.43.0",
      "com.dimafeng" %% "testcontainers-scala-rabbitmq" % "0.43.0",
      "com.dimafeng" %% "testcontainers-scala-minio" % "0.43.0",
      "org.scalatest" %% "scalatest" % "3.2.19",
      "com.github.kokorin.jaffree" % "jaffree" % "2024.08.29",
      "io.minio" % "minio" % "8.3.4" % Test,
    ),
    (Test / test) := ((Test / test) dependsOn (server / docker)).value,
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
