name := "EShop"

version := "0.3"

scalaVersion := "2.13.6"

val akkaVersion = "2.6.16"


libraryDependencies ++= Seq(
  "com.typesafe.akka"         %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka"         %% "akka-actor-typed"       % akkaVersion,
  "org.scalatest"             %% "scalatest"              % "3.2.9" % "test",
  "org.iq80.leveldb"          % "leveldb"                 % "0.12",
  "org.fusesource.leveldbjni" % "leveldbjni-all"          % "1.8",
  "ch.qos.logback"            % "logback-classic"         % "1.2.6"
)

// scalaFmt
scalafmtOnCompile := true
