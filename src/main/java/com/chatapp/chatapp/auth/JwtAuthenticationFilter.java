package com.chatapp.chatapp.auth;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.chatapp.chatapp.Dto.JwtValidationResult;
import com.chatapp.chatapp.config.SecurityConfig;
import com.chatapp.chatapp.repository.TokenRepository;
import com.chatapp.chatapp.service.JwtService;
import com.chatapp.chatapp.util.LoggerUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;


@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter{
    
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final TokenRepository tokenRepository;
    private final LoggerUtil loggerUtil;
    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        
        for (String whitelistedPath : SecurityConfig.WHITE_LIST_URL) {
            String pathToMatch = whitelistedPath.replace("/**", "");
            if (path.startsWith(pathToMatch)) {
                log.debug("Skipping JWT filter for whitelisted path: {}", path);
                return true;
            }
        }
        
        return false;
    }
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    throws ServletException, IOException {
      loggerUtil.setupRequestContext(request);

      try{
        final String authHeader = request.getHeader("Authorization");
        String refreshToken = extractRefreshTokenFromCookie(request);
        
        //
        //ACCESS TOKEN
        //
        if (authHeader == null) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Authorization header missing and no refresh token in cookies");
          log.warn("[{}] Authorization header missing and no refresh token in cookies",
              HttpServletResponse.SC_UNAUTHORIZED);
          return;
        }

        if (!authHeader.startsWith("Bearer ")) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("Invalid authorization format");
          log.warn("[{}] Invalid authorization format (missing Bearer)", HttpServletResponse.SC_UNAUTHORIZED);
          return; 
        }

        //if refresh header is null while sending request to /refresh endpoint this if block catches it 
        if (request.getServletPath().contains("/api/v1/auth/refresh") && refreshToken == null) {
          response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
          response.getWriter().write("missing refresh token");
          log.warn("[{}] missing refresh token", HttpServletResponse.SC_UNAUTHORIZED);
          return; 
        }

        
        final String jwt = authHeader.substring(7);
        UserDetails userDetails;
        JwtValidationResult validationResultAccess = jwtService.validateToken(jwt);
        loggerUtil.setupUserContext(validationResultAccess.getUsername());
        
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            //TODO: this block needs refactor: it should say that user has not been found in the database...
            try{
              //database check TODO: remove
              userDetails = this.userDetailsService.loadUserByUsername(validationResultAccess.getUsername());
            } catch (UsernameNotFoundException e){
              response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
              response.getWriter().write("Invalid token " + validationResultAccess.getStatus());
              log.error("[{}] Invalid access token {}; {}", HttpServletResponse.SC_UNAUTHORIZED, 
                  validationResultAccess.getStatus().toString(), e.getMessage());
              return;
            }
            
            if(!request.getServletPath().contains("/api/v1/auth/refresh")){
              // Only validate JWT signature and expiration, no database check
              if (validationResultAccess.isValid()) {
                setAuthentication(request, userDetails);
              } else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid token " + validationResultAccess.getStatus());
                log.warn("[{}] Invalid access token {}", HttpServletResponse.SC_UNAUTHORIZED, 
                    validationResultAccess.getStatus().toString());
                return; 
              }
            }
        }

        //
        // REFRESH
        //
        if(refreshToken != null){

          if (!request.getServletPath().contains("/api/v1/auth/refresh")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("wrong endpoint");
            log.warn("[{}] wrong endpoint", HttpServletResponse.SC_BAD_REQUEST);
            return; 
          }

          loggerUtil.setupUserContext(validationResultAccess.getUsername());
          
          if (!validationResultAccess.isUsableEvenIfExpired()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token " + validationResultAccess.getStatus());
            log.warn("[{}] Invalid access token {}", HttpServletResponse.SC_UNAUTHORIZED,
                validationResultAccess.getStatus().toString());
            return;
          }

          //REFRESH TOKEN
          UserDetails userDetailsRefresh;
          JwtValidationResult validationResultRefresh = jwtService.validateToken(refreshToken);	
          loggerUtil.setupUserContext(validationResultRefresh.getUsername());
          
          try{
            userDetailsRefresh = this.userDetailsService.loadUserByUsername(validationResultRefresh.getUsername());
          } catch (UsernameNotFoundException e){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Invalid token " + validationResultRefresh.getStatus());
            log.error("[{}] Invalid refresh token {}; {}", HttpServletResponse.SC_UNAUTHORIZED, 
                validationResultRefresh.getStatus().toString(), e.getMessage());
            return;
          }

          if(!validationResultRefresh.getUsername().equals(validationResultAccess.getUsername())){
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("Unauthorized tokens");
            log.warn("[{}] Token holders don't match", HttpServletResponse.SC_UNAUTHORIZED);
            return;
          }

          if (SecurityContextHolder.getContext().getAuthentication() == null) {  
              var isTokenInDatabase = tokenRepository.findByToken(refreshToken)
                  .map(t -> !t.isRevoked())
                  .orElse(false);
                  
              if (validationResultRefresh.isValid() && isTokenInDatabase) {
                  setAuthentication(request, userDetailsRefresh);
              }else {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Invalid token " + validationResultRefresh.getStatus());
                log.warn("[{}] Invalid refresh token {}; isInDatabase: {}", 
                    HttpServletResponse.SC_UNAUTHORIZED, 
                    validationResultRefresh.getStatus().toString(), 
                    isTokenInDatabase);
                return; 
              }
          }
        }

        // If we got here, authentication was successful
        filterChain.doFilter(request, response);
      }finally{
        loggerUtil.clearContext();
      }
    }    

    private void setAuthentication(HttpServletRequest request, UserDetails userDetails) {
      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    private String extractRefreshTokenFromCookie(HttpServletRequest request){
      if (request.getCookies() == null) {
        return null;
      }

      for (Cookie cookie : request.getCookies()) {
        if ("refreshToken".equals(cookie.getName())) {
          return cookie.getValue();
        }
      }

      return null;
    }

  }
