package com.ing.assesment.infra.auth.security.jwt;

import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.domain.auth.port.JwtTokenPort;
import com.ing.assesment.infra.auth.security.SecurityUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.security.core.context.SecurityContextHolder.getContext;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenPort jwtTokenPort;

    public JwtAuthenticationFilter(JwtTokenPort jwtTokenPort) {
        this.jwtTokenPort = jwtTokenPort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7);

        if (!jwtTokenPort.isAccessTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        Long userId = jwtTokenPort.extractUserId(token);
        String email = jwtTokenPort.extractEmail(token);
        Set<UserRole> roles = jwtTokenPort.extractRoles(token)
                .stream()
                .map(UserRole::valueOf)
                .collect(Collectors.toSet());

        SecurityUser securityUser = new SecurityUser(userId, email, roles);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        securityUser,
                        null,
                        roles.stream()
                                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                                .collect(Collectors.toSet())
                );

        getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
