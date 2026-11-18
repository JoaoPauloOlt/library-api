package com.jpoltramari.library_api.api.dto.fine;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
@Schema(name="FineModel", description="Fine details. Payment is not supported in this API version.")
public record FineModel(Long id, Long loanId, Long userId, int lateDays, BigDecimal dailyRate,
 BigDecimal amount, String status, Instant createdAt, Instant waivedAt, Long waivedBy, String waiverReason) {}