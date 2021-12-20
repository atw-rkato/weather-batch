use serde::Deserialize;

use weather_batch_service::WeatherBatchService;

mod weather_batch_service;

use std::io::Write;

type AppError = Box<dyn std::error::Error + Send + Sync + 'static>;

#[derive(Deserialize, Debug)]
struct Env {
    typetalk_token: String,
}

#[tokio::main]
async fn main() -> Result<(), AppError> {
    env_logger::builder()
        .format(|buf, record| {
            let ts = buf.timestamp();
            writeln!(
                buf,
                "{ts} {level} [{target}] {args} {file}:{line}",
                ts = ts,
                level = record.level(),
                target = record.target(),
                args = record.args(),
                file = record.file().unwrap_or("unknown"),
                line = record.line().unwrap_or(0),
            )
        })
        .init();

    log::info!("app start.");
    let env = envy::from_env::<Env>()?;
    let service = WeatherBatchService::new(&env.typetalk_token);
    service.run().await?;
    log::info!("app end.");

    Ok(())
}
