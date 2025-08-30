package com.memorylab.domain.board;

public enum Category {
    NOTICE, FREE, QNA;

    public static Category parse(String v) {
        if (v == null || v.isBlank()) return null;
        return Category.valueOf(v.trim().toUpperCase());
    }
}