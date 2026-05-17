package com.sep490.hdbhms.modules.user.repository;

import com.sep490.hdbhms.modules.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("""
            SELECT u
            FROM User u
            WHERE u.deletedAt IS NULL
              AND (u.phone = :phoneOrEmail OR LOWER(u.email) = LOWER(:phoneOrEmail))
            """)
    Optional<User> findByPhoneOrEmail(@Param("phoneOrEmail") String phoneOrEmail);
}
