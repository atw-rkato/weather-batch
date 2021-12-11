package com.myorg

import wvlet.airframe.launcher._

class Main(
  @option(
    prefix = "-h,--help",
    description = "display help messages",
    isHelp = true,
  )
  help: Boolean = false,
  @option(prefix = "-p", description = "port number")
  port: Int = 8080,
) {

  @command(isDefault = true)
  def default(): Unit = {
    println(s"Hello airframe. port:${port}")
  }
}

object Main {
  def main(args: Array[String]): Unit = {
    Launcher.execute[Main](args)
  }
}
