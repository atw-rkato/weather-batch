# weather-batch

## 環境変数ファイルの作成
env.listを作成し、必要な環境変数を書き込む (pushしない)
```shell
cp env.list.template env.list
```

## Java版 (JVM)

```shell
cd weather-batch-java
docker build -t weather-batch-jvm --progress plain . -f src/main/docker/Dockerfile.jvm
docker container run --rm --env-file ../env.list weather-batch-jvm
```

## Java版 (ネイティブ)

```shell
cd weather-batch-java
docker build -t weather-batch-native --progress plain . -f src/main/docker/Dockerfile.native
docker container run --rm --env-file ../env.list weather-batch-native
```

## Scala版 (JVM)

```shell
cd weather-batch-scala
docker build -t weather-batch-scala --progress plain . -f src/main/docker/Dockerfile.jvm
docker container run --rm --env-file ../env.list weather-batch-scala
```

## Rust版

```shell
cd weather-batch-rust
docker build -t weather-batch-rust --progress plain .
docker container run --rm --env-file ../env.list weather-batch-rust
```