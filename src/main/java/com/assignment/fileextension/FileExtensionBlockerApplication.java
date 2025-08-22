package com.assignment.fileextension;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FileExtensionBlockerApplication {
    public static void main(String[] args) {
        SpringApplication.run(FileExtensionBlockerApplication.class, args);
    }
}