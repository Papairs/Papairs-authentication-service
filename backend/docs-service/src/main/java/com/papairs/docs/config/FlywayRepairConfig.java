package com.papairs.docs.config;

import org.flywaydb.core.api.callback.Callback;
import org.flywaydb.core.api.callback.Context;
import org.flywaydb.core.api.callback.Event;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayRepairConfig {

    @Bean
    public FlywayConfigurationCustomizer flywayConfigurationCustomizer() {
        return configuration -> configuration.callbacks(new Callback() {
            @Override
            public boolean supports(Event event, Context context) {
                return event == Event.BEFORE_MIGRATE;
            }

            @Override
            public boolean canHandleInTransaction(Event event, Context context) {
                return false;
            }

            @Override
            public void handle(Event event, Context context) {
                try {
                    var statement = context.getConnection().createStatement();
                    
                    // Delete any failed migration entries before Flyway validates
                    int deletedRows = statement.executeUpdate("DELETE FROM flyway_schema_history WHERE success = 0");
                    
                    if (deletedRows > 0) {
                        System.out.println("Flyway Repair: Removed " + deletedRows + " failed migration(s)");
                    }
                    
                    statement.close();
                } catch (Exception e) {
                    System.err.println("Flyway Repair: Could not repair failed migrations: " + e.getMessage());
                }
            }

            @Override
            public String getCallbackName() {
                return "FlywayRepairCallback";
            }
        });
    }
}
