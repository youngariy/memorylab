package com.memorylab.service;

import lombok.Getter;

/**
 * 저장할 파일의 종류를 구분하기 위한 Enum 입니다.
 * 각 파일 타입은 저장될 하위 디렉토리 이름을 가집니다.
 */
@Getter
public enum FileType {
    ORIGINAL("originals"),
    CONVERTED("converted"),
    THUMBNAIL("thumbnails");

    private final String directoryName;

    FileType(String directoryName) {
        this.directoryName = directoryName;
    }
}
