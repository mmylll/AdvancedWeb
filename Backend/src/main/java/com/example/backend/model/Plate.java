package com.example.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Plate {
    // 圆盘的基础半径
    public static final Double BASE_RADIUS = 6.0;
    // 圆盘的半径步进（编号加1时半径的增长）
    public static final Double RADIUS_STEP = 2.5;
    // 圆盘的统一高度
    public static final Double HEIGHT = 6.0;

    // 圆盘编号（编号越大，半径越大）
    private int index;
    // 圆盘半径（等于基础半径 + 编号 * 半径步进）
    private Double radius;
    // 圆盘高度
    private Double height = HEIGHT;
}
