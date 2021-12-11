# weather-batch



## Java版

```shell
cd weather-batch-java
```

### JVM版

```shell
docker build -t weather-batch-jvm --progress plain . -f src/main/docker/Dockerfile.jvm
```

```shell
docker container run --rm --env-file ../env.list weather-batch-jvm
```

### ネイティブビルド版

```shell
docker build -t weather-batch-native --progress plain . -f src/main/docker/Dockerfile.native
```

```shell
docker container run --rm --env-file ../env.list weather-batch-native
```

