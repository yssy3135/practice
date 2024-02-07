package com.example.webflux.config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.springframework.r2dbc.core.DatabaseClient;

@Component
@Slf4j
@RequiredArgsConstructor
public class R2dbcConfig implements ApplicationListener<ApplicationReadyEvent> {

    private final DatabaseClient dataBaseClient;


    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {


        //reactor: publisher, subscriber
        dataBaseClient.sql("SELECT 1").fetch().one()
                .subscribe(
                        success -> {
                            log.info("Initialize r2dbc database connection");
                        },
                        error -> {
                            log.error("Fail r2dbc database connection");
                        }
                );

    }

    @Override
    public boolean supportsAsyncExecution() {
        return ApplicationListener.super.supportsAsyncExecution();
    }
}
