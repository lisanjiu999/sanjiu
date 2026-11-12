package edu.lab.select.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "stage_config")
public class StageConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String code;
    public String name;
    public int seq;
    public LocalDateTime startAt;
    public LocalDateTime endAt;
    public String summary;
}
