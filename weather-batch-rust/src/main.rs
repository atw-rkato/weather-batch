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
    env_logger::init();
    log::info!("app start.");
    let env = envy::from_env::<Env>()?;
    let service = WeatherBatchService::new(&env.typetalk_token);
    service.run().await?;
    log::info!("app end.");
    Ok(())
}
