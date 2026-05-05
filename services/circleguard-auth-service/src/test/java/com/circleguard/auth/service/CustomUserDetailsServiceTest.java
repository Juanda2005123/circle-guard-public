package com.circleguard.auth.service;

import com.circleguard.auth.model.LocalUser;
import com.circleguard.auth.model.Permission;
import com.circleguard.auth.model.Role;
import com.circleguard.auth.repository.LocalUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private LocalUserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private LocalUser mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new LocalUser();
        mockUser.setUsername("testuser");
        mockUser.setPassword("encodedpw");
        mockUser.setIsActive(true);

        Role userRole = new Role();
        userRole.setName("USER");
        Permission readPerm = new Permission();
        readPerm.setName("user:read");
        userRole.setPermissions(Set.of(readPerm));

        mockUser.setRoles(new HashSet<>(Set.of(userRole)));
    }

    @Test
    void loadUserByUsername_UserFound_ShouldReturnUserDetails() {
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("encodedpw", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
        assertTrue(userDetails.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("user:read")));
    }

    @Test
    void loadUserByUsername_UserNotFound_ShouldThrowException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> 
            userDetailsService.loadUserByUsername("unknown")
        );
    }

    @Test
    void loadUserByUsername_UserDisabled_ShouldThrowException() {
        mockUser.setIsActive(false);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(mockUser));

        assertThrows(DisabledException.class, () -> 
            userDetailsService.loadUserByUsername("testuser")
        );
    }
}
