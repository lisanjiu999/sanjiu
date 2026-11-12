package edu.lab.select.model;

import jakarta.persistence.*;

@Entity
@Table(name = "student")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String no;
    public String name;
    public String className;
    public double gpa;
    public String skill;
}
