package com.example.usersecuritydemo.auth;

import com.example.usersecuritydemo.client.KafkaProducerClient;
import com.example.usersecuritydemo.config.JwtService;
import com.example.usersecuritydemo.dto.SendEmailMessage;
import com.example.usersecuritydemo.user.Role;
import com.example.usersecuritydemo.user.User;
import com.example.usersecuritydemo.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.common.network.Send;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService{

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    //for kafka messages
    private final KafkaProducerClient kafkaProducerClient;
    private final ObjectMapper objectMapper;
    @Override
    public AuthenticationResponse register(RegisterRequest request) {
        var user = User.builder()
                .firstName(request.getFirstName())
                .lastName((request.getLastName()))
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .build();
        userRepository.save(user);
        var jwt = jwtService.generateToken(user);
        //publish the message in kafka topic
        try {
            SendEmailMessage sendEmailMessage = SendEmailMessage.builder().to(request.getEmail())
                    .from("harry333b4u@gmail.com")
                    .subject("Welcome to scaler")
                    .body("You have registered. Please click on link to verify").build();
            kafkaProducerClient.send("send_email", objectMapper.writeValueAsString(sendEmailMessage));
        }catch (JsonProcessingException ex){
            throw new RuntimeException(ex);
        }
        return AuthenticationResponse.builder().token(jwt)
                .build();
    }

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                authenticationRequest.getUsername(),
                authenticationRequest.getPassword()));
        var user = userRepository.findByEmail(authenticationRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid username or password"));
        var jwt = jwtService.generateToken(user);
        return AuthenticationResponse.builder().
                token(jwt).
                build();
    }
}
