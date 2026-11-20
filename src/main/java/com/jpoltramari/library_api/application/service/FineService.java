package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.port.security.AuthenticatedPrincipal;
import com.jpoltramari.library_api.domain.enums.FineStatus;
import com.jpoltramari.library_api.domain.exception.BusinessException;
import com.jpoltramari.library_api.domain.exception.FineNotFoundException;
import com.jpoltramari.library_api.domain.exception.UserNotFoundException;
import com.jpoltramari.library_api.domain.model.Fine;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.repository.FineRepository;
import com.jpoltramari.library_api.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class FineService {
 private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
 private final FineRepository fineRepository;
 private final FineCalculationService calculationService;
 private final OutboxEventService outboxEventService;
 private final UserRepository userRepository;
 private final Clock clock;

 @Transactional(readOnly=true)
 public Page<Fine> findMyFines(Long userId, FineStatus status, Pageable pageable) {
  return status == null ? fineRepository.findByUserId(userId,pageable)
   : fineRepository.findByUserIdAndStatus(userId,status,pageable);
 }

 @Transactional(readOnly=true)
 @PreAuthorize("hasAuthority('FINE_READ_ALL')")
 public Page<Fine> findAll(FineStatus status, Pageable pageable) {
  return status == null ? fineRepository.findAll(pageable) : fineRepository.findByStatus(status,pageable);
 }

 @Transactional(readOnly=true)
 public Fine findVisibleFine(Long id, AuthenticatedPrincipal principal) {
  Fine fine=fineRepository.findById(id).orElseThrow(()->new FineNotFoundException(id));
  if(!principal.hasPermission("FINE_READ_ALL") && !Objects.equals(fine.getUser().getId(),principal.getUserId()))
   throw new FineNotFoundException(id);
  return fine;
 }

 @Transactional
 @PreAuthorize("hasAuthority('FINE_WAIVE')")
 public Fine waive(Long id,String reason,Long adminUserId) {
  Fine fine=fineRepository.findByIdForUpdate(id).orElseThrow(()->new FineNotFoundException(id));
  if(fine.getStatus()!=FineStatus.OPEN) throw new BusinessException("Only open fines can be waived.");
  var admin=userRepository.findById(adminUserId).orElseThrow(()->new UserNotFoundException(adminUserId));
  Instant waivedAt=clock.instant();
  fine.setStatus(FineStatus.WAIVED);
  fine.setWaivedAt(waivedAt);
  fine.setWaivedBy(admin);
  fine.setWaiverReason(reason.trim());
  Fine saved=fineRepository.save(fine);
  outboxEventService.recordEvent("FINE",saved.getId().toString(),"fine.waived",Map.of(
   "fineId",saved.getId(),"loanId",saved.getLoan().getId(),"userId",saved.getUser().getId(),
   "waivedBy",adminUserId,"reason",saved.getWaiverReason(),"waivedAt",waivedAt));
  return saved;
 }

 /** LocalDateTime circulation values are UTC, then converted to the business timezone. */
 @Transactional
 public Fine createForReturnedLoan(Loan loan, LocalDateTime returnedAt) {
  if(loan.getDueDate()==null || returnedAt==null)
   throw new IllegalStateException("Loan due date and return date are required to calculate a fine.");
  LocalDate dueDate=loan.getDueDate().toInstant(ZoneOffset.UTC).atZone(BUSINESS_ZONE).toLocalDate();
  LocalDate returnDate=returnedAt.toInstant(ZoneOffset.UTC).atZone(BUSINESS_ZONE).toLocalDate();
  int lateDays=calculationService.calculateLateDays(dueDate,returnDate);
  if(lateDays==0) return null;
  if(fineRepository.existsByLoanId(loan.getId()))
   throw new IllegalStateException("A fine already exists for this loan.");
  Fine fine=new Fine();
  fine.setLoan(loan); fine.setUser(loan.getUser()); fine.setLateDays(lateDays);
  fine.setDailyRate(calculationService.getDailyRate());
  fine.setAmount(calculationService.calculateAmount(lateDays));
  fine.setStatus(FineStatus.OPEN); fine.setCreatedAt(clock.instant());
  Fine saved=fineRepository.save(fine);
  outboxEventService.recordEvent("FINE",saved.getId().toString(),"fine.created",Map.of(
   "fineId",saved.getId(),"loanId",loan.getId(),"userId",loan.getUser().getId(),
   "lateDays",lateDays,"dailyRate",saved.getDailyRate(),"amount",saved.getAmount(),"currency","BRL"));
  return saved;
 }
}