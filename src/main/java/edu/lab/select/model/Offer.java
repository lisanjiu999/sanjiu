package edu.lab.select.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "offer")
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long studentId;
    public Long mentorId;
    public Long labId;
    public int roundNo = 1;
    /** PENDING / CONFIRMED / EXPIRED / RELEASED */
    public String status = "PENDING";
    public LocalDateTime createdAt = LocalDateTime.now();
}
