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
import com.chatapp.chatapp.util.ApplicationLogger;

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
         
      // Skip authentication for login endpoint
      if (request.getServletPath().contains("/api/v1/auth/authenticate") || request.getServletPath().contains("/ws")) {
        ApplicationLogger.requestLog(request, "skipping authentication");
        filterChain.doFilter(request, response);
        return;
      }
      

      final String authHeader = request.getHeader("Authorization");
      final String refreshHeader = request.getHeader("X-Refresh-Token"); //header for refresh token
      
      // Handle case where no auth headers are present
      if (authHeader == null && refreshHeader == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.getWriter().write("Authorization header missing");
        ApplicationLogger.requestLog(request, "Authorization header missing", HttpServletResponse.SC_UNAUTHORIZED, null);
        return; 
      }
      //
      //ONLY ACCESS TOKEN
      //
      if(authHeader != null && refreshHeader == null){
        if (!authHeader.startsWith("Bearer ")) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Invalid authorization format");
          ApplicationLogger.requestLog(request, "Invalid authorization format (missing Bearer)", HttpServletResponse.SC_UNAUTHORIZED, authHeader);
          return; 
        }

        
        final String jwt = authHeader.substring(7);
        UserDetails userDetails;
        JwtValidationResult validationResult = jwtService.validateToken(jwt);
        
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            
            try{
              userDetails = this.userDetailsService.loadUserByUsername(validationResult.getUsername());
            } catch (UsernameNotFoundException e){
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid token " + validationResult.getStatus());
              ApplicationLogger.requestLog(request, "Invalid access token", HttpServletResponse.SC_UNAUTHORIZED, authHeader, validationResult.getUsername(), validationResult.getStatus().toString(), e.getMessage());
              return;
            }
          
            // Only validate JWT signature and expiration, no database check
            if (validationResult.isValid()) {
              setAuthentication(request, userDetails);
              ApplicationLogger.requestLog(request, "Valid access token", HttpServletResponse.SC_OK, authHeader, validationResult.getUsername(), validationResult.getStatus().toString());
            } else {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid token " + validationResult.getStatus());
              ApplicationLogger.requestLog(request, "Invalid access token", HttpServletResponse.SC_UNAUTHORIZED, authHeader, validationResult.getUsername(), validationResult.getStatus().toString());
              return; 
            }
        }

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
          ApplicationLogger.requestLog(request, "Authorization header missing", HttpServletResponse.SC_UNAUTHORIZED, null, null);
          return; 
        }

        if (!request.getServletPath().contains("/api/v1/auth/refresh")) {
          response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
          response.getWriter().write("wrong endpoint");
          ApplicationLogger.requestLog(request, "wrong endpoint", HttpServletResponse.SC_BAD_REQUEST, authHeader, refreshHeader);
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
              response.getWriter().write("Invalid token " + validationResultAccess.getStatus());
              ApplicationLogger.requestLog(request, "Invalid access token", HttpServletResponse.SC_UNAUTHORIZED, authHeader, refreshHeader, validationResultAccess.getUsername(), validationResultAccess.getStatus().toString(), e.getMessage());
              return;
            }

            if(validationResultAccess.isUsableEvenIfExpired()){
              isAccessTokenValid = true;
            }else {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid token " + validationResultAccess.getStatus());
              ApplicationLogger.requestLog(request, "Invalid access token", HttpServletResponse.SC_UNAUTHORIZED, authHeader, refreshHeader, validationResultAccess.getUsername(), validationResultAccess.getStatus().toString());
              return; 
            }
        }

        //REFRESH TOKEN
        final String refreshToken = refreshHeader.substring(7);
        UserDetails userDetailsRefresh;
        JwtValidationResult validationResultRefresh = jwtService.validateToken(refreshToken);	
        
        //must be outside of the if below because we check if username of both tokens is the same
        try{
          userDetailsRefresh = this.userDetailsService.loadUserByUsername(validationResultRefresh.getUsername());
        } catch (UsernameNotFoundException e){
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Invalid token " + validationResultRefresh.getStatus());
          ApplicationLogger.requestLog(request, "Invalid refresh token", HttpServletResponse.SC_UNAUTHORIZED, authHeader, refreshHeader, validationResultRefresh.getUsername(), validationResultRefresh.getStatus().toString(), e.getMessage());
          return;
        }

        if(!isAccessTokenValid){
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Invalid token " + validationResultAccess.getStatus());
          ApplicationLogger.requestLog(request, "Invalid access token", HttpServletResponse.SC_UNAUTHORIZED, authHeader, validationResultAccess.getUsername(), validationResultAccess.getStatus().toString());
          return;
        }

        if(!validationResultRefresh.getUsername().equals(validationResultAccess.getUsername())){
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Unauthorized tokens");
          ApplicationLogger.requestLog(request, "Token holders dont match", HttpServletResponse.SC_UNAUTHORIZED, authHeader, refreshHeader);
          return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {  
            //database check for refresh tokens
            var isTokenInDatabase = tokenRepository.findByToken(refreshToken)
                .map(t -> !t.isExpired() && !t.isRevoked())
                .orElse(false);
                
            if (validationResultRefresh.isValid() && isTokenInDatabase) {
                setAuthentication(request, userDetailsRefresh);
                ApplicationLogger.requestLog(request, "Valid refresh and access token", HttpServletResponse.SC_OK, authHeader, refreshHeader, validationResultRefresh.getUsername(), validationResultRefresh.getStatus().toString());
            }else {
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid token " + validationResultRefresh.getStatus());
              ApplicationLogger.requestLog(request, "Invalid refresh token", HttpServletResponse.SC_UNAUTHORIZED, authHeader, refreshHeader, validationResultRefresh.getUsername(), validationResultRefresh.getStatus().toString());
              return; 
            }
        }
      
        // Continue filter chain 
        filterChain.doFilter(request, response);
      }
    }    
  
    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authToken);
    }
  }
