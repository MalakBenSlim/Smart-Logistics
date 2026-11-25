package com.smartlogistics.common.dto;

import java.io.Serializable;

public class AuthResultDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private String message;
    private UserDTO user;
    private String sessionToken;

    public AuthResultDTO() {}

    public AuthResultDTO(boolean success, String message, UserDTO user, String sessionToken) {
        this.success = success;
        this.message = message;
        this.user = user;
        this.sessionToken = sessionToken;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }
}
