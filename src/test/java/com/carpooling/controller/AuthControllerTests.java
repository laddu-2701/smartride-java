package com.carpooling.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.carpooling.repository.UserRepository userRepository;

    private Map<String, String> user;

    @BeforeEach
    void setUp() {
        // clear data so each test is isolated
        userRepository.deleteAll();
        long count = userRepository.count();
        System.out.println("user count after delete all: " + count);

        user = new HashMap<>();
        user.put("name", "Test User");
        user.put("email", "test@example.com");
        user.put("phone", "1234567890");
        user.put("password", "password");
        user.put("role", "PASSENGER");
    }

    @Test
    void registerAndLoginPassenger() throws Exception {
        // register
        MvcResult registerResult = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn();
        int regStatus = registerResult.getResponse().getStatus();
        String regBody = registerResult.getResponse().getContentAsString();
        System.out.println("register response: " + regStatus + " body=" + regBody);
        org.junit.jupiter.api.Assertions.assertEquals(200, regStatus);

        // login
        Map<String, String> creds = new HashMap<>();
        creds.put("username", "test@example.com");
        creds.put("password", "password");

        MvcResult loginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(creds)))
                .andExpect(status().isOk())
                .andReturn();

        String body = loginResult.getResponse().getContentAsString();
        @SuppressWarnings("unchecked")
        Map<String, Object> respMap = objectMapper.readValue(body, Map.class);
        assertThat(respMap).containsKey("token");
    }

    @Test
    void duplicateRegistrationShouldFail() throws Exception {
        MvcResult first = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andReturn();
        int firstStatus = first.getResponse().getStatus();
        String firstBody = first.getResponse().getContentAsString();
        System.out.println("duplicate test first registration: " + firstStatus + " body=" + firstBody);
        org.junit.jupiter.api.Assertions.assertEquals(200, firstStatus);

        // second attempt with same email/phone
        user.put("phone", "1234567890");
        MvcResult second = mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isBadRequest())
                .andReturn();

        String msg = second.getResponse().getContentAsString();
        assertThat(msg).contains("already registered");
    }
}