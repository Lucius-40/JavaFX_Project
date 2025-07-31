package backend.models;
//C:\Lucius FIles\backup\Java-Project\OnS\README.md
import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String username;
    private String passwordHash;
    private String email;
    private String fullName;
    private String address;
    private String phone;
    private LocalDateTime registrationDate;
    private boolean isActive;
    
    public User(String username, String password, String email, String fullName, String address, String phone) {
        this.username = username;
        this.passwordHash = hashPassword(password);
        this.email = email;
        if(fullName != null){
            this.fullName = fullName;
        }else{
            this.fullName = "N/A";
        }
        this.address = address;
        if (phone != null) {
            this.phone = phone;
        }else{
            this.phone = "N/A";
        }
        this.registrationDate = LocalDateTime.now();
        this.isActive = true;
    }
    
    // Constructor for loading from file
    public User(String[] userData) {
        this.username = userData[0];
        this.passwordHash = userData[1];
        this.email = userData[2];
        this.fullName = userData[3];
        this.address = userData[4];
        this.phone = userData[5];
        this.registrationDate = LocalDateTime.parse(userData[6], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        this.isActive = Boolean.parseBoolean(userData[7]);
    }
    
    // Simple SHA-256 hashing
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashedBytes = md.digest(password.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashedBytes) {
                sb.append(String.format("%02x", b));
            }
            // System.out.println("Hashed Password: " + sb.toString());
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }
    
    public boolean verifyPassword(String password) {
        return this.passwordHash.equals(hashPassword(password));
    }
    
    // Getters
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFullName() { return fullName; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public LocalDateTime getRegistrationDate() { return registrationDate; }
    public boolean isActive() { return isActive; }
    
    // For saving to file
    public String toFileString() {
        return username + "|" + passwordHash + "|" + email + "|" + fullName + "|" + 
               address + "|" + phone + "|" + registrationDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + "|" + isActive;
    }
    
    @Override
    public String toString() {
        return "User{username='" + username + "', email='" + email + "', fullName='" + fullName + "'}";
    }

}
