package com.example.backend.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Room {
    public static final int COLUMN_NUMBER = 3;
    public static int plateNumber = 3;

    private List<Column> columns;
    private Map<String, Player> players;

    private static Room room;

    private Room() {
        this.columns = new ArrayList<>();
        this.players = new ConcurrentHashMap<>();
    }

    public static Room getRoom() {
        if (room == null)
            room = new Room();
        return room;
    }
}
