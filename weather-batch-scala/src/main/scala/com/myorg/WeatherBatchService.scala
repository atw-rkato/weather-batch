package com.myorg

import io.circe.syntax._
import io.circe._

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.util.{Failure, Success, Try}

case class Forecast(
  publishingOffice: String,
  reportDatetime: OffsetDateTime,
  todayForecast: DailyForecast,
  tomorrowForecast: DailyForecast,
  dayAfterTomorrowForecast: DailyForecast,
)

case class DailyForecast(timeDefine: OffsetDateTime, content: String)

object WeatherBatchService {
  private val WEATHER_URL          = "https://www.jma.go.jp/bosai/forecast/data/forecast"
  private val YOKOHAMA_OFFICE_CODE = "140000"
  private val TYPETALK_TOPIC_URL   = "https://typetalk.com/api/v1/topics/259767"

  private val dateTimeFormat = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH時", Locale.JAPANESE)
  private val dateFormat     = DateTimeFormatter.ofPattern("dd日 (EEE) ", Locale.JAPANESE)
}

class WeatherBatchService(typetalkSettings: TypetalkSettings) {

  import WeatherBatchService._

  private val client = HttpClient.newHttpClient

  def run(): Either[Throwable, Unit] = {
    for {
      weatherJson <- fetchYokohamaWeather
      forecast    <- extractYokohamaData(weatherJson)
    } yield sendToTypetalk(forecast)
  }

  private def fetchYokohamaWeather: Either[Throwable, Json] = {
    val request = HttpRequest.newBuilder
      .uri(URI.create(WEATHER_URL + "/" + YOKOHAMA_OFFICE_CODE + ".json"))
      .build
    val response = this.client.send(request, HttpResponse.BodyHandlers.ofString)
    parser.parse(response.body)
  }

  private def extractYokohamaData(weatherJson: Json): Either[Throwable, Forecast] = {
    val c           = weatherJson.hcursor
    val details     = c.downN(0)
    val timeSeries  = details.downField("timeSeries").downN(0)
    val timeDefines = timeSeries.downField("timeDefines")
    val weathers    = timeSeries.downField("areas").downN(0).downField("weathers")
    val getForecast = (i: Int) => {
      for {
        dateString <- timeDefines.downN(i).as[String]
        date       <- Try(OffsetDateTime.parse(dateString)).toEither
        content    <- weathers.downN(i).as[String]
      } yield DailyForecast(date, content)
    }

    for {
      publishingOffice         <- details.downField("publishingOffice").as[String]
      reportDatetime           <- details.downField("reportDatetime").as[String].map(OffsetDateTime.parse)
      todayForecast            <- getForecast(0)
      tomorrowForecast         <- getForecast(1)
      dayAfterTomorrowForecast <- getForecast(2)
    } yield Forecast(
      publishingOffice,
      reportDatetime,
      todayForecast,
      tomorrowForecast,
      dayAfterTomorrowForecast,
    )
  }

  private def sendToTypetalk(forecast: Forecast): Unit = {
    val todayForecast            = forecast.todayForecast
    val tomorrowForecast         = forecast.tomorrowForecast
    val dayAfterTomorrowForecast = forecast.dayAfterTomorrowForecast

    val message =
      s"""横浜の天気
         |${dateTimeFormat.format(forecast.reportDatetime)} ${forecast.publishingOffice} 発表 (気象庁より)
         |今日　 ${dateFormat.format(todayForecast.timeDefine)} ：  ${todayForecast.content}
         |明日　 ${dateFormat.format(tomorrowForecast.timeDefine)} ：  ${tomorrowForecast.content}
         |明後日 ${dateFormat.format(dayAfterTomorrowForecast.timeDefine)} ：  ${dayAfterTomorrowForecast.content}
      """.stripMargin
    val json = Map("message" -> message).asJson.noSpaces
    val request = HttpRequest.newBuilder
      .uri(URI.create(TYPETALK_TOPIC_URL))
      .header("Content-Type", "application/json")
      .header("X-TYPETALK-TOKEN", typetalkSettings.token)
      .POST(HttpRequest.BodyPublishers.ofString(json))
      .build
    this.client.send(request, HttpResponse.BodyHandlers.ofString)

    ()
  }
}
