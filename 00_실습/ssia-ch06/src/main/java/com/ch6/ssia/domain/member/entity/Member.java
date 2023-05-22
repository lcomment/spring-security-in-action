package com.ch6.ssia.domain.member.entity;

import com.ch6.ssia.global.security.enums.EncryptionAlgorithm;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String memberName;
    private String password;

    @Enumerated(EnumType.STRING)
    private EncryptionAlgorithm algorithm;

    @OneToMany(mappedBy = "member", fetch = FetchType.EAGER)
    private List<Authority> authorities;

    @Builder
    public Member(String memberName, String password, EncryptionAlgorithm algorithm, List<Authority> authorities) {
        this.memberName = memberName;
        this.password = password;
        this.algorithm = algorithm;
        this.authorities = authorities;
    }
}
