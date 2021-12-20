#!/bin/bash

echo "Java版 (JVM)"
time docker container run --rm --env-file ./env.list weather-batch-java-jvm

echo;
echo "Java版 (ネイティブ)"
time docker container run --rm --env-file ./env.list weather-batch-java-native

echo;
echo "Scala版 (JVM)"
time docker container run --rm --env-file ./env.list weather-batch-scala-jvm

echo;
echo "Rust版"
time docker container run --rm --env-file ./env.list weather-batch-rust