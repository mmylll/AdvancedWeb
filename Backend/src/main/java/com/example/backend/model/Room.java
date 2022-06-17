package com.example.backend.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class Room {
    // 固定的柱子数量
    public static final int COLUMN_NUMBER = 3;
    // 可配置的圆盘数量
    public static int plateNumber = 3;

    // 柱子的列表
    private List<Column> columns;
    // 玩家的列表
    private Map<UUID, Player> players;
    // Whether someone has picked up a plate
    private boolean someonePickUp;

    // 单例
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
