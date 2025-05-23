addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.6")
addSbtPlugin("se.marcuslonnberg" % "sbt-docker" % "1.11.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.12.2")
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.14.2")
addSbtPlugin("com.sksamuel.scapegoat" %% "sbt-scapegoat" % "1.2.12") 
addSbtPlugin("org.wartremover" % "sbt-wartremover" % "3.3.3")
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.3.1")

// Explicit dependency to prevent "Not a valid key: usePgpKeyHex" error
libraryDependencies += "com.github.sbt" % "pgp-library_2.12" % "2.3.1"
