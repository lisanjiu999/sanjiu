package edu.lab.select.service;

import org.springframework.stereotype.Service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 口令散列：PBKDF2-HMAC-SHA256，每账号独立随机盐。
 * 数据库只存 salt 与 hash，任何地方都不保存明文。
 */
@Service
public class PasswordService {

    private static final int ITERATIONS = 60000;
    private static final int KEY_BITS = 256;
    private final SecureRandom random = new SecureRandom();

    public String newSalt() {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    public String hash(String raw, String salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(raw.toCharArray(),
                    Base64.getDecoder().decode(salt), ITERATIONS, KEY_BITS);
            byte[] out = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
            return Base64.getEncoder().encodeToString(out);
        } catch (Exception e) {
            throw new IllegalStateException("口令散列失败", e);
        }
    }

    public boolean matches(String raw, String salt, String expectedHash) {
        if (raw == null || salt == null || expectedHash == null) return false;
        String actual = hash(raw, salt);
        // 定长比较，避免因提前返回泄露信息
        if (actual.length() != expectedHash.length()) return false;
        int diff = 0;
        for (int i = 0; i < actual.length(); i++) {
            diff |= actual.charAt(i) ^ expectedHash.charAt(i);
        }
        return diff == 0;
    }

    /** 口令强度：至少 8 位，且同时包含字母与数字。 */
    public void validateStrength(String raw) {
        if (raw == null || raw.length() < 8) throw new ApiException("密码至少 8 位");
        boolean hasLetter = raw.chars().anyMatch(Character::isLetter);
        boolean hasDigit = raw.chars().anyMatch(Character::isDigit);
        if (!hasLetter || !hasDigit) throw new ApiException("密码需同时包含字母和数字");
    }
}
