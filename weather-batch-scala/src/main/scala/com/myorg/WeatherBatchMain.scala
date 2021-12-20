package com.myorg

import com.typesafe.config.ConfigFactory
import io.circe
import io.circe.config.syntax._
import io.circe.generic.auto._
import wvlet.airframe.{DesignWithContext, newDesign}
import wvlet.log.LogSupport

object WeatherBatchMain extends LogSupport {

  def main(args: Array[String]): Unit = {
    info("app start.")
    val typetalkSettings: TypetalkSettings = loadConfig() match {
      case Left(e)      => throw e
      case Right(value) => value
    }

    val design = bindDesign(typetalkSettings)

    design.build[WeatherBatchService] { service =>
      service.run() match {
        case Left(e)  => throw e
        case Right(_) =>
      }
    }
    info("app end.")
  }

  private def loadConfig(): Either[circe.Error, TypetalkSettings] = {
    val config = ConfigFactory.load()

    config.as[TypetalkSettings]("typetalk")
  }

  private def bindDesign(typetalkSettings: TypetalkSettings): DesignWithContext[WeatherBatchService] = {
    newDesign
      .bind[TypetalkSettings].toInstance(typetalkSettings)
      .bind[WeatherBatchService].toSingleton
  }
}
