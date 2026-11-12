package edu.lab.select.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 平行意向：刻意不存"第几志愿"，表结构层面消除志愿顺序。 */
@Entity
@Table(name = "intention",
        uniqueConstraints = @UniqueConstraint(columnNames = {"studentId", "mentorId", "roundNo"}))
public class Intention {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long studentId;
    public Long mentorId;
    public int roundNo = 1;
    public boolean acceptInner;
    public boolean acceptCross;
    public LocalDateTime submittedAt = LocalDateTime.now();
}
