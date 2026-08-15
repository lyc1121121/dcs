package com.dcsserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-KEY";

    private final String managerApiKey;
    private final String agentApiKey;

    public ApiKeyAuthFilter(@Value("${server-security.api-key}") String managerApiKey,
                             @Value("${agent-client.api-key}") String agentApiKey) {
        this.managerApiKey = managerApiKey;
        this.agentApiKey = agentApiKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!request.getRequestURI().startsWith("/api/")) {
            chain.doFilter(request, response);
            return;
        }

        String provided = request.getHeader(HEADER);
        if (provided == null || !(provided.equals(managerApiKey) || provided.equals(agentApiKey))) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"error\":\"invalid or missing " + HEADER + "\"}");
            return;
        }
        chain.doFilter(request, response);
    }
}
