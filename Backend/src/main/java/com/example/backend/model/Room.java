package com.example.backend.model;

import lombok.Data;

@Data
public class Room {
    private Pillar[] pillars;
    private Player[] players;

    public Room() {

    }

    public Room(Pillar[] pillars, Player[] players) {
        this.pillars = pillars;
        this.players = players;
    }
}
