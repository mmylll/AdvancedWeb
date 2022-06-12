package com.example.backend;

import com.example.backend.config.SocketIOConfig;
import com.example.backend.model.Column;
import com.example.backend.model.Plate;
import com.example.backend.model.Player;
import com.example.backend.model.Room;
import com.example.backend.utils.HanoiUtils;
import com.example.backend.utils.JWTUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.util.List;

@SpringBootApplication
@EnableConfigurationProperties({
        JWTUtils.class,
        SocketIOConfig.class
})
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
        HanoiUtils.init();
    }


}
