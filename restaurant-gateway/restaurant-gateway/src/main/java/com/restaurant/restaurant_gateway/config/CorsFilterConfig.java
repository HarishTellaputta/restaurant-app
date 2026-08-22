package com.restaurant.restaurant_gateway.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
public class CorsFilterConfig extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String origin = request.getHeader("Origin");

        if (origin != null) {
            response.setHeader(
                    "Access-Control-Allow-Origin",
                    origin
            );

            response.setHeader(
                    "Vary",
                    "Origin"
            );

            response.setHeader(
                    "Access-Control-Allow-Methods",
                    "GET,POST,PUT,DELETE,PATCH,OPTIONS"
            );

            response.setHeader(
                    "Access-Control-Allow-Headers",
                    "*"
            );

            response.setHeader(
                    "Access-Control-Expose-Headers",
                    "Authorization"
            );
        }

        // Browser preflight
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {

            response.setStatus(
                    HttpServletResponse.SC_OK
            );

            return;
        }

        filterChain.doFilter(request, response);
    }
}