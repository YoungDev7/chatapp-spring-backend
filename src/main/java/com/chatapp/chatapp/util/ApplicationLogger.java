package com.chatapp.chatapp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Utility class for structured application logging throughout the chat application.
 * 
 * This class provides static methods for logging various types of events including
 * HTTP requests, WebSocket connections, authentication events, and general application
 * messages. It uses SLF4J for logging and formats messages with contextual information
 * such as client IP, request paths, user details, and status codes.
 * 
 * TODO: Refactor this class to use a more structured logging approach, possibly with 
 * MDC (Mapped Diagnostic Context) for better context in logs.
 * 
 */
public class ApplicationLogger {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationLogger.class);

    /**
     * Logs basic request information without status code.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     */
    public static void requestLogFilter(HttpServletRequest request, String message){
        String path = request.getServletPath();
        String clientIP = request.getRemoteAddr();
        logger.info("REQUEST[-] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->clientIP: {}\n", message, path, clientIP);
    }

    /**
     * Logs request information with status code and authorization header.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     * @param statusCode the HTTP status code
     * @param authHeader the authorization header value
     */
    public static void requestLogFilter(HttpServletRequest request, String message, int statusCode, String authHeader){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->authHeader: {}\n" +
                    "   ->clientIP: {}\n", statusCode, message, path, authHeader, clientIP);
    }

    /**
     * Logs request information with user UID and status code.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     * @param userUID the user's unique identifier
     * @param statusCode the HTTP status code
     */
    public static void requestLogFilter(HttpServletRequest request, String message, String userUID,int statusCode){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->userUID: {}\n" +
                    "   ->clientIP: {}\n", statusCode, message, path, userUID, clientIP);
    }

    /**
     * Logs request information with both access and refresh token headers.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     * @param statusCode the HTTP status code
     * @param accessAuthHeader the access token authorization header
     * @param refreshAuthHeader the refresh token authorization header
     */
    public static void requestLogFilter(HttpServletRequest request, String message, int statusCode, String accessAuthHeader, String refreshAuthHeader){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->accessAuthHeader: {}\n" +
                    "   ->refreshAuthHeader: {}\n" +
                    "   ->clientIP: {}\n", statusCode, message, path, accessAuthHeader, refreshAuthHeader, clientIP);
    }

    /**
     * Logs detailed request information including validation status and exception details.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     * @param statusCode the HTTP status code
     * @param authHeader the authorization header value
     * @param user the username or user identifier
     * @param validationStatus the token validation status
     * @param exception the exception message if any
     */
    public static void requestLogFilter(HttpServletRequest request, String message, int statusCode, String authHeader, String user, String validationStatus, String exception){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
        "   ->path: {}\n" +
        "   ->authHeader: {}\n" +
        "   ->clientIP: {}\n" +
        "   ->user: {}\n" + 
        "   ->status: {}\n" +
        "   ->exception: {}\n", statusCode, message, path, authHeader, clientIP, user, validationStatus, exception);
    }
    
    /**
     * Logs comprehensive request information with dual auth headers, user details, and exception.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     * @param statusCode the HTTP status code
     * @param accessAuthHeader the access token authorization header
     * @param refreshAuthHeader the refresh token authorization header
     * @param user the username or user identifier
     * @param validationStatus the token validation status
     * @param exception the exception message if any
     */
    public static void requestLogFilter(HttpServletRequest request, String message, int statusCode, String accessAuthHeader, String refreshAuthHeader, String user, String validationStatus, String exception){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->accessAuthHeader: {}\n" +
                    "   ->refreshAuthHeader: {}\n" +
                    "   ->clientIP: {}\n" +
                    "   ->user: {}\n" + 
                    "   ->status: {}\n" +
                    "   ->exception: {}\n", statusCode, message, path, accessAuthHeader, refreshAuthHeader, clientIP, user, validationStatus, exception);
    }

    /**
     * Logs request information with user details and validation status.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     * @param statusCode the HTTP status code
     * @param authHeader the authorization header value
     * @param user the username or user identifier
     * @param validationStatus the token validation status
     */
    public static void requestLogFilter(HttpServletRequest request, String message, int statusCode, String authHeader, String user, String validationStatus){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->authHeader: {}\n" +
                    "   ->clientIP: {}\n" +
                    "   ->user: {}\n" + 
                    "   ->status: {}\n", statusCode, message, path, authHeader, clientIP, user, validationStatus);
    }

    /**
     * Logs request information without status code but with user and validation details.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     * @param authHeader the authorization header value
     * @param user the username or user identifier
     * @param validationStatus the token validation status
     */
    public static void requestLogFilter(HttpServletRequest request, String message, String authHeader, String user, String validationStatus){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[-] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->authHeader: {}\n" +
                    "   ->clientIP: {}\n" +
                    "   ->user: {}\n" + 
                    "   ->status: {}\n",  message, path, authHeader, clientIP, user, validationStatus);
    }

    /**
     * Logs WebSocket connection events with detailed connection information.
     * 
     * @param message the log message
     * @param user the username or user identifier
     * @param accessorCommand the WebSocket accessor command (CONNECT, DISCONNECT, etc.)
     * @param channel the WebSocket channel or destination
     * @param token the authentication token used for the connection
     */
    public static void websocketConnectionLog(String message, String user, String accessorCommand, String channel, String token){
        logger.info("WebSocket Connection[{}] {}\n" + 
                    "   ->user: {}\n" +
                    "   ->token: {}\n" +
                    "   ->channel: {}\n", accessorCommand, message, user, token, channel);
    }

    /**
     * Logs WebSocket connection events with HTTP request context.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     * @param statusCode the HTTP status code
     * @param authHeader the authorization header value
     */
    public static void websocketConnectionLog(HttpServletRequest request, String message, int statusCode, String authHeader){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->authHeader: {}\n" +
                    "   ->clientIP: {}\n", statusCode, message, path, authHeader, clientIP);
    }

    /**
     * Logs general request events with user information.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     * @param user the username or user identifier
     * @param statusCode the HTTP status code
     */
    public static void requestLog(HttpServletRequest request, String message, String user, int statusCode){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->userUID: {}\n" +
                    "   ->clientIP: {}\n", statusCode, message, path, user, clientIP);
    }

    /**
     * Logs request events with user information and exception details.
     * 
     * @param request the HTTP servlet request
     * @param message the log message
     * @param user the username or user identifier
     * @param statusCode the HTTP status code
     * @param exception the exception message
     */
    public static void requestLog(HttpServletRequest request, String message, String user, int statusCode, String exception){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->userUID: {}\n" +
                    "   ->clientIP: {}\n" +
                    "   ->exception: {}\n", statusCode, message, path, user, clientIP, exception);
    }

    /**
     * Logs debug-level messages for development and troubleshooting.
     * 
     * @param message the debug message
     */
    public static void debugLog(String message){
        logger.debug("DEBUG: {}", message);
    }

    /**
     * Logs warning-level messages for potentially problematic situations.
     * 
     * @param message the warning message
     */
    public static void warningLog(String message){
        logger.warn("WARNING: {}", message);
    }

    /**
     * Logs error-level messages for application errors and exceptions.
     * 
     * @param message the error message
     */
    public static void errorLog(String message){
        logger.error("ERROR: {}", message);
    }

    /**
     * Logs informational messages for general application events.
     * 
     * @param message the informational message
     */
    public static void infoLog(String message){
        logger.info("INFO: {}", message);
    }

}
