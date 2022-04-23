package com.example.backend.service;

import com.example.backend.exception.BaseException;
import com.example.backend.model.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.response.ResponseCode;
import com.example.backend.utils.JWTUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public String login(String username,String password) {
        if (username.equals(""))
            throw new BaseException(ResponseCode.LOGIN_FALSE);
        User user = userRepository.getByUsername(username);
        if (user == null) {
            throw new BaseException(ResponseCode.LOGIN_FALSE);
        }
        if(password.equals(user.getPassword())){
            return JWTUtils.createToken(user.getUsername());
        }else {
            throw new BaseException(ResponseCode.LOGIN_FALSE);
        }
    }

    public String register(String username,String password,String email){
        if(userRepository.existsUserByUsername(username)){
            throw new BaseException(ResponseCode.REGISTER_USERNAME_DUPLICATE);
        }else{
            User user = new User(username,password,email);
            try {
                userRepository.save(user);
            }catch (Exception e){
                throw new BaseException(ResponseCode.REGISTER_FALSE);
            }
            return JWTUtils.createToken(username);
        }
    }

    public User info(String username){
        User user = userRepository.getByUsername(username);
        if(user == null){
            throw new BaseException(ResponseCode.USER_NOT_EXIST);
        }
        return user;
    }
}
