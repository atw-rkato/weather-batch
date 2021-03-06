use anyhow::{Context, Result};
use chrono::{DateTime, FixedOffset};
use chrono_locale::LocaleDate;
use indoc::indoc;
use reqwest::{Client, Response};
use serde_json::{json, Value};

#[derive(Debug)]
struct Forecast {
    publishing_office: String,
    report_datetime: DateTime<FixedOffset>,
    today_forecast: DailyForecast,
    tomorrow_forecast: DailyForecast,
}

#[derive(Debug)]
struct DailyForecast {
    time_define: DateTime<FixedOffset>,
    content: String,
}

pub(crate) struct WeatherBatchService {
    typetalk_token: String,
    client: Client,
}

impl WeatherBatchService {
    const WEATHER_URL: &'static str = "https://www.jma.go.jp/bosai/forecast/data/forecast";
    const YOKOHAMA_OFFICE_CODE: &'static str = "140000";
    const TYPETALK_TOPIC_URL: &'static str = "https://typetalk.com/api/v1/topics/259767";

    pub fn new(typetalk_token: impl Into<String>) -> WeatherBatchService {
        WeatherBatchService { typetalk_token: typetalk_token.into(), client: Client::new() }
    }

    pub(crate) async fn run(self) -> Result<()> {
        let weather_json = self.fetch_yokohama_weather().await?;
        let forecast = self.extract_yokohama_data(&weather_json)?;
        self.send_to_typetalk(&forecast).await?;
        Ok(())
    }

    async fn fetch_yokohama_weather(&self) -> reqwest::Result<Value> {
        let resp = self.client
            .get(format!("{}/{}.json", Self::WEATHER_URL, Self::YOKOHAMA_OFFICE_CODE))
            .send()
            .await?;
        resp.json::<Value>().await
    }

    fn extract_yokohama_data(&self, weather_json: &Value) -> Result<Forecast> {
        let details = weather_json.get(0)
            .with_context(|| serde_json::to_string_pretty(weather_json).unwrap_or_else(|e| e.to_string()))?;
        let time_series = details.get("timeSeries").and_then(|v| v.get(0)).context("timeSeries")?;
        let time_defines = time_series.get("timeDefines").context("timeDefines")?;
        let weathers = time_series.get("areas")
            .and_then(|v| v.get(0))
            .and_then(|v| v.get("weathers"))
            .context("weathers")?;

        let get_forecast = |i: usize| -> Result<DailyForecast> {
            let date_string = time_defines.get(i).and_then(Value::as_str).context(i.to_string())?;
            let time_define = DateTime::parse_from_rfc3339(date_string)?;
            let content = weathers.get(i).and_then(Value::as_str).context(i.to_string())?;
            Ok(DailyForecast { time_define, content: content.to_string() })
        };

        let publishing_office = details.get("publishingOffice")
            .and_then(Value::as_str)
            .context("publishingOffice")?;
        let report_datetime_string = details.get("reportDatetime")
            .and_then(Value::as_str)
            .context("reportDatetime")?;
        let report_datetime = DateTime::parse_from_rfc3339(report_datetime_string)?;
        let today_forecast = get_forecast(0)?;
        let tomorrow_forecast = get_forecast(1)?;

        let forecast = Forecast { publishing_office: publishing_office.to_string(), report_datetime, today_forecast, tomorrow_forecast };
        Ok(forecast)
    }

    async fn send_to_typetalk(&self, Forecast { publishing_office, report_datetime, today_forecast, tomorrow_forecast }: &Forecast) -> reqwest::Result<Response> {
        let message = format!(
            indoc! {r#"
                ???????????????
                {report_datetime} {publishing_office} ?????? (???????????????)
                ?????? {today_forecast_time} ???  {today_forecast_content}
                ?????? {tomorrow_forecast_time} ???  {tomorrow_forecast_content}
            "#},
            report_datetime = report_datetime.formatl("%Y???%m???%d??? %H???", "ja"),
            publishing_office = publishing_office,
            today_forecast_time = today_forecast.time_define.formatl("%d??? (%a) ", "ja"),
            today_forecast_content = today_forecast.content,
            tomorrow_forecast_time = tomorrow_forecast.time_define.formatl("%d??? (%a) ", "ja"),
            tomorrow_forecast_content = tomorrow_forecast.content,
        );

        let json = json!({ "message": message });
        self.client.post(Self::TYPETALK_TOPIC_URL)
            .header("X-TYPETALK-TOKEN", &self.typetalk_token)
            .json(&json)
            .send()
            .await
    }
}
