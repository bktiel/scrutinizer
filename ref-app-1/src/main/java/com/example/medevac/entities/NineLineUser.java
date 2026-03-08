package com.example.medevac.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.lang.Nullable;

import java.util.List;

@Entity
@Getter
@Setter
public class NineLineUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(unique = true)
    String username;
    String password;
    @Nullable
    String callsign;

    @ManyToMany(fetch = FetchType.EAGER)
    List<Role> roles;
}
