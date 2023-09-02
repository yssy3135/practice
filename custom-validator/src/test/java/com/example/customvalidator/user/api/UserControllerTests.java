package com.example.customvalidator.user.api;


import com.example.customvalidator.user.controller.UserController;
import com.example.customvalidator.user.controller.UserRequest;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
public class UserControllerTests {


    private MockMvc mockMvc;

    @InjectMocks
    UserController userController;

    private Gson gson;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        createStandAloneMvc();

    }

    private void createStandAloneMvc(){

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .build();

        gson = new GsonBuilder().create();

    }


    @Test
    public void userRegistrationTest() throws Exception {

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(UserRequest.builder()
                                .userType("manager")
                                .username("ysys3131")
                                .password("0000")
                                .name("yoonsoo")
                                .phone("01012340000")
                        .build()))
        ).andExpect(status().isCreated());
    }


    @Test
    @DisplayName("userType이 manager일때는 name이 null이면 실패")
    public void userRegistrationFailWhenUserTypeManagerAndNameIsNull() throws Exception {

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(UserRequest.builder()
                        .userType("manager")
                        .username("ysys3131")
                        .password("0000")
                        .phone("01012340000")
                        .build()))
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("userType이 manager일때는 phone이 null이면 실패")
    public void userRegistrationFailWhenUserTypeManagerAndPhoneIsNull() throws Exception {

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(UserRequest.builder()
                        .userType("manager")
                        .username("ysys3131")
                        .password("0000")
                        .name("yoonsoo")
                        .build()))
        ).andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("userType이 manager가 아닐때 phone은 null이어도 성공")
    public void userRegistrationSuccessWhenUserTypeGeneralAndPhoneIsNull() throws Exception {

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(UserRequest.builder()
                        .userType("general")
                        .username("ysys3131")
                        .password("0000")
                        .name("yoonsoo")
                        .build()))
        ).andExpect(status().isCreated());
    }


    @Test
    @DisplayName("userType이 manager가 아닐때 name은 null이어도 성공")
    public void userRegistrationSuccessWhenUserTypeGeneralAndNameIsNull() throws Exception {

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(UserRequest.builder()
                        .userType("general")
                        .username("ysys3131")
                        .password("0000")
                        .phone("01012340000")
                        .build()))
        ).andExpect(status().isCreated());
    }




}
