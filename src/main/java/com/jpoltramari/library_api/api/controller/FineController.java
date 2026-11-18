package com.jpoltramari.library_api.api.controller;

import com.jpoltramari.library_api.api.dto.PageResponse;
import com.jpoltramari.library_api.api.dto.fine.FineModel;
import com.jpoltramari.library_api.api.dto.fine.WaiveFineInput;
import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import com.jpoltramari.library_api.application.service.FineService;
import com.jpoltramari.library_api.domain.enums.FineStatus;
import com.jpoltramari.library_api.domain.model.Fine;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/fines")
@RequiredArgsConstructor
@Validated
public class FineController {
 private final FineService fineService;

 @GetMapping("/my")
 @PreAuthorize("hasAuthority('FINE_READ_OWN')")
 public PageResponse<FineModel> findMyFines(
  @RequestParam(required=false) FineStatus status,
  @ParameterObject @PageableDefault(size=20,sort="createdAt") Pageable pageable,
  Authentication authentication) {
  AuthenticatedPrincipal principal=principal(authentication);
  return PageResponse.from(fineService.findMyFines(principal.getUserId(),status,pageable),FineController::toModel);
 }

 @GetMapping
 @PreAuthorize("hasAuthority('FINE_READ_ALL')")
 public PageResponse<FineModel> findAll(
  @RequestParam(required=false) FineStatus status,
  @ParameterObject @PageableDefault(size=20,sort="createdAt") Pageable pageable) {
  return PageResponse.from(fineService.findAll(status,pageable),FineController::toModel);
 }

 @GetMapping("/{id}")
 public FineModel findById(@PathVariable @Positive Long id,Authentication authentication) {
  return toModel(fineService.findVisibleFine(id,principal(authentication)));
 }

 @PutMapping("/{id}/waive")
 @PreAuthorize("hasAuthority('FINE_WAIVE')")
 public FineModel waive(@PathVariable @Positive Long id,@RequestBody @Valid WaiveFineInput input,
  Authentication authentication) {
  return toModel(fineService.waive(id,input.reason(),principal(authentication).getUserId()));
 }

 private AuthenticatedPrincipal principal(Authentication authentication) {
  return (AuthenticatedPrincipal)authentication.getPrincipal();
 }

 private static FineModel toModel(Fine fine) {
  return new FineModel(fine.getId(),fine.getLoan().getId(),fine.getUser().getId(),
   fine.getLateDays(),fine.getDailyRate(),fine.getAmount(),fine.getStatus().name(),
   fine.getCreatedAt(),fine.getWaivedAt(),fine.getWaivedBy()==null?null:fine.getWaivedBy().getId(),
   fine.getWaiverReason());
 }
}