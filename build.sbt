
name := "Kiosk"

version := "0.1"

updateOptions := updateOptions.value.withLatestSnapshots(false)

scalaVersion := "2.12.10"

lazy val EasyWeb = RootProject(uri("git://github.com/scalahub/EasyWeb.git"))

lazy val appkit = RootProject(uri("git://github.com/scalahub/appkit-mod.git"))

libraryDependencies ++= Seq(
  "org.scala-lang.modules" %% "scala-collection-compat" % "2.1.3",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.+",
  "com.typesafe.akka" %% "akka-actor" % "2.4.+",
  "org.bitbucket.inkytonik.kiama" %% "kiama" % "2.1.0",
  "com.lihaoyi" %% "fastparse" % "1.0.0",
  "org.scalacheck" %% "scalacheck" % "1.14.0" % Test
)

resolvers ++= Seq("Sonatype Releases" at "https://oss.sonatype.org/content/repositories/releases/",
  "SonaType" at "https://oss.sonatype.org/content/groups/public",
  "Typesafe maven releases" at "http://repo.typesafe.com/typesafe/maven-releases/",
  "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/")

lazy val root = (project in file(".")).dependsOn(
  EasyWeb, appkit
).settings(
  updateOptions := updateOptions.value.withLatestSnapshots(false),
  mainClass in (Compile, run) := Some("kiosk.KioskWeb"),
  assemblyMergeStrategy in assembly := {
    case PathList("reference.conf") => MergeStrategy.concat
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
)

enablePlugins(JettyPlugin)
