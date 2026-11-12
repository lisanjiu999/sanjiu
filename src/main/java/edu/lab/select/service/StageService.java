package edu.lab.select.service;

import edu.lab.select.model.StageConfig;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 阶段时间窗。演示环境用可控时钟 demoNow 代替真实系统时间，
 * 真实部署时把 now() 换成 LocalDateTime.now() 即可。
 */
@Service
public class StageService {

    @PersistenceContext
    private EntityManager em;

    private LocalDateTime demoNow = LocalDateTime.of(2026, 11, 2, 9, 30);

    public List<StageConfig> stages() {
        return em.createQuery("select s from StageConfig s order by s.seq", StageConfig.class).getResultList();
    }

    public LocalDateTime now() {
        return demoNow;
    }

    public void moveTo(LocalDateTime t) {
        this.demoNow = t;
    }

    public StageConfig byId(Long id) {
        return em.find(StageConfig.class, id);
    }

    /** 管理员改某个阶段的时间窗。只校验先后，不强制与相邻阶段衔接。 */
    @Transactional
    public StageConfig updateWindow(Long id, LocalDateTime start, LocalDateTime end) {
        StageConfig s = em.find(StageConfig.class, id);
        if (s == null) throw new ApiException("阶段不存在");
        if (!start.isBefore(end)) throw new ApiException("开始时间必须早于结束时间");
        s.startAt = start;
        s.endAt = end;
        return s;
    }

    /** 当前所处阶段；早于全部阶段返回第一阶段，晚于全部返回最后一阶段。 */
    public StageConfig current() {
        List<StageConfig> all = stages();
        if (all.isEmpty()) return null;
        StageConfig cur = all.get(0);
        for (StageConfig s : all) {
            if (!demoNow.isBefore(s.startAt)) cur = s;
        }
        return cur;
    }

    public int seq(String code) {
        return stages().stream().filter(s -> s.code.equals(code)).findFirst().map(s -> s.seq).orElse(0);
    }

    public boolean at(String code) {
        StageConfig c = current();
        return c != null && c.code.equals(code);
    }

    public boolean after(String code) {
        StageConfig c = current();
        return c != null && c.seq > seq(code);
    }

    public StageConfig next() {
        StageConfig c = current();
        if (c == null) return null;
        for (StageConfig s : stages()) {
            if (s.seq == c.seq + 1) return s;
        }
        return null;
    }
}
