package com.advancedswiftly.pawfit.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "user_term_agreement")
public class UserTermAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_term_agreement_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "term_id", nullable = false)
    private Term term;

    @Column(name = "agreed", nullable = false)
    private boolean agreed;

    @Column(name = "agreed_at", nullable = false)
    private Instant agreedAt;

    protected UserTermAgreement() {
    }

    private UserTermAgreement(User user, Term term, boolean agreed, Instant agreedAt) {
        this.user = user;
        this.term = term;
        this.agreed = agreed;
        this.agreedAt = agreedAt;
    }

    public static UserTermAgreement record(
            User user, Term term, boolean agreed, Instant agreedAt) {
        return new UserTermAgreement(user, term, agreed, agreedAt);
    }
}
