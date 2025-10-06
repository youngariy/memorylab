package com.memorylab.service;

import com.memorylab.domain.user.Member;
import com.memorylab.domain.user.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final MemberRepository memberRepository;

    /**
     * 이메일을 기준으로 사용자 정보를 조회합니다.
     * @param email 조회할 사용자의 이메일
     * @return 찾아낸 Member 엔티티
     * @throws IllegalArgumentException 해당 이메일의 사용자가 없을 경우 발생
     */
    public Member getMemberByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));
    }
}
