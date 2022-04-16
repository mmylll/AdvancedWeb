package com.example.backend.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一的公共响应体
 */

@Data
@AllArgsConstructor
public class ResponseResult implements Serializable {
    /**
     * 返回状态码
     */
    private Integer status;
    /**
     * 返回信息
     */
    private String message;
    /**
     * 数据
     */
    private Object data;

}