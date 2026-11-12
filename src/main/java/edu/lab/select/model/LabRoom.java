package edu.lab.select.model;

import jakarta.persistence.*;

@Entity
@Table(name = "lab_room")
public class LabRoom {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String code;
    public String name;
    public String direction;
    public int capacity;
    public String status = "OPEN";
    public String note;
}
