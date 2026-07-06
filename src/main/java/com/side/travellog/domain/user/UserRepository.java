package com.side.travellog.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    
    boolean existsByEmail(String email);
    
    User findByEmail(String email);

    User findByNickname(String nickname);
}