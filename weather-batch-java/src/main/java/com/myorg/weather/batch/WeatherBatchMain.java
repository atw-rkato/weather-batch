package com.myorg.weather.batch;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class WeatherBatchMain implements QuarkusApplication {

    private final WeatherBatchService service;

    public WeatherBatchMain(WeatherBatchService service) {
        this.service = service;
    }

    @Override
    public int run(String... args) throws Exception {
        this.service.run();
        return 0;
    }
}
