package ai.amygo.raas.config;

import io.opentelemetry.exporter.logging.LoggingSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAAS-015: local/demo SpanExporter that prints spans — no OTLP collector required.
 * Swap for OTLP exporter in ops when a collector is available.
 */
@Configuration
public class TracingConfig {

    @Bean
    @ConditionalOnMissingBean(SpanExporter.class)
    SpanExporter loggingSpanExporter() {
        return LoggingSpanExporter.create();
    }
}
