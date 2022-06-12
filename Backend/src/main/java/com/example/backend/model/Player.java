package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String username;

    private Double x;
    private Double y;
    private Double z;

    private Double rx;
    private Double ry;
    private Double rz;

    private Integer plate;
}
