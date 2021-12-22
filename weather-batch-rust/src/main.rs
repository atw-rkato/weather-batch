use serde::Deserialize;

use weather_batch_service::WeatherBatchService;

mod weather_batch_service;

type AppError = Box<dyn std::error::Error + Send + Sync + 'static>;

#[derive(Deserialize, Debug)]
struct Env {
    typetalk_token: String,
}

#[tokio::main]
async fn main() -> Result<(), AppError> {
    init_logger();

    log::info!("app start.");
    let env = envy::from_env::<Env>()?;
    let service = WeatherBatchService::new(&env.typetalk_token);
    match service.run().await {
        Ok(_) => log::info!("app end."),
        Err(e) => log::error!("{}", e)
    }

    Ok(())
}

fn init_logger() {
    env_logger::builder()
        .format(|buf, record| {
            use std::io::Write;

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
