package com.training.starter.service;

import com.training.starter.dto.request.LoginRequest;
import com.training.starter.dto.request.RefreshTokenRequest;
import com.training.starter.dto.request.RegisterRequest;
import com.training.starter.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);
}
