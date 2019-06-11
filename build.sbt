name := "Kiosk"

version := "0.1"

scalaVersion := "2.12.8"

lazy val EasyWeb = RootProject(uri("https://github.com/scalahub/EasyWeb.git"))
//lazy val EasyWeb = RootProject(uri("../EasyWeb"))

lazy val root = (project in file(".")).dependsOn(
  EasyWeb
)