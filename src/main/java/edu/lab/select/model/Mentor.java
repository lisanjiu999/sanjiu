package edu.lab.select.model;

import jakarta.persistence.*;

@Entity
@Table(name = "mentor")
public class Mentor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public Long labId;
    public String no;
    public String name;
    public String title;
    public String tags;
    @Column(length = 4000)
    public String intro;
    public String introStatus = "DRAFT";
    public int quota = 3;
    public String status = "ACTIVE";
}
