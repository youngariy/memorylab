// PostCreateRequest.java
package com.memorylab.dto.post;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PostCreateRequest {
    @NotBlank private String title;
    private String content;
}
