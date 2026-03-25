package com.example.eventlottery.profile;

/**
 * User data model stored in Firestore.
 * Represents a single authenticated account and profile.
 */
public class User {

    // Firestore + app fields (matches the project spec):
    // userId, username, email, role, phoneNumber
    private String userId;
    private String username;
    private String email;
    private String role;
    private String phoneNumber;

    // Required empty constructor for Firestore deserialization.
    // Firestore needs a no-arg constructor so it can create the object before setting fields.
    public User() {
    }

    public User(String userId, String username, String email, String role, String phoneNumber) {
        // This constructor is used when we create a profile from user input and then write to Firestore.
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.phoneNumber = phoneNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        // Setter used by Firestore mapping or by the app when updating the model.
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}

