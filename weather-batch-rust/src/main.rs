mod service;

use serde::Deserialize;

type AppError = Box<dyn std::error::Error + Send + Sync + 'static>;

#[derive(Deserialize, Debug)]
struct Env {
    typetalk_token: String,
}

#[tokio::main]
async fn main() -> Result<(), AppError> {
    env_logger::init();
    let env = envy::from_env::<Env>()?;
    service::run(&env.typetalk_token).await?;
    Ok(())
}
