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
        //phone是除id外的唯一标志 需要进行检查
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
}
