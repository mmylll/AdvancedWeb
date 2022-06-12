package com.example.backend.model;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Column {
    public static final Double HEIGHT = 100.0;

    private List<Plate> plates = new ArrayList<>();
    private Double height = HEIGHT;
}
