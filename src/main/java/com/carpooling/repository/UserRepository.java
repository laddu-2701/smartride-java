package com.carpooling.repository;

import com.carpooling.model.User;
import com.carpooling.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhone(String phone);
    Optional<User> findFirstByRole(Role role);
    long countByRole(Role role);
    long countByBlockedTrue();
    long countByRoleAndDriverVerifiedTrue(Role role);
    List<User> findByRole(Role role);
}
