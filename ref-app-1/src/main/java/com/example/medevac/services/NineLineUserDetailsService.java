package com.example.medevac.services;

import com.example.medevac.entities.NineLineUser;
import com.example.medevac.entities.NineLineUserDetails;
import com.example.medevac.entities.Role;
import com.example.medevac.pojos.ResponderDto;
import com.example.medevac.repositories.NineLineUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NineLineUserDetailsService implements UserDetailsService {
    @Autowired
    NineLineUserRepository nineLineUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        NineLineUser user = nineLineUserRepository.findByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException(username);
        }
        return new NineLineUserDetails(user);
    }

    public List<ResponderDto> getAllResponders() {
        return nineLineUserRepository.findByRoleName("RESPONDER").stream().map(nineLineUser -> ResponderDto.from(nineLineUser)).toList();
    }
}
