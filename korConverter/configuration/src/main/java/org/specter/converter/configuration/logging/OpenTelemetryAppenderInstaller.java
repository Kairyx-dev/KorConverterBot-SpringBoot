package org.specter.converter.configuration.logging;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Wires Spring Boot's managed {@link OpenTelemetry} instance into the {@link OpenTelemetryAppender}
 * declared in {@code runtime/cfg/logback-spring.xml}.
 *
 * <p>Spring Boot 4's {@code OpenTelemetryLoggingAutoConfiguration} provisions the OTLP {@code
 * LogRecordExporter} but does not install the SDK into the external logback appender. Without this
 * bridge bean, the appender starts up with a no-op OpenTelemetry instance and silently drops every
 * log record — Loki stays empty even though the exporter is correctly configured.
 *
 * <p>{@link InitializingBean} runs once, after the {@code OpenTelemetry} bean is available and
 * before traffic handling. Any log record emitted afterwards flows: Logback event →
 * OpenTelemetryAppender → LogRecordExporter → OTLP → Loki.
 */
@AutoConfiguration
public class OpenTelemetryAppenderInstaller {

    @Bean
    public InitializingBean openTelemetryAppenderInstaller(OpenTelemetry openTelemetry) {
        return () -> OpenTelemetryAppender.install(openTelemetry);
    }
}
