scalaVersion := crossScalaVersions.value.last

crossScalaVersions := Seq("2.10.6", "2.11.8")

libraryDependencies ++= Seq(
  "com.twitter" %% "util-eval" % "6.34.0",
  "org.scalatest" %% "scalatest" % "3.0.0" % "test"
)
