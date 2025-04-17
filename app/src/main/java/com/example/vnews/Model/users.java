package com.example.vnews.Model;

public class users {
    private String id;
    private String username;
    private String email;
    private String password;
    private String avtUrl;

    public users() {
    }

    public users(String id, String username, String email, String password, String avtUrl) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.password = password;
        this.avtUrl = avtUrl;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getAvtUrl() {
        return avtUrl;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setAvtUrl(String avtUrl) {
        this.avtUrl = avtUrl;
    }
}