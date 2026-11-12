package edu.lab.select.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 登录账号。与业务身份（学生 / 导师 / 管理员）通过 role + refId 关联。 */
@Entity
@Table(name = "account", uniqueConstraints = @UniqueConstraint(columnNames = "username"))
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    public String username;
    public String passwordHash;
    public String salt;
    /** student / mentor / admin */
    public String role;
    public Long refId;
    public String status = "ACTIVE";
    public int failCount;
    public LocalDateTime lockUntil;
    public LocalDateTime lastLoginAt;
    public boolean mustChange;
}
