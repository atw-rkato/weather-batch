package com.myorg

import com.typesafe.config.ConfigFactory
import io.circe.config.syntax._
import io.circe.generic.auto._
import wvlet.airframe.newDesign
import wvlet.log.LogSupport

object WeatherBatchMain extends LogSupport {

  def main(args: Array[String]): Unit = {
    info("app start.")
    info("configuration read start.")
    val config = ConfigFactory.load()

    val typetalkSettings = config.as[TypetalkSettings]("typetalk") match {
      case Left(e)      => throw e
      case Right(value) => value
    }

    info("configuration read end.")

    val design = newDesign
      .bind[TypetalkSettings].toInstance(typetalkSettings)
      .bind[WeatherBatchService].toSingleton

    design.build[WeatherBatchService] { service =>
      info("task start.")
      service.run() match {
        case Left(e)  => throw e
        case Right(_) => info("task end.")
      }
    }
    info("app end.")
  }
}
