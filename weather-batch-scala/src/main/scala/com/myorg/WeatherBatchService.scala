package com.myorg

import io.circe._
import sttp.client3._
import sttp.client3.httpclient.HttpClientSyncBackend
import sttp.model.MediaType

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import scala.util.Try

case class Forecast(
  publishingOffice: String,
  reportDatetime: OffsetDateTime,
  todayForecast: DailyForecast,
  tomorrowForecast: DailyForecast,
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

  private[this] val backend = HttpClientSyncBackend()

  def run(): Either[Throwable, Unit] = {
    for {
      weatherJson <- fetchYokohamaWeather
      forecast    <- extractYokohamaData(weatherJson)
      _           <- sendToTypetalk(forecast)
    } yield ()
  }

  private def fetchYokohamaWeather: Either[Throwable, Json] = {
    val request = basicRequest
      .get(uri"${WEATHER_URL}/${YOKOHAMA_OFFICE_CODE}.json")

    val response = request.send(backend)

    response.body.fold(
      err => Left(new IllegalStateException(err)),
      parser.parse,
    )
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
      publishingOffice     <- details.get[String]("publishingOffice")
      reportDatetimeString <- details.get[String]("reportDatetime")
      reportDatetime       <- Try(OffsetDateTime.parse(reportDatetimeString)).toEither
      todayForecast        <- getForecast(0)
      tomorrowForecast     <- getForecast(1)
    } yield Forecast(publishingOffice, reportDatetime, todayForecast, tomorrowForecast)
  }

  private def sendToTypetalk(forecast: Forecast): Either[Throwable, String] = {
    val todayForecast    = forecast.todayForecast
    val tomorrowForecast = forecast.tomorrowForecast

    val message =
      s"""横浜の天気
         |${dateTimeFormat.format(forecast.reportDatetime)} ${forecast.publishingOffice} 発表 (気象庁より)
         |今日 ${dateFormat.format(todayForecast.timeDefine)} ：  ${todayForecast.content}
         |明日 ${dateFormat.format(tomorrowForecast.timeDefine)} ：  ${tomorrowForecast.content}
         |""".stripMargin
    val json = Json.obj("message" -> Json.fromString(message)).noSpaces
    val request = basicRequest
      .post(uri"$TYPETALK_TOPIC_URL")
      .header("X-TYPETALK-TOKEN", typetalkSettings.token)
      .contentType(MediaType.ApplicationJson)
      .body(json)
    val response = request.send(backend)

    response.body.fold(
      err => Left(new IllegalStateException(err)),
      Right(_),
    )
  }
}
