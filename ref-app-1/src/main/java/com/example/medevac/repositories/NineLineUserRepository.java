package com.example.medevac.repositories;

import com.example.medevac.entities.NineLineUser;
import com.example.medevac.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NineLineUserRepository extends JpaRepository<NineLineUser, Long> {
    NineLineUser findByUsername(String username);

    @Query("SELECT user FROM NineLineUser user JOIN user.roles role WHERE role.name = :roleName")
    List<NineLineUser> findByRoleName(@Param("roleName") String roleName);
}
