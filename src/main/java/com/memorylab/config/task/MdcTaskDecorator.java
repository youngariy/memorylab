package com.memorylab.config.task;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * 부모 스레드의 MDC(Mapped Diagnostic Context)를 자식 스레드로 복사하는 TaskDecorator 입니다.
 * 이를 통해 @Async 또는 TaskExecutor를 통해 실행되는 비동기 작업에서도 로그 추적 ID를 유지할 수 있습니다.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // 작업을 제출하는 현재 스레드의 MDC 컨텍스트를 가져옵니다.
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        // 실제 작업을 실행할 때, 가져온 MDC 컨텍스트를 설정하고 작업을 실행합니다.
        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                // 작업 완료 후에는 항상 MDC를 정리하여 스레드 풀의 다른 작업에 영향을 주지 않도록 합니다.
                MDC.clear();
            }
        };
    }
}
