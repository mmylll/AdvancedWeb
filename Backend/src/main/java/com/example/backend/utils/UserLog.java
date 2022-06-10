package com.example.backend.utils;

import lombok.Data;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "log")
public class UserLog implements Serializable {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private LocalDateTime date;

    private Integer plate;

    public UserLog() {

    }

    public UserLog(String username, String type, LocalDateTime date, Integer plate) {
        this.username = username;
        this.type = type;
        this.date = date;
        this.plate = plate;
    }
}
