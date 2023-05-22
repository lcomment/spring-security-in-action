package com.ch6.ssia.domain.member.service;

import com.ch6.ssia.global.security.CustomUserDetails;
import com.ch6.ssia.domain.member.entity.Member;
import com.ch6.ssia.domain.member.repository.MemberRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class MemberDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    @Override
    public CustomUserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return new CustomUserDetails(getMember(username));
    }

     private Member getMember(String memberName) {
        return memberRepository.findMemberByMemberName(memberName).orElseThrow(EntityNotFoundException::new);
     }
}
