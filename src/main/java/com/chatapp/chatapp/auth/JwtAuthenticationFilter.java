package com.chatapp.chatapp.auth;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.chatapp.chatapp.repository.TokenRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter{
    
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
         
      // Skip authentication for auth endpoints
      if (request.getServletPath().contains("/api/v1/auth") && !request.getServletPath().contains("/api/v1/auth/refresh")) {
        System.out.println("[DEBUG] Skipping auth for " + request.getServletPath());
        filterChain.doFilter(request, response);
        return;
      }
      
      //System.out.println("[DEBUG] auth not skipped");

      final String authHeader = request.getHeader("Authorization");
      final String refreshHeader = request.getHeader("X-Refresh-Token"); //header for refresh token
      
      // Handle case where no auth headers are present
      if (authHeader == null && refreshHeader == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Authorization header missing");
        return; 
      }
      //
      //ONLY ACCESS TOKEN
      //
      if(authHeader != null && refreshHeader == null){
        if (!authHeader.startsWith("Bearer ")) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Invalid authorization format");
          System.out.println("[LOG] Invalid authorization format (missing Bearer) for " + request.getServletPath());
          System.out.println("  auth header: " + authHeader);
          return; 
        }

        //System.out.println("[DEBUG] valid header");
        
        final String jwt = authHeader.substring(7);
        UserDetails userDetails;
        JwtValidationResult validationResult = jwtService.validateToken(jwt);
        
        //System.out.println("no jwt or user details problesm");
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            //System.out.println("inside of if");
            try{
              userDetails = this.userDetailsService.loadUserByUsername(validationResult.getUsername());
            } catch (UsernameNotFoundException e){
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid token");
              System.out.println("[LOG] Invalid access token for " + request.getServletPath());
              System.out.println("  exception: " + e.getMessage());
              System.out.println("  auth header: " + authHeader);
              return;
            }
        
            // Only validate JWT signature and expiration, no database check
            if (validationResult.isValid()) {
              setAuthentication(request, userDetails);
            } else {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid token");
              System.out.println("[LOG] Invalid access token for " + request.getServletPath());
              System.out.println("status: " + validationResult.getStatus());
              System.out.println("  auth header: " + authHeader);
              return; 
            }
        }
        //System.out.println("[DEBUG] outside of if ");
        // If we got here, authentication was successful
        filterChain.doFilter(request, response);
        return;
      }
      
      //
      // REFRESH & ACCESS TOKEN
      //
      if(authHeader != null && refreshHeader != null){
        if ((authHeader == null || !authHeader.startsWith("Bearer ")) && (refreshHeader == null || !refreshHeader.startsWith("Bearer "))) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Invalid authorization format");
          System.out.println("[LOG] Invalid authorization format (missing Bearer) for " + request.getServletPath());
          System.out.println("  auth header: " + authHeader);
          System.out.println("  refresh header: " + refreshHeader);
          return; 
        }

        if (!request.getServletPath().contains("/api/v1/auth/refresh")) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          response.getWriter().write("wrong endpoint");
          return; 
        }

        //ACCESS TOKEN
        final String jwt = authHeader.substring(7);
        UserDetails userDetails;
        JwtValidationResult validationResultAccess = jwtService.validateToken(jwt);
        boolean isAccessTokenValid = false;
        
        if (SecurityContextHolder.getContext().getAuthentication() == null) {            
            // Only validate JWT signature and expiration, no database check
            try{
              userDetails = this.userDetailsService.loadUserByUsername(validationResultAccess.getUsername());
            } catch (UsernameNotFoundException e){
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid token");
              System.out.println("[LOG] Invalid access token for " + request.getServletPath());
              System.out.println("  exception: " + e.getMessage());
              System.out.println("  auth header: " + authHeader);
              System.out.println("  refresh header: " + refreshHeader);
              return;
            }

            if(validationResultAccess.isUsableEvenIfExpired()){
              isAccessTokenValid = true;
            }else {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid token");
              System.out.println("[LOG] Invalid access token for " + request.getServletPath());
              System.out.println("status: " + validationResultAccess.getStatus());
              System.out.println("  auth header: " + authHeader);
              System.out.println("  refresh header: " + refreshHeader);
              return; 
            }
        }

        //REFRESH TOKEN
        final String refreshToken = refreshHeader.substring(7);
        UserDetails userDetailsRefreshToken;
        JwtValidationResult validationResultRefresh = jwtService.validateToken(refreshToken);	
        
        //must be outside of the if below because we check if username of both tokens is the same
        try{
          userDetailsRefreshToken = this.userDetailsService.loadUserByUsername(validationResultRefresh.getUsername());
        } catch (UsernameNotFoundException e){
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Invalid token");
          System.out.println("[LOG] Invalid refresh token for " + request.getServletPath());
          System.out.println("  exception: " + e.getMessage());
          System.out.println("  auth header: " + authHeader);
          System.out.println("  refresh header: " + refreshHeader);
          return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null && isAccessTokenValid && validationResultRefresh.getUsername().equals(validationResultAccess.getUsername())) {  
            //database check for refresh tokens
            var isTokenInDatabase = tokenRepository.findByToken(refreshToken)
                .map(t -> !t.isExpired() && !t.isRevoked())
                .orElse(false);
                
            if (validationResultRefresh.isValid() && isTokenInDatabase) {
                setAuthentication(request, userDetailsRefreshToken);
            }else {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid token");
              System.out.println("[LOG] Invalid Refresh token for " + request.getServletPath());
              System.out.println("status: " + validationResultRefresh.getStatus());
              System.out.println("in database: " + isTokenInDatabase);
              System.out.println("  auth header: " + authHeader);
              System.out.println("  refresh header: " + refreshHeader);
              return; 
            }
        }
      
        // Continue filter chain regardless of authentication result
        filterChain.doFilter(request, response);
      }
    }    
  
    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authToken);
    }
  }
