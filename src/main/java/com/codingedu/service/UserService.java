package com.codingedu.service;

import com.codingedu.entity.User;
import com.codingedu.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isUsernameTaken(String username) {
        return userRepository.findByUsername(username).isPresent();
    }

    public boolean isNicknameTaken(String nickname) {
        return userRepository.findByNickname(nickname).isPresent();
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));
    }

    @Transactional
    public void register(User user) {
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void updateNickname(String username, String newNickname) {
        // 닉네임 중복 체크 (현재 사용자 제외)
        User existingUser = userRepository.findByNickname(newNickname).orElse(null);
        if (existingUser != null && !existingUser.getUsername().equals(username)) {
            throw new IllegalArgumentException("이미 사용 중인 닉네임입니다.");
        }
        
        User user = findByUsername(username);
        user.setNickname(newNickname);
        userRepository.save(user);
    }

    @Transactional
    public void updateEmail(String username, String newEmail) {
        User user = findByUsername(username);
        user.setEmail(newEmail);
        userRepository.save(user);
    }

    @Transactional
    public boolean updatePassword(String username, String oldPassword, String newPassword) {
        User user = findByUsername(username);
        
        // 이전 비밀번호 검증
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            return false;
        }
        
        // 새로운 비밀번호로 업데이트
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }
}
