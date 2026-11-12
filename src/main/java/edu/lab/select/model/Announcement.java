package edu.lab.select.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "announcement")
public class Announcement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String batchNo;
    public String title;
    public LocalDateTime publishedAt;
    /** DRAFT / PUBLISHED / CLOSED */
    public String status = "DRAFT";
    public int objections;
}
