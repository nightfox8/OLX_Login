package com.olx.controller;

import com.olx.dto.AuthenticationRequest;
import com.olx.dto.User;
import com.olx.security.JwtUtil;
import com.olx.service.LoginService;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/olx/user")
public class LoginController {

    @Autowired
    LoginService loginService;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    JwtUtil jwtUtil;

    @Autowired
    UserDetailsService userDetailsService;

    @ApiOperation(value = "Authenticate user in the application.")
    @PostMapping(value = "/authenticate",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<String> login(@RequestBody AuthenticationRequest authenticationRequest) {
        try {
            String token = jwtUtil.generateToken(userDetailsService.loadUserByUsername(authenticationRequest.getUsername()));
            loginService.login(authenticationRequest.getUsername());
            return new ResponseEntity<>(token, HttpStatus.OK);
        } catch (BadCredentialsException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @ApiOperation(value = "Logs out authenticated user from the application.")
    @DeleteMapping(value = "/logout")
    public ResponseEntity<Boolean> logout(@RequestHeader("Authorization") String authToken) {

        ResponseEntity<Boolean> validTokenResponse = validateToken(authToken);
        if (Boolean.TRUE.equals(validTokenResponse.getBody())) {
            ResponseEntity<String> usernameResponse = getUsername(authToken);
            return new ResponseEntity<>(loginService.logout(usernameResponse.getBody()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
    }

    @ApiOperation(value = "Register a user in the application.")
    @PostMapping(value = "",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Object> registerUser(@RequestBody User user) {
        return new ResponseEntity<>(loginService.registerUser(user), HttpStatus.CREATED);
    }

    @ApiOperation(value = "Get information of a user from the application.")
    @GetMapping(value = "",
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<Object> getUserInfo(@RequestHeader("Authorization") String authToken) {

        ResponseEntity<Boolean> validTokenResponse = validateToken(authToken);
        if (Boolean.TRUE.equals(validTokenResponse.getBody())) {
            ResponseEntity<String> usernameResponse = getUsername(authToken);
            return new ResponseEntity<>(loginService.getUserInfo(usernameResponse.getBody()), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
    }

    @GetMapping(value = "/validate/token")
    public ResponseEntity<Boolean> validateToken(@RequestHeader("Authorization") String authToken) {
        try {
            String token = authToken.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);
            if (username.isEmpty()) {
                return new ResponseEntity<>(false, HttpStatus.UNAUTHORIZED);
            }
            if (loginService.isUserInactive(username)) {
                return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity<>(jwtUtil.validateToken(token, userDetailsService.loadUserByUsername(username)), HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(false, HttpStatus.BAD_REQUEST);
        }
    }

    @GetMapping(value = "/name")
    public ResponseEntity<String> getUsername(@RequestHeader("Authorization") String authToken) {
        try {
            String token = authToken.replace("Bearer ", "");
            String username = jwtUtil.extractUsername(token);
            if (username.isEmpty()) {
                return new ResponseEntity<>("", HttpStatus.UNAUTHORIZED);
            }
            return new ResponseEntity<>(username, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>("", HttpStatus.BAD_REQUEST);
        }
    }
}
