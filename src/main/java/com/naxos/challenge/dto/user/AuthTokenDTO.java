package com.naxos.challenge.dto.user;

import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Getter
@Setter
@Schema(description = "Wrapper with AccessToken and RefreshToken")
public class AuthTokenDTO {

    String  accessToken;
    String refreshToken;

    public static AuthTokenDTO of (String accessToken, String refreshToken) {
        AuthTokenDTO authTokenDTO = new AuthTokenDTO();
        authTokenDTO.setAccessToken(accessToken);
        authTokenDTO.setRefreshToken(refreshToken);
        return authTokenDTO;
    }
}
