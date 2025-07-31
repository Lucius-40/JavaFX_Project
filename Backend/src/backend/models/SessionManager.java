package backend.models;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private static Map<String, UserSession> activeSessions = new ConcurrentHashMap<>();
    private static final long SESSION_TIMEOUT = 24 * 30 * 60 * 1000; 
    
    public static class UserSession {
        private String sessionId;
        private String username;
        private long lastActivity;
        
        public UserSession(String sessionId, String username) {
            this.sessionId = sessionId;
            this.username = username;
            this.lastActivity = System.currentTimeMillis();
        }
        
        public void updateActivity() {
            this.lastActivity = System.currentTimeMillis();
        }
        
        public boolean isExpired() {
            return (System.currentTimeMillis() - lastActivity) > SESSION_TIMEOUT;
        }
        
        public String getSessionId() { return sessionId; }
        public String getUsername() { return username; }
        public long getLastActivity() { return lastActivity; }
    }
    
    public static String createSession(String username) {
        String sessionId = UUID.randomUUID().toString();
        UserSession session = new UserSession(sessionId, username);
        activeSessions.put(sessionId, session);
        
        System.out.println("Session created for user: " + username + " with ID: " + sessionId);
        return sessionId;
    }
    
    public static UserSession getSession(String sessionId) {
        UserSession session = activeSessions.get(sessionId);
        if (session != null) {
            if (session.isExpired()) {
                activeSessions.remove(sessionId);
                System.out.println("Session expired for user: " + session.getUsername());
                return null;
            }
            session.updateActivity();
        }
        return session;
    }
    
    public static boolean isValidSession(String sessionId) {
        return getSession(sessionId) != null;
    }
    
    public static void removeSession(String sessionId) {
        UserSession session = activeSessions.remove(sessionId);
        if (session != null) {
            System.out.println("Session removed for user: " + session.getUsername());
        }
    }
    
    public static void cleanupExpiredSessions() {
        Iterator<Map.Entry<String, UserSession>> iterator = activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, UserSession> entry = iterator.next();
            if (entry.getValue().isExpired()) {
                System.out.println("Cleaning up expired session for user: " + entry.getValue().getUsername());
                iterator.remove();
            }
        }
    }
    
    public static int getActiveSessionCount() {
        return activeSessions.size();
    }
}