package edu.lab.select.service;

import edu.lab.select.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SelectionService {

    @PersistenceContext
    private EntityManager em;

    private final StageService stage;

    public SelectionService(StageService stage) {
        this.stage = stage;
    }

    // ---------------------------------------------------------------- 基础查询

    public List<LabRoom> labs() {
        return em.createQuery("select l from LabRoom l order by l.code", LabRoom.class).getResultList();
    }

    public LabRoom lab(Long id) {
        return em.find(LabRoom.class, id);
    }

    public Mentor mentor(Long id) {
        return em.find(Mentor.class, id);
    }

    public Student student(Long id) {
        return em.find(Student.class, id);
    }

    public List<Student> students() {
        return em.createQuery("select s from Student s order by s.no", Student.class).getResultList();
    }

    public List<Mentor> allMentors() {
        return em.createQuery("select m from Mentor m order by m.labId, m.id", Mentor.class).getResultList();
    }

    public List<Mentor> mentorsOfLab(Long labId) {
        return em.createQuery("select m from Mentor m where m.labId=:l and m.status='ACTIVE' order by m.id", Mentor.class)
                .setParameter("l", labId).getResultList();
    }

    private long count(String jpql, String p, Object v) {
        return em.createQuery(jpql, Long.class).setParameter(p, v).getSingleResult();
    }

    /** 某导师已确认人数（名额占用的唯一口径）。 */
    public long enrolledCount(Long mentorId) {
        return count("select count(e) from Enrollment e where e.mentorId=:m", "m", mentorId);
    }

    public int remaining(Mentor m) {
        return (int) Math.max(0, m.quota - enrolledCount(m.id));
    }

    // ---------------------------------------------------------------- 学生端

    public Map<String, Object> labCards() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (LabRoom l : labs()) {
            List<Mentor> ms = mentorsOfLab(l.id);
            int quota = ms.stream().mapToInt(m -> m.quota).sum();
            int enrolled = ms.stream().mapToInt(m -> (int) enrolledCount(m.id)).sum();
            long applicants = em.createQuery(
                            "select count(distinct i.studentId) from Intention i, Mentor m " +
                                    "where i.mentorId=m.id and m.labId=:l and i.roundNo=1", Long.class)
                    .setParameter("l", l.id).getSingleResult();
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("id", l.id);
            c.put("code", l.code);
            c.put("name", l.name);
            c.put("direction", l.direction);
            c.put("capacity", l.capacity);
            c.put("mentorCount", ms.size());
            c.put("quota", quota);
            c.put("enrolled", enrolled);
            c.put("remaining", Math.max(0, quota - enrolled));
            c.put("applicants", applicants);
            c.put("note", l.note);
            out.add(c);
        }
        return Map.of("labs", out);
    }

    /** 全院导师卡片，供意向填报的选择器与基础数据页使用。 */
    public List<Map<String, Object>> allMentorCards() {
        List<Map<String, Object>> out = new ArrayList<>();
        Map<Long, LabRoom> labMap = new HashMap<>();
        for (LabRoom l : labs()) labMap.put(l.id, l);
        for (Mentor m : allMentors()) {
            if (!"ACTIVE".equals(m.status)) continue;
            LabRoom l = labMap.get(m.labId);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", m.id);
            r.put("name", m.name);
            r.put("title", m.title);
            r.put("tags", m.tags);
            r.put("quota", m.quota);
            r.put("remaining", remaining(m));
            r.put("introStatus", m.introStatus);
            r.put("labId", m.labId);
            r.put("labCode", l == null ? "" : l.code);
            r.put("labName", l == null ? "" : l.name);
            out.add(r);
        }
        return out;
    }

    public Map<String, Object> labDetail(Long labId) {
        LabRoom l = lab(labId);
        if (l == null) throw new ApiException("实训室不存在");
        List<Map<String, Object>> ms = new ArrayList<>();
        for (Mentor m : mentorsOfLab(labId)) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", m.id);
            row.put("name", m.name);
            row.put("title", m.title);
            row.put("tags", m.tags);
            row.put("quota", m.quota);
            row.put("remaining", remaining(m));
            row.put("introStatus", m.introStatus);
            row.put("excerpt", m.intro == null ? "" : m.intro.substring(0, Math.min(46, m.intro.length())));
            row.put("applicants", count("select count(i) from Intention i where i.mentorId=:m and i.roundNo=1", "m", m.id));
            ms.add(row);
        }
        return Map.of("lab", l, "mentors", ms);
    }

    public Map<String, Object> mentorDetail(Long mentorId) {
        Mentor m = mentor(mentorId);
        if (m == null) throw new ApiException("导师不存在");
        LabRoom l = lab(m.labId);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", m.id);
        row.put("name", m.name);
        row.put("title", m.title);
        row.put("tags", m.tags);
        row.put("intro", m.intro);
        row.put("introStatus", m.introStatus);
        row.put("quota", m.quota);
        row.put("remaining", remaining(m));
        row.put("labId", m.labId);
        row.put("labName", l == null ? "" : l.name);
        row.put("labCode", l == null ? "" : l.code);
        row.put("applicants", count("select count(i) from Intention i where i.mentorId=:m and i.roundNo=1", "m", m.id));
        return row;
    }

    public Map<String, Object> myIntentions(Long studentId) {
        List<Intention> list = em.createQuery(
                        "select i from Intention i where i.studentId=:s and i.roundNo=1", Intention.class)
                .setParameter("s", studentId).getResultList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Intention i : list) {
            Mentor m = mentor(i.mentorId);
            LabRoom l = m == null ? null : lab(m.labId);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("mentorId", i.mentorId);
            r.put("mentorName", m == null ? "" : m.name);
            r.put("labName", l == null ? "" : l.name);
            r.put("labCode", l == null ? "" : l.code);
            r.put("acceptInner", i.acceptInner);
            r.put("acceptCross", i.acceptCross);
            r.put("submittedAt", i.submittedAt);
            rows.add(r);
        }
        boolean inner = list.isEmpty() || list.get(0).acceptInner;
        boolean cross = list.isEmpty() || list.get(0).acceptCross;
        return Map.of("intentions", rows, "acceptInner", inner, "acceptCross", cross,
                "locked", !stage.at("INTENT"));
    }

    @Transactional
    public Map<String, Object> submitIntentions(Long studentId, List<Long> mentorIds,
                                                boolean acceptInner, boolean acceptCross) {
        if (!stage.at("INTENT")) throw new ApiException("当前不在意向填报时间窗内");
        if (mentorIds == null || mentorIds.isEmpty()) throw new ApiException("至少选择 1 位导师");
        if (mentorIds.size() > 3) throw new ApiException("最多填报 3 个平行意向");
        if (new HashSet<>(mentorIds).size() != mentorIds.size()) throw new ApiException("不能重复选择同一位导师");
        for (Long mid : mentorIds) {
            Mentor m = mentor(mid);
            if (m == null || !"ACTIVE".equals(m.status)) throw new ApiException("所选导师不可选");
        }
        em.createQuery("delete from Intention i where i.studentId=:s and i.roundNo=1")
                .setParameter("s", studentId).executeUpdate();
        for (Long mid : mentorIds) {
            Intention i = new Intention();
            i.studentId = studentId;
            i.mentorId = mid;
            i.acceptInner = acceptInner;
            i.acceptCross = acceptCross;
            i.submittedAt = LocalDateTime.now();
            em.persist(i);
        }
        return Map.of("ok", true, "count", mentorIds.size());
    }

    public Map<String, Object> myOffers(Long studentId) {
        List<Offer> offers = em.createQuery(
                        "select o from Offer o where o.studentId=:s order by o.id", Offer.class)
                .setParameter("s", studentId).getResultList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Offer o : offers) {
            Mentor m = mentor(o.mentorId);
            LabRoom l = m == null ? null : lab(m.labId);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", o.id);
            r.put("mentorId", o.mentorId);
            r.put("mentorName", m == null ? "" : m.name);
            r.put("labName", l == null ? "" : l.name);
            r.put("status", o.status);
            r.put("roundNo", o.roundNo);
            InterviewScore sc = em.createQuery(
                            "select s from InterviewScore s where s.studentId=:s and s.mentorId=:m", InterviewScore.class)
                    .setParameter("s", studentId).setParameter("m", o.mentorId)
                    .getResultList().stream().findFirst().orElse(null);
            r.put("score", sc == null ? null : sc.total);
            r.put("comment", sc == null ? null : sc.comment);
            rows.add(r);
        }
        Enrollment en = myEnrollment(studentId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("offers", rows);
        out.put("enrollment", en == null ? null : enrollmentRow(en));
        out.put("deadline", stage.stages().stream().filter(s -> s.code.equals("CONFIRM")).findFirst()
                .map(s -> s.endAt).orElse(null));
        return out;
    }

    private Enrollment myEnrollment(Long studentId) {
        return em.createQuery("select e from Enrollment e where e.studentId=:s", Enrollment.class)
                .setParameter("s", studentId).getResultList().stream().findFirst().orElse(null);
    }

    private Map<String, Object> enrollmentRow(Enrollment e) {
        Mentor m = mentor(e.mentorId);
        LabRoom l = m == null ? null : lab(m.labId);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mentorId", e.mentorId);
        r.put("mentorName", m == null ? "" : m.name);
        r.put("labName", l == null ? "" : l.name);
        r.put("labCode", l == null ? "" : l.code);
        r.put("source", e.source);
        r.put("status", e.status);
        return r;
    }

    @Transactional
    public Map<String, Object> confirmOffer(Long studentId, Long offerId) {
        if (!stage.at("CONFIRM")) throw new ApiException("当前不在确认时间窗内");
        Offer target = em.find(Offer.class, offerId);
        if (target == null || !Objects.equals(target.studentId, studentId)) throw new ApiException("意向不存在");
        if (!"PENDING".equals(target.status)) throw new ApiException("该意向已处理");
        if (myEnrollment(studentId) != null) throw new ApiException("你已确认过去向");
        for (Offer o : em.createQuery("select o from Offer o where o.studentId=:s", Offer.class)
                .setParameter("s", studentId).getResultList()) {
            o.status = o.id.equals(offerId) ? "CONFIRMED" : "RELEASED";
        }
        enroll(studentId, target.mentorId, "DIRECT", target.roundNo);
        return Map.of("ok", true);
    }

    // ---------------------------------------------------------------- 导师端

    public Map<String, Object> mentorOverview(Long mentorId) {
        Mentor m = mentor(mentorId);
        if (m == null) throw new ApiException("导师不存在");
        long applicants = count("select count(i) from Intention i where i.mentorId=:m and i.roundNo=1", "m", mentorId);
        long scored = count("select count(s) from InterviewScore s where s.mentorId=:m", "m", mentorId);
        long admits = count("select count(d) from MentorDecision d where d.mentorId=:m and d.decision='ADMIT'", "m", mentorId);
        long waits = count("select count(d) from MentorDecision d where d.mentorId=:m and d.decision='WAIT'", "m", mentorId);
        LabRoom l = lab(m.labId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("mentor", m);
        out.put("lab", l);
        out.put("applicants", applicants);
        out.put("scored", scored);
        out.put("admits", admits);
        out.put("waits", waits);
        out.put("enrolled", enrolledCount(mentorId));
        out.put("remaining", remaining(m));
        return out;
    }

    /** 报名学生：不含志愿顺序，也不含其填报的其他导师。 */
    public Map<String, Object> mentorApplicants(Long mentorId) {
        List<Intention> list = em.createQuery(
                        "select i from Intention i where i.mentorId=:m and i.roundNo=1 order by i.submittedAt", Intention.class)
                .setParameter("m", mentorId).getResultList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Intention i : list) {
            Student s = student(i.studentId);
            if (s == null) continue;
            InterviewScore sc = scoreOf(mentorId, s.id);
            MentorDecision d = decisionOf(mentorId, s.id);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("studentId", s.id);
            r.put("no", s.no);
            r.put("name", s.name);
            r.put("className", s.className);
            r.put("gpa", s.gpa);
            r.put("skill", s.skill);
            r.put("acceptInner", i.acceptInner);
            r.put("acceptCross", i.acceptCross);
            r.put("submittedAt", i.submittedAt);
            r.put("score", sc == null ? null : sc.total);
            r.put("dimensions", sc == null ? null
                    : Map.of("attitude", sc.attitude, "logic", sc.logic, "coding", sc.coding, "time", sc.timeInput));
            r.put("comment", sc == null ? null : sc.comment);
            r.put("decision", d == null ? null : d.decision);
            r.put("waitRank", d == null ? null : d.waitRank);
            rows.add(r);
        }
        return Map.of("applicants", rows, "quota", mentor(mentorId).quota,
                "enrolled", enrolledCount(mentorId));
    }

    private InterviewScore scoreOf(Long mentorId, Long studentId) {
        return em.createQuery("select s from InterviewScore s where s.mentorId=:m and s.studentId=:s", InterviewScore.class)
                .setParameter("m", mentorId).setParameter("s", studentId)
                .getResultList().stream().findFirst().orElse(null);
    }

    private MentorDecision decisionOf(Long mentorId, Long studentId) {
        return em.createQuery("select d from MentorDecision d where d.mentorId=:m and d.studentId=:s", MentorDecision.class)
                .setParameter("m", mentorId).setParameter("s", studentId)
                .getResultList().stream().findFirst().orElse(null);
    }

    @Transactional
    public Map<String, Object> saveScore(Long mentorId, Long studentId, int attitude, int logic,
                                         int coding, int timeInput, String comment) {
        if (!stage.at("INTERVIEW")) throw new ApiException("当前不在面试考核时间窗内");
        if (!hasIntention(mentorId, studentId)) throw new ApiException("该学生未填报本导师");
        InterviewScore sc = scoreOf(mentorId, studentId);
        if (sc == null) {
            sc = new InterviewScore();
            sc.mentorId = mentorId;
            sc.studentId = studentId;
        }
        sc.attitude = clamp(attitude);
        sc.logic = clamp(logic);
        sc.coding = clamp(coding);
        sc.timeInput = clamp(timeInput);
        sc.total = (sc.attitude + sc.logic + sc.coding + sc.timeInput) * 5;
        sc.comment = comment;
        if (sc.id == null) em.persist(sc); else em.merge(sc);
        return Map.of("ok", true, "total", sc.total);
    }

    private int clamp(int v) {
        return Math.max(1, Math.min(5, v));
    }

    private boolean hasIntention(Long mentorId, Long studentId) {
        return count("select count(i) from Intention i where i.mentorId=:m and i.studentId=:s", "m", mentorId) > 0;
    }

    @Transactional
    public Map<String, Object> saveDecision(Long mentorId, Long studentId, String decision, Integer waitRank) {
        if (!stage.at("INTERVIEW")) throw new ApiException("当前不在面试考核时间窗内");
        if (!hasIntention(mentorId, studentId)) throw new ApiException("该学生未填报本导师");
        Mentor m = mentor(mentorId);
        if ("ADMIT".equals(decision)) {
            long admits = count("select count(d) from MentorDecision d where d.mentorId=:m and d.decision='ADMIT'", "m", mentorId);
            MentorDecision cur = decisionOf(mentorId, studentId);
            boolean already = cur != null && "ADMIT".equals(cur.decision);
            if (!already && admits >= m.quota) throw new ApiException("拟录取人数已达名额上限，请改标为递补或内部调剂候选");
        }
        MentorDecision d = decisionOf(mentorId, studentId);
        if (d == null) {
            d = new MentorDecision();
            d.mentorId = mentorId;
            d.studentId = studentId;
        }
        d.decision = decision;
        d.waitRank = waitRank == null ? 0 : waitRank;
        if (d.id == null) em.persist(d); else em.merge(d);
        return Map.of("ok", true);
    }

    @Transactional
    public Map<String, Object> updateProfile(Long mentorId, String title, String tags, String intro) {
        Mentor m = mentor(mentorId);
        if (m == null) throw new ApiException("导师不存在");
        m.title = title;
        m.tags = tags;
        m.intro = intro;
        m.introStatus = "PENDING";
        em.merge(m);
        return Map.of("ok", true, "introStatus", m.introStatus);
    }

    // ---------------------------------------------------------------- 管理员端

    public Map<String, Object> dashboard() {
        List<Map<String, Object>> rows = new ArrayList<>();
        int quotaAll = 0, enrolledAll = 0, applicantsAll = 0;
        for (LabRoom l : labs()) {
            List<Mentor> ms = mentorsOfLab(l.id);
            int quota = ms.stream().mapToInt(m -> m.quota).sum();
            int enrolled = ms.stream().mapToInt(m -> (int) enrolledCount(m.id)).sum();
            long applicants = em.createQuery(
                            "select count(distinct i.studentId) from Intention i, Mentor m " +
                                    "where i.mentorId=m.id and m.labId=:l and i.roundNo=1", Long.class)
                    .setParameter("l", l.id).getSingleResult();
            long admits = em.createQuery(
                            "select count(d) from MentorDecision d, Mentor m " +
                                    "where d.mentorId=m.id and m.labId=:l and d.decision='ADMIT'", Long.class)
                    .setParameter("l", l.id).getSingleResult();
            long waits = em.createQuery(
                            "select count(d) from MentorDecision d, Mentor m " +
                                    "where d.mentorId=m.id and m.labId=:l and d.decision='WAIT'", Long.class)
                    .setParameter("l", l.id).getSingleResult();
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", l.id);
            r.put("code", l.code);
            r.put("name", l.name);
            r.put("direction", l.direction);
            r.put("quota", quota);
            r.put("applicants", applicants);
            r.put("admits", admits);
            r.put("waits", waits);
            r.put("enrolled", enrolled);
            r.put("remaining", Math.max(0, quota - enrolled));
            rows.add(r);
            quotaAll += quota;
            enrolledAll += enrolled;
            applicantsAll += applicants;
        }
        long studentCount = em.createQuery("select count(s) from Student s", Long.class).getSingleResult();
        long submitted = em.createQuery("select count(distinct i.studentId) from Intention i", Long.class).getSingleResult();
        long offerCount = em.createQuery("select count(o) from Offer o", Long.class).getSingleResult();
        long pending = em.createQuery("select count(o) from Offer o where o.status='PENDING'", Long.class).getSingleResult();
        long enrolledCnt = em.createQuery("select count(e) from Enrollment e", Long.class).getSingleResult();
        long adjust = em.createQuery("select count(a) from AdjustRecord a", Long.class).getSingleResult();

        Map<String, Object> totals = new LinkedHashMap<>();
        totals.put("students", studentCount);
        totals.put("submitted", submitted);
        totals.put("quota", quotaAll);
        totals.put("applicants", applicantsAll);
        totals.put("offers", offerCount);
        totals.put("pending", pending);
        totals.put("enrolled", enrolledCnt);
        totals.put("unplaced", Math.max(0, studentCount - enrolledCnt));
        totals.put("adjusted", adjust);
        return Map.of("labs", rows, "totals", totals);
    }

    @Transactional
    // ---------------------------------------------------------------- 演示时钟

    /** 把演示时钟拨到任意时刻，八个阶段的状态随之变化。 */
    public Map<String, Object> setClock(LocalDateTime at) {
        if (at == null) throw new ApiException("请提供时间");
        stage.moveTo(at);
        StageConfig cur = stage.current();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("demoNow", stage.now());
        out.put("stage", cur == null ? null : cur.name);
        return out;
    }

    public Map<String, Object> updateStageWindow(Long id, LocalDateTime start, LocalDateTime end) {
        StageConfig s = stage.updateWindow(id, start, end);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("id", s.id);
        out.put("startAt", s.startAt);
        out.put("endAt", s.endAt);
        return out;
    }

    public Map<String, Object> advanceStage() {        StageConfig cur = stage.current();
        StageConfig next = stage.next();
        if (next == null) throw new ApiException("已到最后一个阶段");
        List<String> log = new ArrayList<>();
        // 进入下一阶段前，结算上一阶段的结果
        if ("INTERVIEW".equals(cur.code)) {
            int n = generateOffers();
            log.add("生成拟录取意向 " + n + " 条");
        }
        if ("CONFIRM".equals(cur.code)) {
            int expired = expireUnconfirmed();
            int filled = fillWaitlist();
            log.add("逾期未确认释放 " + expired + " 条，递补补位 " + filled + " 人");
        }
        stage.moveTo(next.startAt.plusMinutes(30));
        if ("CONFIRM".equals(next.code)) {
            int auto = autoConfirmSingles();
            log.add("单意向自动确认 " + auto + " 人");
        }
        if ("PUBLISH".equals(next.code)) {
            int fin = finalizeAll();
            log.add("录取结果转正式 " + fin + " 条");
        }
        return Map.of("ok", true, "stage", stage.current().name, "log", log);
    }

    @Transactional
    public int generateOffers() {
        List<MentorDecision> admits = em.createQuery(
                "select d from MentorDecision d where d.decision='ADMIT' and d.roundNo=1", MentorDecision.class).getResultList();
        int n = 0;
        for (MentorDecision d : admits) {
            Long exists = em.createQuery(
                            "select count(o) from Offer o where o.studentId=:s and o.mentorId=:m", Long.class)
                    .setParameter("s", d.studentId).setParameter("m", d.mentorId).getSingleResult();
            if (exists > 0) continue;
            Mentor m = mentor(d.mentorId);
            Offer o = new Offer();
            o.studentId = d.studentId;
            o.mentorId = d.mentorId;
            o.labId = m == null ? null : m.labId;
            o.status = "PENDING";
            em.persist(o);
            n++;
        }
        return n;
    }

    @Transactional
    public int autoConfirmSingles() {
        List<Long> studentIds = em.createQuery(
                "select distinct o.studentId from Offer o where o.status='PENDING'", Long.class).getResultList();
        int n = 0;
        for (Long sid : studentIds) {
            List<Offer> pending = em.createQuery(
                            "select o from Offer o where o.studentId=:s and o.status='PENDING'", Offer.class)
                    .setParameter("s", sid).getResultList();
            if (pending.size() == 1) {
                Offer o = pending.get(0);
                o.status = "CONFIRMED";
                enroll(sid, o.mentorId, "DIRECT", o.roundNo);
                n++;
            }
        }
        return n;
    }

    @Transactional
    public int expireUnconfirmed() {
        List<Offer> pending = em.createQuery("select o from Offer o where o.status='PENDING'", Offer.class).getResultList();
        int n = 0;
        for (Offer o : pending) {
            o.status = "EXPIRED";
            n++;
        }
        return n;
    }

    @Transactional
    public int fillWaitlist() {
        int filled = 0;
        for (Mentor m : allMentors()) {
            int free = remaining(m);
            if (free <= 0) continue;
            List<MentorDecision> waits = em.createQuery(
                            "select d from MentorDecision d where d.mentorId=:m and d.decision='WAIT' order by d.waitRank, d.id",
                            MentorDecision.class)
                    .setParameter("m", m.id).getResultList();
            for (MentorDecision d : waits) {
                if (free <= 0) break;
                if (myEnrollment(d.studentId) != null) continue;
                enroll(d.studentId, m.id, "WAITLIST", 1);
                free--;
                filled++;
            }
        }
        return filled;
    }

    private void enroll(Long studentId, Long mentorId, String source, int roundNo) {
        if (myEnrollment(studentId) != null) return;
        Mentor m = mentor(mentorId);
        Enrollment e = new Enrollment();
        e.studentId = studentId;
        e.mentorId = mentorId;
        e.labId = m == null ? null : m.labId;
        e.source = source;
        e.roundNo = roundNo;
        e.status = "PROVISIONAL";
        em.persist(e);
    }

    @Transactional
    public int finalizeAll() {
        List<Enrollment> all = em.createQuery("select e from Enrollment e", Enrollment.class).getResultList();
        for (Enrollment e : all) e.status = "FINAL";
        return all.size();
    }

    /** 调剂池试算：不落库，先给管理员看结果。 */
    public Map<String, Object> adjustPreview() {
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<Long, Integer> free = new HashMap<>();
        for (Mentor m : allMentors()) free.put(m.id, remaining(m));
        List<Student> pool = new ArrayList<>();
        for (Student s : students()) if (myEnrollment(s.id) == null) pool.add(s);
        pool.sort((a, b) -> Double.compare(b.gpa, a.gpa));

        int l1 = 0, l2 = 0, l3 = 0, fail = 0;
        for (Student s : pool) {
            List<Intention> ins = em.createQuery(
                            "select i from Intention i where i.studentId=:s", Intention.class)
                    .setParameter("s", s.id).getResultList();
            Set<Long> myLabs = ins.stream().map(i -> mentor(i.mentorId)).filter(Objects::nonNull)
                    .map(m -> m.labId).collect(Collectors.toSet());
            boolean inner = !ins.isEmpty() && ins.get(0).acceptInner;
            boolean cross = !ins.isEmpty() && ins.get(0).acceptCross;

            Mentor target = null;
            int level = 0;
            String reason = "";
            if (inner) {
                target = pickMentor(free, myLabs, true);
                if (target != null) { level = 1; reason = "服从室内调剂，本室仍有余额"; }
            }
            if (target == null && cross) {
                target = pickMentor(free, myLabs, false);
                if (target != null) { level = 2; reason = "服从跨室调剂，全院仍有余额"; }
            }
            if (target == null) {
                target = pickMentor(free, myLabs, false);
                level = 3;
                reason = target == null ? "全院名额已满，需人工统筹" : "未服从调剂，最终统筹安置";
            }
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("studentId", s.id);
            r.put("no", s.no);
            r.put("name", s.name);
            r.put("className", s.className);
            r.put("gpa", s.gpa);
            r.put("level", level);
            r.put("reason", reason);
            r.put("mentorId", target == null ? null : target.id);
            r.put("mentorName", target == null ? null : target.name);
            r.put("labCode", target == null ? null : (lab(target.labId) == null ? null : lab(target.labId).code));
            rows.add(r);
            if (level == 1) l1++; else if (level == 2) l2++; else if (target != null) l3++; else fail++;
            if (target != null) free.put(target.id, free.get(target.id) - 1);
        }
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("pool", pool.size());
        stat.put("level1", l1);
        stat.put("level2", l2);
        stat.put("level3", l3);
        stat.put("failed", fail);
        return Map.of("rows", rows, "stat", stat);
    }

    private Mentor pickMentor(Map<Long, Integer> free, Set<Long> preferLabs, boolean onlyPreferred) {
        List<Mentor> candidates = allMentors().stream()
                .filter(m -> free.getOrDefault(m.id, 0) > 0)
                .filter(m -> !onlyPreferred || preferLabs.contains(m.labId))
                .sorted(Comparator.comparingInt(m -> -free.getOrDefault(m.id, 0)))
                .collect(Collectors.toList());
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    @Transactional
    public Map<String, Object> adjustCommit() {
        Map<String, Object> preview = adjustPreview();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) preview.get("rows");
        int n = 0;
        for (Map<String, Object> r : rows) {
            Long mentorId = (Long) r.get("mentorId");
            if (mentorId == null) continue;
            Long studentId = (Long) r.get("studentId");
            if (myEnrollment(studentId) != null) continue;
            enroll(studentId, mentorId, "ADJUST", 1);
            Mentor m = mentor(mentorId);
            AdjustRecord a = new AdjustRecord();
            a.studentId = studentId;
            a.level = (Integer) r.get("level");
            a.mentorId = mentorId;
            a.labId = m == null ? null : m.labId;
            a.reason = (String) r.get("reason");
            a.operator = "学院管理员";
            em.persist(a);
            n++;
        }
        return Map.of("ok", true, "count", n);
    }

    // ---------------------------------------------------------------- 公示

    @Transactional
    public Map<String, Object> publish(String title) {
        Announcement a = new Announcement();
        a.batchNo = "B" + LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        a.title = title == null || title.isBlank() ? "实训室进组选拔录取名单公示" : title;
        a.publishedAt = LocalDateTime.now();
        a.status = "PUBLISHED";
        em.persist(a);
        return Map.of("ok", true, "batchNo", a.batchNo);
    }

    public Map<String, Object> announcement() {
        List<Announcement> list = em.createQuery(
                "select a from Announcement a order by a.id desc", Announcement.class).getResultList();
        List<Map<String, Object>> groups = new ArrayList<>();
        for (LabRoom l : labs()) {
            List<Map<String, Object>> rows = new ArrayList<>();
            for (Mentor m : mentorsOfLab(l.id)) {
                for (Enrollment e : em.createQuery(
                                "select e from Enrollment e where e.mentorId=:m order by e.studentId", Enrollment.class)
                        .setParameter("m", m.id).getResultList()) {
                    Student s = student(e.studentId);
                    Map<String, Object> r = new LinkedHashMap<>();
                    r.put("studentNo", s == null ? "" : s.no);
                    r.put("studentName", s == null ? "" : s.name);
                    r.put("className", s == null ? "" : s.className);
                    r.put("mentorName", m.name);
                    r.put("source", e.source);
                    r.put("status", e.status);
                    rows.add(r);
                }
            }
            groups.add(Map.of("code", l.code, "name", l.name, "rows", rows));
        }
        return Map.of("announcements", list, "groups", groups);
    }

    public Map<String, Object> studentHome(Long studentId) {
        Student s = student(studentId);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("student", s);
        out.put("intentionCount", count("select count(i) from Intention i where i.studentId=:s", "s", studentId));
        out.put("offerCount", count("select count(o) from Offer o where o.studentId=:s and o.status='PENDING'", "s", studentId));
        Enrollment e = myEnrollment(studentId);
        out.put("enrollment", e == null ? null : enrollmentRow(e));
        return out;
    }

    // ---------------------------------------------------------------- 基础数据维护

    @Transactional
    public Map<String, Object> addMentor(Long labId, String name, String title, String tags, int quota) {
        Mentor m = new Mentor();
        m.labId = labId;
        m.name = name;
        m.title = title;
        m.tags = tags;
        m.quota = quota;
        m.intro = name + "，" + title + "。研究方向：" + tags + "。";
        m.introStatus = "DRAFT";
        em.persist(m);
        return Map.of("ok", true, "id", m.id);
    }

    @Transactional
    public Map<String, Object> moveMentor(Long mentorId, Long labId) {
        Mentor m = mentor(mentorId);
        if (m == null) throw new ApiException("导师不存在");
        long enrolled = enrolledCount(mentorId);
        if (enrolled > 0) throw new ApiException("该导师名下已有 " + enrolled + " 名学生，需先完成调剂再移动");
        m.labId = labId;
        em.merge(m);
        return Map.of("ok", true);
    }

    @Transactional
    public Map<String, Object> deleteMentor(Long mentorId) {
        Mentor m = mentor(mentorId);
        if (m == null) throw new ApiException("导师不存在");
        if (enrolledCount(mentorId) > 0) throw new ApiException("该导师名下已有学生，不能删除");
        m.status = "DISABLED";
        em.merge(m);
        return Map.of("ok", true);
    }

    @Transactional
    public Map<String, Object> addLab(String code, String name, String direction, int capacity) {
        LabRoom l = new LabRoom();
        l.code = code;
        l.name = name;
        l.direction = direction;
        l.capacity = capacity;
        em.persist(l);
        return Map.of("ok", true, "id", l.id);
    }
}
