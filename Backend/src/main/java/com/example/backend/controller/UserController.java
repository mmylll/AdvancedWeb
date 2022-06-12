package com.example.backend.controller;

import com.example.backend.annotation.BaseResponse;
import com.example.backend.model.Room;
import com.example.backend.model.User;
import com.example.backend.repository.UserLogRepository;
import com.example.backend.repository.UserRepository;
import com.example.backend.response.ResponseResult;
import com.example.backend.service.UserService;
import com.example.backend.utils.JWTUtils;
import com.example.backend.utils.UserLog;
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
    private UserLogRepository userLogRepository;

    @Autowired
    private UserService userService;

    @GetMapping("FindAllUsers")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @PostMapping("Login")
    public ResponseResult login(HttpServletResponse response, @RequestParam(value = "username") String username, @RequestParam(value = "password") String password) {
//    public ResponseResult login(HttpServletResponse response,@RequestBody JSONObject jsonParam) throws JSONException {
        //response.setHeader(JWTUtils.TOKEN, userService.login(username, password));
        return ResponseResult.getSuccessResponse("登录成功", userService.login(username, password));
    }

    @PostMapping("Register")
    public ResponseResult register(HttpServletResponse response, @RequestParam(value = "username") String username, @RequestParam(value = "password") String password, @RequestParam(value = "email") String email) {
        response.setHeader(JWTUtils.TOKEN, userService.register(username, password, email));
        return new ResponseResult("注册成功");
    }

    @GetMapping("Info")
    public User Info(@RequestParam("username") String username) {
        return userService.info(username);
    }

    @GetMapping("Log")
    public List<UserLog> getAllLogs(@RequestParam("username") String username) {
        return userLogRepository.findAllByUsername(username);
    }

    @GetMapping("Join")
    public Room join() {
        return Room.getRoom();
    }

    @PostMapping("Set")
    public Room set(@RequestParam(value = "number") String number) {
        return userService.set(Integer.parseInt(number));
    }
}
