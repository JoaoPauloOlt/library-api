package com.jpoltramari.library_api.domain.model;

import com.jpoltramari.library_api.domain.enums.FineStatus;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "fines", indexes = {
        @Index(name = "idx_fines_user_status", columnList = "user_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Fine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false, unique = true,
            foreignKey = @ForeignKey(name = "fk_fines_loan"))
    private Loan loan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_fines_user"))
    private User user;

    @Column(name = "late_days", nullable = false)
    private int lateDays;

    @Column(name = "daily_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FineStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "waived_at")
    private Instant waivedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "waived_by",
            foreignKey = @ForeignKey(name = "fk_fines_waived_by"))
    private User waivedBy;

    @Column(name = "waiver_reason", length = 500)
    private String waiverReason;
}
