package com.example.medevac.controllers;

import jdk.jfr.ContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {
    @Autowired
    MockMvc mvc;

    @WithAnonymousUser
    @Test
    void shouldReturnUnauthorizedForAnonymousUser() throws Exception {
        mvc.perform(get("/api/v1/validateSession"))
                .andExpect(status().isUnauthorized());
    }

    @WithMockUser
    @Test
    void shouldReturnOkForAuthenticatedUser() throws Exception {
        mvc.perform(get("/api/v1/validateSession"))
                .andExpect(status().isOk());
    }

    @WithAnonymousUser
    @Test
    void shouldReturnWithLoggedInIfCredentialsCorrect() throws Exception {

        String exampleCredentials = "{user:\"Batman\",password:\"Password\"}";

        mvc.perform(post("/api/v1/loginUser")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "Batman")
                        .param("password", "Password")
                )
                .andExpect(status().isOk());
    }

    @WithAnonymousUser
    @Test
    void shouldRejectWithNotLoggedInIfCredentialsIncorrect() throws Exception {
        mvc.perform(post("/api/v1/loginUser")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", "Eunbin")
                        .param("password", "Password")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithAnonymousUser
    void shouldRejectAnonymousUserFromGettingAssignments () throws Exception {
        mvc.perform(get("/api/v1/assignment")
                .contentType (MediaType.APPLICATION_JSON))
            .andExpect(status().isFound ());
    }

    @Test
    @WithMockUser
    void shouldAllowMockUserFromGettingAssignments () throws Exception {
        mvc.perform(get("/api/v1/assignment")
                .contentType (MediaType.APPLICATION_JSON))
            .andExpect(status().isOk ());
    }


    @Test
    @WithAnonymousUser
    void shouldRejectAnonymousUserFromGettingRequests () throws Exception {
        mvc.perform(get("/api/v1/medevac")
                .contentType (MediaType.APPLICATION_JSON))
            .andExpect(status().isFound ());
    }

    @Test
    @WithMockUser
    void shouldAllowMockUserFromGettingRequests () throws Exception {
        mvc.perform(get("/api/v1/medevac/all")
                .with(user ("Batman")
                    .password ("Password")
                    .authorities (new SimpleGrantedAuthority ("DISPATCHER")))
                .contentType (MediaType.APPLICATION_JSON))
            .andExpect(status().isOk ());
    }
}