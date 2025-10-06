package com.memorylab.api;

import com.memorylab.api.dto.MeResponse;
import com.memorylab.domain.user.Member;
import com.memorylab.domain.user.MemberRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class MeController {

    private final MemberRepository memberRepository;

    public MeController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(@AuthenticationPrincipal User principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("{\"error\":\"unauthorized\"}");
        }

        Member m = memberRepository.findByEmail(principal.getUsername())
                .orElse(null);

        // 권한은 토큰/시큐리티 컨텍스트에서 꺼냄
        List<String> roles = principal.getAuthorities().stream().map(Object::toString).toList();

        // DB에 사용자가 없으면 최소한 email/roles만이라도 내려주기
        if (m == null) {
            return ResponseEntity.ok(new MeResponse(
                    null,
                    principal.getUsername(),
                    "",
                    "",
                    roles,
                    null
            ));
        }

        return ResponseEntity.ok(new MeResponse(
                m.getId(),
                m.getEmail(),
                nvl(m.getName()),
                nvl(m.getNickname()),
                roles,
                m.getCreatedAt()
        ));
    }

    private static String nvl(String s) { return s == null ? "" : s; }
}
