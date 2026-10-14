package com.example.mysqlmcp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MysqlMcpServerApplication {

    public static void main(String[] args) {
        // Ensure standard error gets early framework notices if any
        SpringApplication.run(MysqlMcpServerApplication.class, args);
    }
}
