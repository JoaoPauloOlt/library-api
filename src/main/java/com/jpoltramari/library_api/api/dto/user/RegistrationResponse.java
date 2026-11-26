package com.jpoltramari.library_api.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RegistrationResponse", description = "Generic response that does not disclose whether an email is already registered.")
public record RegistrationResponse(
        @Schema(description = "Generic registration confirmation.")
        String message
) {
    public static RegistrationResponse genericConfirmation() {
        return new RegistrationResponse(
                "If the address is eligible for registration, the account will be created."
        );
    }
}
