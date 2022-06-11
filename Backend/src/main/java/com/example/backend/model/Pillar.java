package com.example.backend.model;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.TreeMap;

@Data
@Component
@ConfigurationProperties(prefix="column")
public class Pillar {
    private int index;
    private TreeMap<Integer,Plate> plates = new TreeMap<>();
    @Value("${column.height}")
    private int height;

    public Pillar() {

    }

    public Pillar(int index, TreeMap<Integer, Plate> plates, int height) {
        this.index = index;
        this.plates = plates;
        this.height = height;
    }
}
