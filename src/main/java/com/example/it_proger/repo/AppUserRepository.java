package com.example.it_proger.repo;

import com.example.it_proger.models.AppUser;
import com.example.it_proger.models.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.core.userdetails.User;

import java.util.List;

public interface AppUserRepository extends JpaRepository<AppUser, Integer> {
    public AppUser findByEmail(String email);
    public AppUser findById(int id);

    List<AppUser> findByRole(Role role);
}
