package com.example.backend.controller;

import com.example.backend.annotation.BaseResponse;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.UserService;
import com.example.backend.utils.JWTUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * user接口
 */

@BaseResponse
@RestController
public class UserController {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @GetMapping("findAllUsers")
    public List<User> getAllUsers(){
        return userRepository.findAll();
    }

    @PostMapping("login")
    public void login(HttpServletResponse response, @RequestParam(value = "username") String username, @RequestParam(value = "password") String password){
        response.setHeader(JWTUtils.TOKEN, userService.login(username,password));
    }
}
