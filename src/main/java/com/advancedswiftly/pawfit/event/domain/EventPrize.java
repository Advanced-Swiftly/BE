package com.advancedswiftly.pawfit.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "event_prize")
public class EventPrize {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "prize_id")
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id")
    private StampEvent event;
    @Column(name = "prize_rank", nullable = false)
    private int rank;
    @Column(name = "name", nullable = false, length = 150)
    private String name;
    @Column(name = "quantity", nullable = false)
    private int quantity;
    protected EventPrize() { }
    public Long getId() { return id; }
    public int getRank() { return rank; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
}
