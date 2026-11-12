package edu.lab.select.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 演示用会话：token -> 身份，带过期时间。真实系统应替换为 JWT + 统一身份认证。 */
@Service
public class TokenStore {

    public record Session(String role, Long id, Long accountId, LocalDateTime expiresAt) {}

    private static final int HOURS = 8;

    private final Map<String, Session> tokens = new ConcurrentHashMap<>();

    public String issue(String role, Long id, Long accountId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokens.put(token, new Session(role, id, accountId, LocalDateTime.now().plusHours(HOURS)));
        return token;
    }

    public Session get(String token) {
        if (token == null) return null;
        Session s = tokens.get(token);
        if (s == null) return null;
        if (s.expiresAt().isBefore(LocalDateTime.now())) {
            tokens.remove(token);
            return null;
        }
        return s;
    }

    public void revoke(String token) {
        if (token != null) tokens.remove(token);
    }

    public void revokeAccount(Long accountId) {
        tokens.entrySet().removeIf(e -> accountId.equals(e.getValue().accountId()));
    }
}
