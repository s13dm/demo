package com.demo.user.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import com.demo.order.dto.UserDeliveryStatusResponse;
import com.demo.user.dto.CreateUserResponse;
import com.demo.user.dto.LoginResponse;
import com.demo.user.dto.UserOrdersResponse;
import com.demo.user.entity.Role;
import com.demo.user.service.UserService;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void registerUser_returnsCreatedUser() throws Exception {
        when(userService.registerUser(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CreateUserResponse(1L, "홍길동", "hong@example.com"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "홍길동",
                                  "email": "hong@example.com",
                                  "password": "pass1234"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.email").value("hong@example.com"));
    }

    @Test
    void registerAdmin_returnsCreatedAdmin() throws Exception {
        when(userService.registerAdmin(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new CreateUserResponse(2L, "관리자", "admin@example.com"));

        mockMvc.perform(post("/api/users/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "관리자",
                                  "email": "admin@example.com",
                                  "password": "admin1234",
                                  "adminSecretKey": "ADMIN_SECRET_2026"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(2))
                .andExpect(jsonPath("$.name").value("관리자"));
    }

    @Test
    void login_returnsLoginResponse() throws Exception {
        when(userService.login(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new LoginResponse(1L, "홍길동", "hong@example.com", Role.ROLE_USER, "로그인 성공"));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "hong@example.com",
                                  "password": "pass1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.message").value("로그인 성공"));
    }

    @Test
    void logout_returnsSuccessMessage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);

        mockMvc.perform(post("/api/users/logout").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("로그아웃 성공"));
    }

    @Test
    void getUserOrders_whenLoggedIn_returnsOrders() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("role", Role.ROLE_USER);

        when(userService.getUserOrders(1L))
                .thenReturn(new UserOrdersResponse(1L, "홍길동", 0, List.of()));

        mockMvc.perform(get("/api/users/1/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.userName").value("홍길동"))
                .andExpect(jsonPath("$.totalOrders").value(0));
    }

    @Test
    void getUserOrders_whenNotLoggedIn_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/users/1/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserOrders_adminCanViewOtherUserOrders() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 99L);
        session.setAttribute("role", Role.ROLE_ADMIN);

        when(userService.getUserOrders(1L))
                .thenReturn(new UserOrdersResponse(1L, "홍길동", 0, List.of()));

        mockMvc.perform(get("/api/users/1/orders").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1));
    }

    @Test
    void getAdminDeliveries_adminCanView() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("role", Role.ROLE_ADMIN);

        when(userService.getAllUsersDeliveryStatus())
                .thenReturn(List.of(
                        new UserDeliveryStatusResponse(1L, "유저1", "user1@example.com", 0, List.of()),
                        new UserDeliveryStatusResponse(2L, "유저2", "user2@example.com", 0, List.of())
                ));

        mockMvc.perform(get("/api/users/admin/deliveries").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userName").value("유저1"))
                .andExpect(jsonPath("$[1].userName").value("유저2"));
    }

    @Test
    void getAdminDeliveries_nonAdminForbidden() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("userId", 1L);
        session.setAttribute("role", Role.ROLE_USER);

        mockMvc.perform(get("/api/users/admin/deliveries").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAdminDeliveries_notLoggedInForbidden() throws Exception {
        mockMvc.perform(get("/api/users/admin/deliveries"))
                .andExpect(status().isForbidden());
    }
}
