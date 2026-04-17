package org.specter.converter.boot.logging;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires Spring Boot's managed {@link OpenTelemetry} instance into the {@link OpenTelemetryAppender}
 * declared in {@code logback-spring.xml}.
 *
 * <p>Spring Boot 4's {@code OpenTelemetryLoggingAutoConfiguration} provides the OTLP {@code
 * LogRecordExporter}, but does not install the SDK into the external logback appender. Without this
 * bridge bean, the appender starts up with a no-op OpenTelemetry instance and silently drops all
 * log records — Loki stays empty even though the exporter is correctly configured.
 *
 * <p>{@link InitializingBean} runs once, after the {@code OpenTelemetry} bean has been created and
 * before the application starts handling traffic. Any log record emitted after this point flows
 * through: Logback event → OpenTelemetryAppender → LogRecordExporter → OTLP → Loki.
 */
@Configuration
public class OpenTelemetryAppenderInstaller {

  @Bean
  public InitializingBean openTelemetryAppenderInstaller(OpenTelemetry openTelemetry) {
    return () -> OpenTelemetryAppender.install(openTelemetry);
  }
}
