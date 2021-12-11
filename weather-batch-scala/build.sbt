import org.scalafmt.sbt.ScalafmtPlugin.autoImport.scalafmtConfig

enablePlugins(PackPlugin)
packMain := Map("run-weather-batch-scala" -> "com.myorg.WeatherBatchMain")

lazy val root = (project in file("."))
  .settings(
    organization := "com.myorg",
    name := "weather-batch-scala",
    version := "0.1",
    scalaVersion := "2.13.7",
    scalacOptions ++= Seq(
      "-encoding",
      "UTF-8",
      "-deprecation",
      "-feature",
      "-unchecked",
      "-Xlint",
      "-Ywarn-dead-code",
      "-Ywarn-numeric-widen",
      "-Ywarn-value-discard",
    ),
    javacOptions ++= Seq("-source", "11", "-target", "11", "-encoding", "UTF-8"),
    scalafmtConfig := file(".scalafmt.conf"),
    libraryDependencies ++= dependencies,
  )

lazy val circeVersion    = "0.14.1"
lazy val airFrameVersion = "21.12.0"
lazy val dependencies = Seq(
  "org.wvlet.airframe" %% "airframe"      % airFrameVersion,
  "org.wvlet.airframe" %% "airframe-log"  % airFrameVersion,
  "io.circe"           %% "circe-core"    % circeVersion,
  "io.circe"           %% "circe-generic" % circeVersion,
  "io.circe"           %% "circe-parser"  % circeVersion,
  "io.circe"           %% "circe-config"  % "0.8.0",
)
