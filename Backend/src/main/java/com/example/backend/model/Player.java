package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String username;

    private Number x;
    private Number y;
    private Number z;

    private Number rx;
    private Number ry;
    private Number rz;

    private Integer plate;
}
