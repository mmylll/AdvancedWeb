package com.example.backend.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix="plate")
public class Plate {
    private int index;
    private int radius;
    @Value("${plate.height}")
    private int height;

    public Plate(){

    }

    public Plate(Integer index, int radius, int height) {
        this.index = index;
        this.radius = radius;
        this.height = height;
    }
}
