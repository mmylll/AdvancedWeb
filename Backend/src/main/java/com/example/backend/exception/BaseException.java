package com.example.backend.exception;

import com.example.backend.response.ResponseCode;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 自定义异常
 */

@Data
@EqualsAndHashCode(callSuper = false)
public class BaseException extends RuntimeException{

    private ResponseCode code;

    public BaseException(ResponseCode code) {
        this.code = code;
    }

    public BaseException(Throwable cause, ResponseCode code) {
        super(cause);
        this.code = code;
    }
}