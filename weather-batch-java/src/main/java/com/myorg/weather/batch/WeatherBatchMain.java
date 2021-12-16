package com.myorg.weather.batch;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.jboss.logging.Logger;

@QuarkusMain
public class WeatherBatchMain implements QuarkusApplication {
    private static final Logger log = Logger.getLogger(WeatherBatchMain.class);

    private final WeatherBatchService service;

    public WeatherBatchMain(WeatherBatchService service) {
        this.service = service;
    }

    @Override
    public int run(String... args) throws Exception {
        log.info("app start.");
        this.service.run();
        log.info("app end.");
        return 0;
    }
}
