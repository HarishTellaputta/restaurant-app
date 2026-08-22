package com.restaurant.auth.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class DebugRequestFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        System.out.println("=================================");
        System.out.println("REQUEST");
        System.out.println("METHOD  : " + request.getMethod());
        System.out.println("URI     : " + request.getRequestURI());
        System.out.println("ORIGIN  : " + request.getHeader("Origin"));
        System.out.println("AUTH    : " + request.getHeader("Authorization"));
        System.out.println("=================================");

        filterChain.doFilter(request, response);

        System.out.println("RESPONSE STATUS: " + response.getStatus());
    }
}