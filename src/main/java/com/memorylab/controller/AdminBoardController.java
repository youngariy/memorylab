package com.memorylab.controller;

import com.memorylab.service.BoardAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/boards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class AdminBoardController {

    private final BoardAdminService boardAdminService;

    @PostMapping("/{boardId}/cancel")
    public ResponseEntity<Void> cancelConversion(@PathVariable Long boardId) {
        boardAdminService.cancelConversion(boardId);
        return ResponseEntity.ok().build();
    }
}
