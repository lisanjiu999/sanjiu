package edu.lab.select.service;

import edu.lab.select.model.Account;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccountService {

    private static final int MAX_FAIL = 5;
    private static final int LOCK_MINUTES = 5;

    @PersistenceContext
    private EntityManager em;

    private final PasswordService passwords;

    public AccountService(PasswordService passwords) {
        this.passwords = passwords;
    }

    public Account byUsername(String username) {
        List<Account> list = em.createQuery("select a from Account a where a.username=:u", Account.class)
                .setParameter("u", username == null ? "" : username.trim())
                .getResultList();
        return list.isEmpty() ? null : list.get(0);
    }

    @Transactional
    public Account create(String username, String rawPassword, String role, Long refId, boolean mustChange) {
        Account a = new Account();
        a.username = username;
        a.role = role;
        a.refId = refId;
        a.salt = passwords.newSalt();
        a.passwordHash = passwords.hash(rawPassword, a.salt);
        a.mustChange = mustChange;
        em.persist(a);
        return a;
    }

    /**
     * 校验账号口令；连续失败 5 次锁定 5 分钟。失败信息统一，不区分账号是否存在。
     * noRollbackFor：失败计数要在抛异常前落库，否则事务回滚会让计数永远归零。
     */
    @Transactional(noRollbackFor = ApiException.class)
    public Account authenticate(String username, String rawPassword) {
        Account a = byUsername(username);
        String generic = "账号或密码不正确";
        if (a == null) throw new ApiException(generic);
        if (!"ACTIVE".equals(a.status)) throw new ApiException("账号已停用，请联系教学办公室");
        if (a.lockUntil != null && a.lockUntil.isAfter(LocalDateTime.now())) {
            throw new ApiException("密码错误次数过多，账号已锁定，请 " + LOCK_MINUTES + " 分钟后重试");
        }
        if (!passwords.matches(rawPassword, a.salt, a.passwordHash)) {
            a.failCount = a.failCount + 1;
            if (a.failCount >= MAX_FAIL) {
                a.lockUntil = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
                a.failCount = 0;
                em.merge(a);
                throw new ApiException("密码错误次数过多，账号已锁定 " + LOCK_MINUTES + " 分钟");
            }
            em.merge(a);
            throw new ApiException(generic);
        }
        a.failCount = 0;
        a.lockUntil = null;
        a.lastLoginAt = LocalDateTime.now();
        em.merge(a);
        return a;
    }

    @Transactional
    public void changePassword(Long accountId, String oldRaw, String newRaw) {
        Account a = em.find(Account.class, accountId);
        if (a == null) throw new ApiException("账号不存在");
        if (!passwords.matches(oldRaw, a.salt, a.passwordHash)) throw new ApiException("当前密码不正确");
        if (oldRaw != null && oldRaw.equals(newRaw)) throw new ApiException("新密码不能与当前密码相同");
        passwords.validateStrength(newRaw);
        a.salt = passwords.newSalt();
        a.passwordHash = passwords.hash(newRaw, a.salt);
        a.mustChange = false;
        em.merge(a);
    }
}
