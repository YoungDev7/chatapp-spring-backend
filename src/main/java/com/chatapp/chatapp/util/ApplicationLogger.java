package com.chatapp.chatapp.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletRequest;

 
public class ApplicationLogger {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationLogger.class);

    //TODO: refactor this class to use a more structured logging approach, possibly with MDC (Mapped Diagnostic Context) for better context in logs (copilot suggestion)
    
    //request, message
    public static void requestLogFilter(HttpServletRequest request, String message){
        String path = request.getServletPath();
        String clientIP = request.getRemoteAddr();
        logger.info("REQUEST[-] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->clientIP: {}\n", message, path, clientIP);
    }

    //request, message, statusCode, access token authHeader
    public static void requestLogFilter(HttpServletRequest request, String message, int statusCode, String authHeader){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->authHeader: {}\n" +
                    "   ->clientIP: {}\n", statusCode, message, path, authHeader, clientIP);
    }

    //request, message, user uid, statusCode, 
    public static void requestLogFilter(HttpServletRequest request, String message, String userUID,int statusCode){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->userUID: {}\n" +
                    "   ->clientIP: {}\n", statusCode, message, path, userUID, clientIP);
    }

    //request, message, statusCode, access token authHeader, refresh token authHeader
    public static void requestLogFilter(HttpServletRequest request, String message, int statusCode, String accessAuthHeader, String refreshAuthHeader){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->accessAuthHeader: {}\n" +
                    "   ->refreshAuthHeader: {}\n" +
                    "   ->clientIP: {}\n", statusCode, message, path, accessAuthHeader, refreshAuthHeader, clientIP);
    }

    //request, message, statusCode, access token authHeader, user, validation status, exception
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
    
    //request, message, statusCode, access token authHeader, refresh token authHeader, user, validation status, exception
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

    //request, message, statusCode, access token authHeader, user, validation status
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

    //request, message, access token authHeader, user, validation status
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

    

    public static void websocketConnectionLog(String message, String user, String accessorCommand, String channel, String token){
        logger.info("WebSocket Connection[{}] {}\n" + 
                    "   ->user: {}\n" +
                    "   ->token: {}\n" +
                    "   ->channel: {}\n", accessorCommand, message, user, token, channel);
    }

    //request, message, statusCode, access token authHeader
    public static void websocketConnectionLog(HttpServletRequest request, String message, int statusCode, String authHeader){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->authHeader: {}\n" +
                    "   ->clientIP: {}\n", statusCode, message, path, authHeader, clientIP);
    }

    //request, message, user, statusCode, 
    public static void requestLog(HttpServletRequest request, String message, String user, int statusCode){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->userUID: {}\n" +
                    "   ->clientIP: {}\n", statusCode, message, path, user, clientIP);
    }

    //request, message, user, statusCode, exception
    public static void requestLog(HttpServletRequest request, String message, String user, int statusCode, String exception){
        String clientIP = request.getRemoteAddr();
        String path = request.getServletPath();
        logger.info("REQUEST[{}] {}\n" + 
                    "   ->path: {}\n" +
                    "   ->userUID: {}\n" +
                    "   ->clientIP: {}\n" +
                    "   ->exception: {}\n", statusCode, message, path, user, clientIP, exception);
    }

    public static void debugLog(String message){
        logger.debug("DEBUG: {}", message);
    }

    public static void warningLog(String message){
        logger.warn("WARNING: {}", message);
    }

    public static void errorLog(String message){
        logger.error("ERROR: {}", message);
    }

}
