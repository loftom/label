package com.medlabel;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.medlabel.mapper")
public class LabelSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(LabelSystemApplication.class, args);
    }
}
