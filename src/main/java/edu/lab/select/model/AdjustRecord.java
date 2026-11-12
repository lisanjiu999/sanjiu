package edu.lab.select.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "adjust_record")
public class AdjustRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long studentId;
    /** 1 内部调剂 / 2 跨室调剂 / 3 最终安置 */
    public int level;
    public Long mentorId;
    public Long labId;
    public String reason;
    public String operator;
    public LocalDateTime createdAt = LocalDateTime.now();
}
