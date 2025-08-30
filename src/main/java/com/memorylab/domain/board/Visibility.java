package com.memorylab.domain.board;

public enum Visibility {
    PUBLIC, PRIVATE;

    public static Visibility parse(String v) {
        if (v == null || v.isBlank()) return Visibility.PUBLIC;
        return Visibility.valueOf(v.trim().toUpperCase());
    }
}