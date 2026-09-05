package com.indomaret.backend.service;

import com.indomaret.backend.dto.LoginRequest;
import com.indomaret.backend.dto.LoginResponse;

public interface AuthService {
    LoginResponse login(LoginRequest request);
}
