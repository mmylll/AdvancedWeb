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
    public static final Double BASE_RADIUS = 3.0;
    public static final Double RADIUS_STEP = 1.0;
    public static final Double HEIGHT = 1.0;

    private int index;
    private Double radius;
    private Double height = HEIGHT;
}
