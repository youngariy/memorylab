package com.memorylab.service;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * MultipartFile을 지정된 타입에 따라 적절한 하위 디렉토리에 저장하고, 상대 경로를 반환합니다.
     */
    String storeFile(MultipartFile file, FileType fileType);

    /**
     * byte 배열 데이터를 지정된 타입에 따라 적절한 하위 디렉토리에 저장하고, 상대 경로를 반환합니다.
     * AI 서버로부터 다운로드한 파일을 저장할 때 사용됩니다.
     *
     * @param fileData 저장할 파일의 byte 배열 데이터
     * @param fileType 저장할 파일의 종류 (ORIGINAL, CONVERTED, THUMBNAIL)
     * @param originalFileName 원본 파일명 (확장자 추출용)
     * @return 저장소 루트로부터의 상대 경로
     */
    String storeFile(byte[] fileData, FileType fileType, String originalFileName);

    /**
     * 지정된 상대 경로의 파일을 저장소에서 삭제합니다.
     */
    void deleteFile(String relativePath);
}
