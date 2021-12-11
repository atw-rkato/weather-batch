package com.myorg

import com.typesafe.config.ConfigFactory
import io.circe.config.syntax._
import io.circe.generic.auto._
import wvlet.airframe.newDesign
import wvlet.log.LogSupport

object WeatherBatchMain extends App with LogSupport {
  val config = ConfigFactory.load()

  val typetalkSettings = config.as[TypetalkSettings]("typetalk") match {
    case Left(e) =>
      error(e.getMessage)
      sys.exit(1)
    case Right(value) => value
  }

  val design = newDesign
    .bind[TypetalkSettings].toInstance(typetalkSettings)
    .bind[WeatherBatchService].toSingleton

  design.build[WeatherBatchService] { service =>
    service.run() match {
      case Left(e)  => error(e.getMessage)
      case Right(_) =>
    }
  }
}
