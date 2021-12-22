#!/bin/bash

echo "Java版 (JVM)"
cd weather-batch-java && \
docker build -t weather-batch-java-jvm -f ./src/main/docker/Dockerfile.jvm . --progress plain && \

echo;
echo "Java版 (ネイティブ)"
docker build -t weather-batch-java-native -f ./src/main/docker/Dockerfile.native . --progress plain && \
cd ../

echo;
echo "Scala版 (JVM)"
cd weather-batch-scala && \
docker build -t weather-batch-scala-jvm -f ./src/main/docker/Dockerfile.jvm . --progress plain && \
cd ../

echo;
echo "Rust版"
cd weather-batch-rust && \
docker build -t weather-batch-rust . --progress plain && \
cd ../