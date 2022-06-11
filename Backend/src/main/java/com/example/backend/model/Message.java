package com.example.backend.model;

import lombok.Data;
import java.util.Date;

@Data
public class Message {
    private String username;
    private String message;
    private Date date;

    public Message(){

    }

    public Message(String username, String message) {
        this.username = username;
        this.message = message;
        this.date = new Date();
    }
}
