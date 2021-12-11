name := "weather-batch-scala"

version := "0.1"

scalaVersion := "2.13.7"

val airFrameVersion = "21.12.0"
libraryDependencies += "org.wvlet.airframe" %% "airframe-launcher" % airFrameVersion

enablePlugins(PackPlugin)
packMain := Map("weather-batch" -> "com.myorg.Main")
