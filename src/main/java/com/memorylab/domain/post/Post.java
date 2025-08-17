// Post.java
package com.memorylab.domain.post;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=100)
    private String title;

    @Column(columnDefinition="TEXT")
    private String content;

    private LocalDateTime createdAt;
    @PrePersist void onCreate(){ createdAt = LocalDateTime.now(); }
}
