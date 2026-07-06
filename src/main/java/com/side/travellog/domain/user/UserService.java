package com.side.travellog.domain.user;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public void register(String email, String password, String nickname) {
        if (!password.matches("^(?=.*[a-zA-Z])(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$")) {
            throw new IllegalArgumentException("비밀번호는 8자 이상, 영문/숫자/특수문자를 포함해야 합니다.");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nickname(nickname)
                .build();

        userRepository.save(user);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public void updateNickname(String email, String nickname) {
        User user = userRepository.findByEmail(email);
        user.updateNickname(nickname);
        userRepository.save(user);
    }

    public void updatePassword(String email, String currentPassword, String newPassword) {
        User user = userRepository.findByEmail(email);
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 올바르지 않습니다.");
        }
        user.updatePassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}