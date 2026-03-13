package com.ing.assesment.infra.auth.api;

import com.ing.assesment.domain.auth.command.LoginCommand;
import com.ing.assesment.domain.auth.command.RefreshTokenCommand;
import com.ing.assesment.domain.auth.command.RegisterUserCommand;
import com.ing.assesment.domain.auth.command.handler.RegisterUserCommandHandler;
import com.ing.assesment.domain.auth.model.TokenPair;
import com.ing.assesment.domain.common.CommandHandler;
import com.ing.assesment.domain.common.VoidCommandHandler;
import com.ing.assesment.infra.auth.api.request.LoginRequest;
import com.ing.assesment.infra.auth.api.request.RefreshTokenRequest;
import com.ing.assesment.infra.auth.api.request.RegisterRequest;
import com.ing.assesment.infra.auth.api.response.LoginResponse;
import com.ing.assesment.infra.auth.api.response.TokenResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final VoidCommandHandler<RegisterUserCommand> registerHandler;
    private final CommandHandler<LoginCommand, TokenPair> loginHandler;
    private final CommandHandler<RefreshTokenCommand, TokenPair> refreshTokenHandler;


    @Operation(summary = "Register new user")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "400", description = "Invalid request")
    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        RegisterUserCommand command = new RegisterUserCommand(request.email(), request.password());
        registerHandler.handle(command);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @Operation(summary = "Login user with credentials")
    @ApiResponse(responseCode = "20", description = "login succeeded")
    @ApiResponse(responseCode = "401", description = "wrong email/password")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPair tokenPair = loginHandler.handle(new LoginCommand(request.email(), request.password()));

        return ResponseEntity.ok(
                new LoginResponse(
                        tokenPair.accessToken(),
                        tokenPair.refreshToken(),
                        tokenPair.tokenType()
                )
        );
    }

    @Operation(summary = "Refresh access token using refresh token")
    @ApiResponse(responseCode = "200", description = "New access token generated")
    @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        TokenPair tokenPair = refreshTokenHandler.handle(new RefreshTokenCommand(request.refreshToken()));

        return ResponseEntity.ok(
                new TokenResponse(
                        tokenPair.accessToken(),
                        tokenPair.refreshToken(),
                        tokenPair.tokenType()
                )
        );
    }
}
