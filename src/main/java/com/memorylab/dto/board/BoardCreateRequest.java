// PostCreateRequest.java
package com.memorylab.dto.board;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class BoardCreateRequest {
    @NotBlank private String title;
    private String content;
}
