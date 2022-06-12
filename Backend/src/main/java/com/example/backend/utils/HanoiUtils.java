package com.example.backend.utils;

import com.example.backend.model.Column;
import com.example.backend.model.Plate;
import com.example.backend.model.Room;

import java.util.List;

public class HanoiUtils {
    public static Room init() {
        Room room = Room.getRoom();

        // 初始化柱子
        List<Column> columns = room.getColumns();
        for (int i = 0; i < Room.COLUMN_NUMBER; i++) {
            columns.add(new Column());
        }

        // 把所有plates按照从大到小的顺序放入第一根柱子
        List<Plate> plates = columns.get(0).getPlates();
        for (int i = Room.plateNumber - 1; i >= 0; i--) {
            Plate plate = new Plate(i, Plate.BASE_RADIUS + i * Plate.RADIUS_STEP, Plate.HEIGHT);
            plates.add(plate);
        }
        return room;
    }
}
