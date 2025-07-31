package backend.models;

public class UserSession {
    private static UserSession instance;
    private String username;
    private String fullName;
    private String sessionId;
    private boolean loggedIn = false;
    
    private UserSession() {}
    
    public static UserSession getInstance() {
        if (instance == null) {
            instance = new UserSession();
        }
        return instance;
    }
    
    public void login(String username, String fullName, String sessionId) {
        this.username = username;
        this.fullName = fullName;
        this.sessionId = sessionId;
        this.loggedIn = true;
        System.out.println("👤 User session started for: " + username);
    }
    
    public void logout() {
        if (loggedIn && username != null) {
            System.out.println("👤 User session ended for: " + username);
        }
        this.username = null;
        this.fullName = null;
        this.sessionId = null;
        this.loggedIn = false;
    }
    
    public boolean isLoggedIn() {
        return loggedIn && username != null;
    }
    
    public String getUsername() {
        return username != null ? username : "Guest";
    }
    
    public String getFullName() {
        return fullName != null ? fullName : "Guest";
    }
    
    public String getSessionId() {
        return sessionId;
    }
}