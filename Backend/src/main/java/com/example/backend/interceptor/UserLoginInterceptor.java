package com.example.backend.interceptor;

import com.example.backend.exception.BaseException;
import com.example.backend.response.ResponseCode;
import com.example.backend.utils.JWTUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 拦截器：验证用户是否登录
 */

public class UserLoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        //http的header中获得token
        String token = request.getHeader(JWTUtils.TOKEN);
        //token不存在
        if (token == null || token.equals(""))
            throw new BaseException(ResponseCode.LOGIN_NOT);
        //验证token
        String sub = JWTUtils.validateToken(token);
        if (sub == null || sub.equals(""))
            throw new BaseException(ResponseCode.JWT_FALSE);
        //更新token有效时间 (如果需要更新其实就是产生一个新的token)
        if (JWTUtils.isNeedUpdate(token)){
            String newToken = JWTUtils.createToken(sub);
            response.setHeader(JWTUtils.TOKEN,newToken);
        }
        return true;
    }
}