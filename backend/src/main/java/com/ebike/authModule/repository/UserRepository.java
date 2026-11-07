package com.ebike.authModule.repository;

import com.ebike.authModule.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @EntityGraph(attributePaths = {"roles"})
    List<User> findAll();

    Optional<User> findByEmail(String email);

    Optional<User> findByUsername(String username);

    @EntityGraph(attributePaths = {
        "roles",
        "roles.permissions",
        "userPermissions",
        "userPermissions.permission"
    })
    Optional<User> findWithPermissionsByUsername(String username);

    @EntityGraph(attributePaths = {
        "roles",
        "roles.permissions",
        "userPermissions",
        "userPermissions.permission"
    })
    Optional<User> findWithPermissionsByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    @org.springframework.data.jpa.repository.Query(value = "select exists(select 1 from ebike_auth.users where id<>:excludedId and (case when phone_number like '0%' then '+84'||substring(phone_number from 2) else phone_number end)=:phone)", nativeQuery = true)
    boolean phoneInUse(@org.springframework.data.repository.query.Param("phone") String phone,
                       @org.springframework.data.repository.query.Param("excludedId") Long excludedId);
}
