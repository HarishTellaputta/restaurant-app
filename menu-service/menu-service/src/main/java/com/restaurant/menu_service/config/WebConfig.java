package com.restaurant.menu_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.file.upload-dir}")
    private String uploadDir;


    @Override
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {

        Path uploadPath =
                Path.of(uploadDir)
                        .toAbsolutePath()
                        .normalize();


        registry.addResourceHandler(
                        "/uploads/food-items/**"
                )
                .addResourceLocations(
                        uploadPath.toUri().toString()
                );
    }
}