package com.biblosmast.service;

import com.biblosmast.model.dto.LoginRequest;
import com.biblosmast.model.dto.LoginResponse;

public interface IAuthService {
    LoginResponse login(LoginRequest request);
}
