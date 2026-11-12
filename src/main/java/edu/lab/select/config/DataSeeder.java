package edu.lab.select.config;

import edu.lab.select.model.*;
import edu.lab.select.service.AccountService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** 演示数据。真实部署时由学院导入名单，此处用于把八阶段流程完整跑通。 */
@Component
public class DataSeeder implements CommandLineRunner {

    @PersistenceContext
    private EntityManager em;

    private final AccountService accounts;

    public DataSeeder(AccountService accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Long exists = em.createQuery("select count(l) from LabRoom l", Long.class).getSingleResult();
        if (exists > 0) return;

        seedStages();
        Long[] labs = seedLabs();
        List<Mentor> mentors = seedMentors(labs);
        List<Student> students = seedStudents();
        seedIntentions(students, mentors);
        seedScoresAndDecisions(mentors);
        seedAccounts(students, mentors);
    }

    /** 一人一号：学生用学号，导师用工号；初始密码为 Lab@ + 编号后四位，管理员单独设定。 */
    private void seedAccounts(List<Student> students, List<Mentor> mentors) {
        for (Student s : students) {
            accounts.create(s.no, "Lab@" + tail(s.no), "student", s.id, false);
        }
        for (Mentor m : mentors) {
            accounts.create(m.no, "Lab@" + tail(m.no), "mentor", m.id, false);
        }
        accounts.create("admin", "Admin@2026", "admin", 1L, false);
    }

    private String tail(String no) {
        return no.length() <= 4 ? no : no.substring(no.length() - 4);
    }

    private void seedStages() {
        Object[][] rows = {
                {"PREACH", "集中宣讲", 1, "2026-10-29T14:00", "2026-10-29T18:00", "学院统一宣讲，各实训室介绍方向、名额与考核要求"},
                {"OPENDAY", "实训室开放日", 2, "2026-10-29T18:00", "2026-10-30T22:00", "学生实地走访，与导师、高年级学生交流"},
                {"INTENT", "意向填报", 3, "2026-11-02T08:00", "2026-11-02T12:00", "最多填报 3 个平行导师意向，不分先后"},
                {"INTERVIEW", "导师初选与面试考核", 4, "2026-11-02T12:00", "2026-11-03T17:00", "以实训室为单位统一面试，确定拟录取、递补与调剂候选"},
                {"CONFIRM", "拟录取与学生确认", 5, "2026-11-04T08:00", "2026-11-04T12:00", "单意向自动确认，多意向限时择一，逾期释放名额"},
                {"SUPPLEMENT", "分轮补录", 6, "2026-11-04T12:00", "2026-11-05T17:00", "公布剩余名额，重新填报，顺位递补"},
                {"ADJUST", "统筹调剂录取", 7, "2026-11-06T08:00", "2026-11-06T12:00", "内部调剂优先，其次跨室调剂，最后统筹安置"},
                {"PUBLISH", "结果公示与正式入组", 8, "2026-11-06T12:00", "2026-11-07T12:00", "公示 1 天，无异议后正式入组"},
        };
        for (Object[] r : rows) {
            StageConfig s = new StageConfig();
            s.code = (String) r[0];
            s.name = (String) r[1];
            s.seq = (Integer) r[2];
            s.startAt = LocalDateTime.parse((String) r[3]);
            s.endAt = LocalDateTime.parse((String) r[4]);
            s.summary = (String) r[5];
            em.persist(s);
        }
    }

    private Long[] seedLabs() {
        Object[][] rows = {
                {"202", "智能感知实验室", "多模态感知 / 边缘计算", 12, "主攻端侧智能，设备齐全，适合愿意动手的同学"},
                {"204", "数据智能实验室", "数据挖掘 / 大模型应用", 12, "有脱敏校园数据集与推理算力"},
                {"206", "嵌入式系统实验室", "嵌入式 / 机器人控制", 10, "四足与差速底盘平台，需两人以上操作"},
                {"505", "人机交互实验室", "交互设计 / 可穿戴", 10, "偏用户研究，需要跑访谈与可用性测试"},
                {"402", "视觉计算实验室", "计算机视觉 / 三维重建", 12, "自研采集设备，数学基础要求较高"},
                {"404", "网络系统实验室", "网络安全 / 分布式系统", 10, "隔离实验环境，规则严格"},
                {"406", "软件工程实验室", "软件工程 / 智能体工程", 14, "与真实业务对接，强调工程规范"},
        };
        Long[] ids = new Long[rows.length];
        for (int i = 0; i < rows.length; i++) {
            LabRoom l = new LabRoom();
            l.code = (String) rows[i][0];
            l.name = (String) rows[i][1];
            l.direction = (String) rows[i][2];
            l.capacity = (Integer) rows[i][3];
            l.note = (String) rows[i][4];
            em.persist(l);
            ids[i] = l.id;
        }
        return ids;
    }

    private List<Mentor> seedMentors(Long[] labs) {
        Object[][] rows = {
                {0, "陈立行", "教授", "多模态感知 · 边缘推理", 4,
                        "做的是把视觉、语音、惯导塞进一块开发板里跑起来。组里目前在做园区巡检机器人的端侧识别，学生进来先从数据采集和模型量化入手，三个月后独立负责一个传感器模块。希望你能坐得住、愿意调参数、不排斥写文档。"},
                {0, "周予安", "副教授", "传感器融合 · 嵌入式视觉", 3,
                        "研究多传感器时间同步与融合定位，课题来自与本地企业的联合项目。培养方式是每周一次代码评审，学生需要自己维护一套可复现的实验记录。比起聪明，我更看重按时交付。"},
                {1, "林知远", "教授", "大模型应用 · 知识图谱", 4,
                        "组里在做面向教学场景的领域问答，需要把课程资料做成可检索的知识库。学生进来会先接一段真实数据，从清洗到评测完整走一遍。要求能读英文论文，愿意做枯燥的评测。"},
                {1, "沈彦", "讲师", "数据挖掘 · 推荐系统", 3,
                        "方向是行为序列建模与冷启动推荐，手上有脱敏的校园服务日志。培养上强调工程规范，代码要过 CI。希望学生至少熟悉一门脚本语言和基本的 SQL。"},
                {1, "何晓帆", "高级工程师", "数据平台 · 工程化落地", 2,
                        "负责把算法组的原型做成能跑的服务。学生在这里会接触到容器、调度、监控这些真实环境里的东西。不要求你算法多强，但要求你出了问题能自己定位。"},
                {2, "赵恒", "副教授", "机器人控制 · 运动规划", 3,
                        "实验室有一台四足和两台差速底盘，做室内导航与抓取。学生从仿真环境开始，跑通之后再上真机。安全第一，真机操作必须两人在场。"},
                {2, "吴桐", "讲师", "嵌入式系统 · 实时调度", 3,
                        "关注实时性，做电机控制环和通信总线的优化。学生需要会看示波器和逻辑分析仪。进来先跟着做一轮硬件调试，再谈算法。"},
                {3, "苏禾", "副教授", "交互设计 · 可穿戴计算", 3,
                        "做贴身设备上的交互，比如手表、指环、织物传感。组里偏用户研究，学生要跑访谈、做原型、组织可用性测试。欢迎会写代码的设计师，也欢迎愿意学设计的学生。"},
                {3, "郑一舟", "讲师", "人机协同 · 可用性评估", 2,
                        "研究人和智能体协作时的信任与接管，实验以人为被试。学生需要设计实验、招募被试、做统计分析。对心理学方法感兴趣的同学会做得比较舒服。"},
                {4, "罗启明", "教授", "三维重建 · SLAM", 4,
                        "做室内场景的稠密重建与定位，有一套自研采集设备。学生进来先复现一篇经典论文，再谈改进。数学基础要过得去，线性代数和优化不能全靠调库。"},
                {4, "蒋沐", "副教授", "视频理解 · 生成模型", 3,
                        "方向是长视频中的动作理解与可控生成。算力有限，所以更看重方法设计而不是堆模型。每周组会一次，学生要能讲清楚自己这周失败在哪。"},
                {5, "韩澈", "教授", "网络安全 · 协议分析", 4,
                        "做工业协议的异常检测与固件分析。学生需要熟悉抓包和逆向的基本工具。所有实验在隔离环境内进行，规则很严。"},
                {5, "邓维", "讲师", "分布式系统 · 可观测性", 3,
                        "关注大规模系统的故障定位，做链路追踪与日志压缩。组里有一套小规模集群可以随便折腾。希望你能读源码，不只是会调 API。"},
                {6, "方叙", "副教授", "软件工程 · 智能体工程", 4,
                        "研究多智能体系统的协作与评测，做的是把大模型接进真实业务流程。学生要写测试、做评测集，不能只写 demo。我更看重工程判断力。"},
                {6, "唐棠", "讲师", "需求工程 · 软件测试", 3,
                        "方向是需求一致性与自动化测试生成。学生会参与真实项目的需求梳理和用例设计。要求文字表达清楚，能跟非技术的人把话说清楚。"},
                {6, "白榆", "高级工程师", "DevOps · 云原生", 3,
                        "负责实验室的构建与发布流水线。学生在这里会真正跑通一次从提交到上线的全过程。希望你怕麻烦但不怕重复劳动。"},
        };
        List<Mentor> out = new ArrayList<>();
        int seq = 0;
        for (Object[] r : rows) {
            Mentor m = new Mentor();
            m.no = "T" + (2001 + seq++);
            m.labId = labs[(Integer) r[0]];
            m.name = (String) r[1];
            m.title = (String) r[2];
            m.tags = (String) r[3];
            m.quota = (Integer) r[4];
            m.intro = (String) r[5];
            m.introStatus = "APPROVED";
            em.persist(m);
            out.add(m);
        }
        return out;
    }

    private List<Student> seedStudents() {
        String[] names = {"陈嘉禾", "李昊然", "王雨桐", "张子航", "刘思远", "孙悦宁", "周牧之", "吴涵",
                "郑亦航", "何书言", "冯知夏", "蒋墨", "沈清和", "韩牧野", "杨知微", "朱砚",
                "秦亦然", "许嘉树", "邓清越", "曹星野", "彭沐", "苏行止", "卢思齐", "崔文柏",
                "谭书宁", "范云舟"};
        String[] classes = {"计科2201", "计科2202", "软工2201", "软工2202", "人工智能2201", "物联网2201"};
        String[] skills = {"Python", "C++", "前端", "数据分析", "算法", "嵌入式"};
        List<Student> out = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            Student s = new Student();
            s.no = "2022" + String.format("%04d", 1001 + i * 7);
            s.name = names[i];
            s.className = classes[i % classes.length];
            s.gpa = Math.round((3.1 + ((i * 37) % 14) / 10.0) * 10) / 10.0;
            s.skill = skills[i % skills.length];
            em.persist(s);
            out.add(s);
        }
        return out;
    }

    private void seedIntentions(List<Student> students, List<Mentor> mentors) {
        int n = mentors.size();
        for (int i = 0; i < students.size(); i++) {
            Student s = students.get(i);
            int count = 1 + (i % 3); // 1~3 个平行意向
            Set<Integer> picked = new LinkedHashSet<>();
            int k = 0;
            while (picked.size() < count && k < 12) {
                picked.add((i * 5 + k * 3 + 1) % n);
                k++;
            }
            for (Integer idx : picked) {
                Intention in = new Intention();
                in.studentId = s.id;
                in.mentorId = mentors.get(idx).id;
                in.acceptInner = i % 3 != 0;
                in.acceptCross = i % 2 == 0;
                in.submittedAt = LocalDateTime.of(2026, 11, 2, 8, 10 + (i % 40));
                em.persist(in);
            }
        }
    }

    private void seedScoresAndDecisions(List<Mentor> mentors) {
        for (Mentor m : mentors) {
            List<Intention> list = em.createQuery(
                            "select i from Intention i where i.mentorId=:m", Intention.class)
                    .setParameter("m", m.id).getResultList();
            List<InterviewScore> scores = new ArrayList<>();
            for (Intention in : list) {
                InterviewScore sc = new InterviewScore();
                sc.mentorId = m.id;
                sc.studentId = in.studentId;
                int base = (int) (in.studentId * 7 % 5);
                sc.attitude = 3 + (base % 3);
                sc.logic = 2 + ((base + 1) % 4);
                sc.coding = 2 + ((base + 2) % 4);
                sc.timeInput = 3 + ((base + 3) % 3);
                sc.total = (sc.attitude + sc.logic + sc.coding + sc.timeInput) * 5;
                String[] high = {"基础扎实，表达清楚，能承担独立模块。", "动手快，问题定位清楚，适合做端侧。", "思路完整，愿意啃文档，可以直接进项目。"};
                String[] mid = {"动手能力尚可，需要有人在方向上带一段。", "基础够用，主动性还可以，先跟一轮迭代。", "表达稍弱，但代码写得干净。"};
                String[] low = {"目前基础偏弱，建议先补齐基础课再进组。", "投入时间不确定，需要先明确每周可支配时间。", "对方向理解还比较模糊，建议先参加开放日。"};
                int k = (int) ((sc.studentId * 3 + m.id) % 3);
                sc.comment = sc.total >= 80 ? high[k] : sc.total >= 65 ? mid[k] : low[k];
                em.persist(sc);
                scores.add(sc);
            }
            scores.sort((a, b) -> b.total - a.total);
            int rank = 1;
            for (int i = 0; i < scores.size(); i++) {
                InterviewScore sc = scores.get(i);
                MentorDecision d = new MentorDecision();
                d.mentorId = m.id;
                d.studentId = sc.studentId;
                if (i < m.quota) {
                    d.decision = "ADMIT";
                } else if (i < m.quota + 2) {
                    d.decision = "WAIT";
                    d.waitRank = rank++;
                } else {
                    d.decision = "ADJUST";
                }
                em.persist(d);
            }
        }
    }
}
