name := "Kiosk"

version := "0.1"

scalaVersion := "2.12.8"

lazy val EasyWeb = RootProject(uri("git://github.com/scalahub/EasyWeb.git"))
//lazy val EasyWeb = RootProject(uri("../EasyWeb"))

lazy val SigmaState = RootProject(uri("git://github.com/ScorexFoundation/sigmastate-interpreter.git"))
//lazy val SigmaState = RootProject(uri("../sigmastate-interpreter"))

lazy val CryptoNode = RootProject(uri("git://github.com/scalahub/CryptoNode.git"))
//lazy val CryptoNode = RootProject(uri("../CryptoNode"))

val bouncycastleBcprov = "org.bouncycastle" % "bcprov-jdk15on" % "1.60"
val scrypto            = "org.scorexfoundation" %% "scrypto" % "2.1.6"
val scorexUtil         = "org.scorexfoundation" %% "scorex-util" % "0.1.3"
val macroCompat        = "org.typelevel" %% "macro-compat" % "1.1.1"
val paradise           = "org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full

val specialVersion = "master-d1624dc1-SNAPSHOT"
val specialCommon  = "io.github.scalan" %% "common" % specialVersion
val specialCore    = "io.github.scalan" %% "core" % specialVersion
val specialLibrary = "io.github.scalan" %% "library" % specialVersion

val meta        = "io.github.scalan" %% "meta" % specialVersion
val plugin      = "io.github.scalan" %% "plugin" % specialVersion
val libraryapi  = "io.github.scalan" %% "library-api" % specialVersion
val libraryimpl = "io.github.scalan" %% "library-impl" % specialVersion
val libraryconf = "io.github.scalan" %% "library-conf" % specialVersion

libraryDependencies ++= Seq(
  scrypto,
  scorexUtil,
  "org.bouncycastle" % "bcprov-jdk15on" % "1.+",
  "com.typesafe.akka" %% "akka-actor" % "2.4.+",
  "org.bitbucket.inkytonik.kiama" %% "kiama" % "2.1.0",
  "com.lihaoyi" %% "fastparse" % "1.0.0",
)

resolvers ++= Seq("Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Typesafe maven releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/")

lazy val root = (project in file(".")).dependsOn(
  EasyWeb, SigmaState, CryptoNode
)
