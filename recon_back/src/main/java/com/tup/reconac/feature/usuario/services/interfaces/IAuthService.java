package com.tup.reconac.feature.usuario.services.interfaces;

import com.tup.reconac.feature.usuario.dtos.request.LoginRequestDto;
import com.tup.reconac.feature.usuario.dtos.request.RefreshRequestDto;
import com.tup.reconac.feature.usuario.dtos.request.RegisterRequestDto;
import com.tup.reconac.feature.usuario.dtos.response.AuthResponseDto;
import com.tup.reconac.feature.usuario.dtos.response.RefreshResponseDto;

public interface IAuthService {
    void register(RegisterRequestDto request);
    AuthResponseDto login(LoginRequestDto request);
    RefreshResponseDto refresh(RefreshRequestDto request);
}
