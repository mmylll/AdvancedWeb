package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    // 用户名
    private String username;

    // 位置坐标（x,y,z）
    private Number x;
    private Number y;
    private Number z;

    // 朝向向量（rx,ry.rz）
    private Number rx;
    private Number ry;
    private Number rz;

    // 持有的圆盘编号
    private Integer plate;
}
