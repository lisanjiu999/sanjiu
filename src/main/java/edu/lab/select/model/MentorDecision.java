package edu.lab.select.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mentor_decision",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mentorId", "studentId", "roundNo"}))
public class MentorDecision {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long mentorId;
    public Long studentId;
    public int roundNo = 1;
    /** ADMIT / WAIT / ADJUST / REJECT */
    public String decision = "REJECT";
    public int waitRank = 0;
}
