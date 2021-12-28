use std::process;

use anyhow::Result;
use serde::Deserialize;

use weather_batch_service::WeatherBatchService;

mod weather_batch_service;

#[derive(Deserialize, Debug)]
struct Env {
    typetalk_token: String,
}

#[tokio::main]
async fn main() {
    init_logger();
    log::info!("app start.");

    if let Err(e) = run().await {
        log::error!("{:?}", e);
        process::exit(1);
    } else {
        log::info!("app end.");
    }
}

async fn run() -> Result<()> {
    let env = envy::from_env::<Env>()?;
    let service = WeatherBatchService::new(env.typetalk_token);
    service.run().await
}

fn init_logger() {
    use std::io::Write;

    env_logger::builder()
        .format(|buf, record| {
            let ts = buf.timestamp_millis();
            let level = record.level();
            let level_style = buf.default_level_style(level);
            writeln!(
                buf,
                "{ts} {level} [{target}] {args} ({file}:{line})",
                ts = ts,
                level = level_style.value(level),
                target = record.target(),
                args = level_style.value(record.args()),
                file = record.file().unwrap_or("unknown"),
                line = record.line().unwrap_or(0),
            )
        })
        .init();
}
