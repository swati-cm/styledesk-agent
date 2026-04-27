package com.styledesk;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {

    @Override
    public void filter(ContainerRequestContext request) {
        // Handle preflight OPTIONS request
        if (request.getMethod().equalsIgnoreCase("OPTIONS")) {
            request.abortWith(Response.ok()
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Methods",
                    "GET, POST, PUT, DELETE, OPTIONS")
                .header("Access-Control-Allow-Headers",
                    "Content-Type, Authorization, X-API-Key, Accept")
                .header("Access-Control-Max-Age", "86400")
                .build());
        }
    }

    @Override
    public void filter(ContainerRequestContext request,
                       ContainerResponseContext response) {
        // Add CORS headers to every response
        response.getHeaders().add(
            "Access-Control-Allow-Origin", "*");
        response.getHeaders().add(
            "Access-Control-Allow-Methods",
            "GET, POST, PUT, DELETE, OPTIONS");
        response.getHeaders().add(
            "Access-Control-Allow-Headers",
            "Content-Type, Authorization, X-API-Key, Accept");
        response.getHeaders().add(
            "Access-Control-Expose-Headers",
            "Content-Type, X-API-Key");
    }
}