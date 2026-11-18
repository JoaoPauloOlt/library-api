package com.jpoltramari.library_api.api.dto.fine;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record WaiveFineInput(@NotBlank @Size(max=500) String reason) {}