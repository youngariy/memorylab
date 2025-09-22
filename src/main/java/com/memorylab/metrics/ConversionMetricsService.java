package com.memorylab.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

@Service
public class ConversionMetricsService {

    private final Counter conversionPendingCounter;
    private final Counter conversionCompletedCounter;
    private final Counter conversionErrorCounter;

    public ConversionMetricsService(MeterRegistry meterRegistry) {
        this.conversionPendingCounter = Counter.builder("conversions.status")
                .tag("status", "pending")
                .description("변환 대기열에 추가된 작업 수")
                .register(meterRegistry);

        this.conversionCompletedCounter = Counter.builder("conversions.status")
                .tag("status", "completed")
                .description("성공적으로 변환이 완료된 작업 수")
                .register(meterRegistry);

        this.conversionErrorCounter = Counter.builder("conversions.status")
                .tag("status", "error")
                .description("변환에 실패한 작업 수")
                .register(meterRegistry);
    }

    /** PENDING 상태 카운터를 1 증가시킵니다. */
    public void incrementPending() {
        conversionPendingCounter.increment();
    }

    /** COMPLETED 상태 카운터를 1 증가시킵니다. */
    public void incrementCompleted() {
        conversionCompletedCounter.increment();
    }

    /** ERROR 상태 카운터를 1 증가시킵니다. */
    public void incrementError() {
        conversionErrorCounter.increment();
    }
}
