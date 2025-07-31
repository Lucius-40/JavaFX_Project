package backend.models;

import java.io.*;
import java.util.*;

public class UserManager {
    private static Map<String, User> users = new HashMap<>();
    private static String USERS_FILE = "Backend/data/users.txt";
    
    public static void setUsersFilePath(String path) {
        USERS_FILE = path;
    }
    
    public static void loadUsers() {
        users.clear();
        File file = new File(USERS_FILE);
        
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
                System.out.println("Created new users file: " + USERS_FILE);
            } catch (IOException e) {
                System.err.println("Error creating users file: " + e.getMessage());
            }
            return;
        }
        
        try (BufferedReader reader = new BufferedReader(new FileReader(USERS_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                
                String[] userData = line.split("\\|");
                if (userData.length >= 8) {
                    User user = new User(userData);
                    users.put(user.getUsername().toLowerCase(), user);
                }
            }
            System.out.println("Loaded " + users.size() + " users from file");
        } catch (IOException e) {
            System.err.println("Error loading users: " + e.getMessage());
        }
    }
    
    public static boolean registerUser(String username, String password, String email, String fullName, String address, String phone) {
        if (username == null || username.trim().isEmpty() || 
            password == null || password.trim().isEmpty()) {
            return false;
        }
        
        if (users.containsKey(username.toLowerCase())) {
            System.out.println("User already exists: " + username);
            return false;
        }
        
        // Check if email already exists
        for (User user : users.values()) {
            if (user.getEmail().equalsIgnoreCase(email)) {
                System.out.println("Email already registered: " + email);
                return false;
            }
        }
        
        // Create new user
        User newUser = new User(username, password, email, fullName, address, phone);
        users.put(username.toLowerCase(), newUser);
        saveUsers();
        
        System.out.println("User registered successfully: " + username);
        return true;
    }
    
    public static User loginUser(String username, String password) {
        if (username == null || password == null) {
            return null;
        }
        
        User user = users.get(username.toLowerCase());
        if (user != null && user.isActive() && user.verifyPassword(password)) {
            System.out.println("User logged in successfully: " + username);
            return user;
        }
        
        System.out.println("Login failed for user: " + username);
        return null;
    }
    
    public static boolean userExists(String username) {
        return users.containsKey(username.toLowerCase());
    }
    
    public static boolean emailExists(String email) {
        return users.values().stream()
                .anyMatch(user -> user.getEmail().equalsIgnoreCase(email));
    }
    
    public static void saveUsers() {
        try (PrintWriter writer = new PrintWriter(new FileWriter(USERS_FILE))) {
            for (User user : users.values()) {
                writer.println(user.toFileString());
            }
            System.out.println("Users saved to file successfully");
        } catch (IOException e) {
            System.err.println("Error saving users: " + e.getMessage());
        }
    }
    
    public static int getUserCount() {
        return users.size();
    }
    
    public static User getUser(String username) {
        if (username == null) {
            return null;
        }
        return users.get(username.toLowerCase());
    }
}