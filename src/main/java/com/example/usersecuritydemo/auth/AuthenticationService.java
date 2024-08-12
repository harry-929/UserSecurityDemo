package com.example.usersecuritydemo.auth;

public interface AuthenticationService {
    public AuthenticationResponse register(RegisterRequest request);
    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest);
}
