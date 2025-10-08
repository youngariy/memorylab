package com.memorylab.domain.board;

public enum ExternalStatus {
    QUEUED,
    PROCESSING,
    COMPLETED,
    FAILED,
    CANCELED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELED;
    }
}
