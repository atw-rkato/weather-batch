use chrono::{DateTime, FixedOffset};
use chrono_locale::LocaleDate;
use indoc::indoc;
use reqwest::{Response};
use serde_json::{json, Value};

use crate::AppError;

const WEATHER_URL: &str = "https://www.jma.go.jp/bosai/forecast/data/forecast";
const YOKOHAMA_OFFICE_CODE: &str = "140000";
const TYPETALK_TOPIC_URL: &str = "https://typetalk.com/api/v1/topics/259767";

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

pub(crate) async fn run(typetalk_token: &str) -> Result<(), AppError> {
    let weather_json = fetch_yokohama_weather().await?;
    let forecast = extract_yokohama_data(&weather_json)?;
    send_to_typetalk(&forecast, typetalk_token).await?;
    Ok(())
}

async fn fetch_yokohama_weather() -> reqwest::Result<Value> {
    let resp = reqwest::get(format!("{}/{}.json", WEATHER_URL, YOKOHAMA_OFFICE_CODE)).await?;
    resp.json::<Value>().await
}

fn extract_yokohama_data(weather_json: &Value) -> Result<Forecast, AppError> {
    let details = weather_json.get(0).ok_or("")?;
    let time_series = details.get("timeSeries").and_then(|v| v.get(0)).ok_or("timeSeries")?;
    let time_defines = time_series.get("timeDefines").ok_or("timeDefines")?;
    let weathers = time_series.get("areas")
        .and_then(|v| v.get(0))
        .and_then(|v| v.get("weathers"))
        .ok_or("weathers")?;

    let get_forecast = |i: usize| -> Result<DailyForecast, AppError> {
        let date_string = time_defines.get(i).and_then(|v| v.as_str()).ok_or(i.to_string())?;
        let time_define = DateTime::parse_from_rfc3339(date_string)?;
        let content = weathers.get(i).and_then(|v| v.as_str()).map(str::to_string).ok_or(i.to_string())?;
        Ok(DailyForecast { time_define, content })
    };

    let publishing_office = details.get("publishingOffice")
        .and_then(|v| v.as_str())
        .map(str::to_string)
        .ok_or("publishingOffice")?;
    let report_datetime_string = details.get("reportDatetime")
        .and_then(|v| v.as_str())
        .ok_or("reportDatetime")?;
    let report_datetime = DateTime::parse_from_rfc3339(report_datetime_string)?;
    let today_forecast = get_forecast(0)?;
    let tomorrow_forecast = get_forecast(1)?;

    let forecast = Forecast { publishing_office, report_datetime, today_forecast, tomorrow_forecast };
    Ok(forecast)
}

async fn send_to_typetalk(
    Forecast { publishing_office, report_datetime, today_forecast, tomorrow_forecast }: &Forecast,
    typetalk_token: &str,
) -> reqwest::Result<Response> {
    let message = format!(
        indoc! {r#"
            横浜の天気
            {report_datetime} {publishing_office} 発表 (気象庁より)
            今日 {today_forecast_time} ：  {today_forecast_content}
            明日 {tomorrow_forecast_time} ：  {tomorrow_forecast_content}
        "#},
        report_datetime = report_datetime.formatl("%Y年%m月%d日 %H時", "ja"),
        publishing_office = publishing_office,
        today_forecast_time = today_forecast.time_define.formatl("%d日 (%a) ", "ja"),
        today_forecast_content = today_forecast.content,
        tomorrow_forecast_time = tomorrow_forecast.time_define.formatl("%d日 (%a) ", "ja"),
        tomorrow_forecast_content = tomorrow_forecast.content,
    );

    let json = json!({ "message": message });
    let client = reqwest::Client::new();
    client.post(TYPETALK_TOPIC_URL)
        .header("X-TYPETALK-TOKEN", typetalk_token)
        .json(&json)
        .send()
        .await
}
