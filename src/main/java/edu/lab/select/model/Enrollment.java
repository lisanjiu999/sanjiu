package edu.lab.select.model;

import jakarta.persistence.*;

@Entity
@Table(name = "enrollment", uniqueConstraints = @UniqueConstraint(columnNames = "studentId"))
public class Enrollment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long studentId;
    public Long mentorId;
    public Long labId;
    /** DIRECT / WAITLIST / ADJUST */
    public String source;
    public int roundNo = 1;
    /** PROVISIONAL / FINAL */
    public String status = "PROVISIONAL";
}
