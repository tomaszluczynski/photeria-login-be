name := """photeria-login-be"""

version := "1.0-SNAPSHOT"

scalaVersion := "2.11.7"

lazy val root = project.in(file(".")).enablePlugins(PlayScala)


//fork in run := true

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "anorm" % "2.5.0",
  jdbc,
  filters,
  ws,
  "org.slf4j" % "slf4j-simple" % "1.7.21",
  "mysql" % "mysql-connector-java" % "5.1.34",
  "javax.inject" % "javax.inject" % "1"
)

routesGenerator := InjectedRoutesGenerator

