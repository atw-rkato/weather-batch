package com.myorg.weather.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.enterprise.context.ApplicationScoped;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class WeatherBatchService {

    public record Forecast(
      String publishingOffice,
      OffsetDateTime reportDatetime,
      DailyForecast todayForecast,
      DailyForecast tomorrowForecast,
      DailyForecast dayAfterTomorrowForecast
    ) {
    }

    public record DailyForecast(OffsetDateTime timeDefine, String content) {
    }

    private static final String WEATHER_URL = "https://www.jma.go.jp/bosai/forecast/data/forecast";
    private static final String YOKOHAMA_OFFICE_CODE = "140000";
    private static final String TYPETALK_TOPIC_URL = "https://typetalk.com/api/v1/topics/259767";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH時", Locale.JAPANESE);
    private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("dd日 (EEE) ", Locale.JAPANESE);

    private final HttpClient client = HttpClient.newHttpClient();

    private final String typetalkToken;

    public WeatherBatchService(@ConfigProperty(name = "typetalk.token") String typetalkToken) {
        this.typetalkToken = typetalkToken;
    }

    public void run() throws IOException, InterruptedException {
        var weatherJson = fetchYokohamaWeather();
        var aaa = extractYokohamaData(weatherJson);
        sendToTypetalk(aaa);
    }

    private JsonNode fetchYokohamaWeather() throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
          .uri(URI.create(WEATHER_URL + "/" + YOKOHAMA_OFFICE_CODE + ".json"))
          .build();

        var response = this.client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        return mapper.readTree(response.body());
    }

    private Forecast extractYokohamaData(JsonNode weatherJson) {
        var details = weatherJson.get(0);
        var publishingOffice = details.get("publishingOffice").asText();
        var reportDatetime = OffsetDateTime.parse(details.get("reportDatetime").asText());
        var timeSeries = details.get("timeSeries").get(0);
        var timeDefines = timeSeries.get("timeDefines");
        var weathers = timeSeries.get("areas").get(0).get("weathers");

        var todayForecast = new DailyForecast(
          OffsetDateTime.parse(timeDefines.get(0).asText()),
          weathers.get(0).asText()
        );
        var tomorrowForecast = new DailyForecast(
          OffsetDateTime.parse(timeDefines.get(1).asText()),
          weathers.get(1).asText()
        );
        var dayAfterTomorrowForecast = new DailyForecast(
          OffsetDateTime.parse(timeDefines.get(2).asText()),
          weathers.get(2).asText()
        );
        return new Forecast(publishingOffice, reportDatetime, todayForecast, tomorrowForecast, dayAfterTomorrowForecast);
    }

    private void sendToTypetalk(Forecast forecast) throws IOException, InterruptedException {
        var todayForecast = forecast.todayForecast();
        var tomorrowForecast = forecast.tomorrowForecast();
        var dayAfterTomorrowForecast = forecast.dayAfterTomorrowForecast();
        var message = """
          横浜の天気
          %s %s 発表 (気象庁より)
          今日　 %s ：  %s
          明日　 %s ：  %s
          明後日 %s ：  %s
          """
          .formatted(
            dateTimeFormat.format(forecast.reportDatetime()), forecast.publishingOffice(),
            dateFormat.format(todayForecast.timeDefine()), todayForecast.content(),
            dateFormat.format(tomorrowForecast.timeDefine()), tomorrowForecast.content(),
            dateFormat.format(dayAfterTomorrowForecast.timeDefine()), dayAfterTomorrowForecast.content()
          );

        var json = mapper.writeValueAsString(Map.of("message", message));
        var request = HttpRequest.newBuilder()
          .uri(URI.create(TYPETALK_TOPIC_URL))
          .header("Content-Type", "application/json")
          .header("X-TYPETALK-TOKEN", this.typetalkToken)
          .POST(HttpRequest.BodyPublishers.ofString(json))
          .build();

        this.client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
