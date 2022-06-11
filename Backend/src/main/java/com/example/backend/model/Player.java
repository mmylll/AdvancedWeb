package com.example.backend.model;

import lombok.Data;

@Data
public class Player {
    String username;
    private int x;
    private int y;
    private int z;
    private double rx;
    private double ry;
    private double rz;
    private Plate plate;

    public Player(){

    }

    public Player(String username, int x, int y, int z, double rx, double ry, double rz, Plate plate) {
        this.username = username;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rx = rx;
        this.ry = ry;
        this.rz = rz;
        this.plate = plate;
    }
}
