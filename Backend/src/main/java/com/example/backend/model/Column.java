package com.example.backend.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Column {
    // 柱子的固定高度
    public static final Double HEIGHT = 100.0;

    // 柱子上的圆盘列表
    private List<Plate> plates = new ArrayList<>();
    // 柱子自身的高度
    private Double height = HEIGHT;
}
