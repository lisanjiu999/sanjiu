# 学生实训室进组选拔系统

按《学生实训室进组选拔系统 程序设计规划书》实现的完整可运行原型：八阶段流程、三端（学生 / 导师 / 管理员）、平行意向、双向选择、分轮补录、三级优先统筹调剂、公示归档。

- 后端：Java 17 + Spring Boot 3.2.5 + Spring Data JPA + H2（内存库，启动自动灌入演示数据）
- 前端：原生 HTML / CSS / JavaScript（无构建步骤，由 Spring Boot 直接托管）
- 设计：厚描边卡通（3px 墨色描边 + 硬投影 + 大圆角 + 柔和色板），详见 `DESIGN.md`

## 运行

**最简单：双击项目根目录的 `启动.bat`**（Windows）。脚本会先检查 jar，再开浏览器并起服务；关掉窗口即停止。

命令行方式：

```bash
cd selection-system

# 已有 jar，直接跑
java -jar target/selection-system-1.0.0.jar --server.port=8080

# 或让 Maven 起（改完 Java 代码不用手动打包）
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8080

# 或先打包再跑（改完前端/后端都建议这一步）
mvn clean package -DskipTests
java -jar target/selection-system-1.0.0.jar --server.port=8080
```

Git Bash / macOS / Linux 用 `./start.sh`。

打开 <http://localhost:8080>。

> 若环境变量里存在 `SERVER__PORT`，Spring Boot 会优先采用它，导致端口不是 8080；用 `--server.port=8080` 显式覆盖即可。

### 只想改前端样式时

静态资源打在 jar 里，改完 css/js 必须重新 `mvn package` 才会生效；调试期可以让服务直接读源码目录：

```bash
java -jar target/selection-system-1.0.0.jar --server.port=8080 \
  --spring.web.resources.static-locations=file:./src/main/resources/static/
```

改完刷新即可（静态资源已设 `Cache-Control: no-store`）。`index.html` 里 `app.css?v=...` 的版本串在正式发布时记得往上推一位。

## 账号与密码

一人一号，必须输入账号与密码登录。预置数据：7 间实训室、16 位导师、26 名学生、51 条平行意向、51 份面试评分与决策。

| 身份 | 账号 | 初始密码 |
| --- | --- | --- |
| 学生 | 学号，如 `20221001` | `Lab@` + 学号后四位，如 `Lab@1001` |
| 导师 | 工号 `T2001`—`T2016` | `Lab@` + 工号后四位，如 `Lab@2001` |
| 管理员 | `admin` | `Admin@2026` |

登录页右下角的「演示账号」可以展开查看这三组示例，其余账号按上面的规则推导。

口令以 PBKDF2-HMAC-SHA256（每账号独立随机盐，60000 次迭代）存于 `account` 表，数据库里没有明文。登录失败 5 次锁定 5 分钟；会话 8 小时过期。登录后可在右上角「修改密码」，改密成功会强制重新登录（旧 token 立即失效），新密码至少 8 位且需含字母和数字。

## 演示路径（建议按顺序）

1. **学生端**：总览 → 实训室 → 导师详情（三级下钻，看导师自己写的介绍）→ 意向填报（3 个平行位 + 两个调剂开关）。
2. **导师端**：概览 → 我的介绍（编辑并提交审核）→ 报名学生（注意：看不到志愿顺序）→ 面试评分（四维）→ 录取决策（拟录取受名额限制）。
3. **管理员端**：录取看板 → 阶段控制 → 点「推进到下一阶段」，观察自动结算。
4. 推进到「拟录取与学生确认」：46 条意向生成、11 人单意向自动确认；换一个有 2 个拟录取的学生看「我的结果」，可择一确认。
5. 继续推进：确认阶段结束后逾期名额释放并按递补顺序补位 → 统筹调剂（三级优先试算 → 确认落库）→ 公示发布，录取结果转正式。

「演示时钟」是可控的：所有阶段判断都基于它。**管理员 → 阶段控制** 可以直接拨日期时间（含前一天/后一天/本阶段开始/下一阶段四个快捷键），八个阶段的时间窗也能在表格里逐行改，改完即存、立即生效。真实部署把 `StageService.now()` 换成 `LocalDateTime.now()` 即可。

## 目录

```
selection-system/
├─ 启动.bat / start.bat   一键启动（检查 jar → 开浏览器 → 起服务）
├─ start.sh              Git Bash / macOS / Linux 用
├─ src/main/java/edu/lab/select/
│  ├─ model/          实体：账号、实训室、导师、学生、意向、拟录取、录取、决策、评分、阶段、调剂、公示
│  ├─ service/        SelectionService（业务与算法）、StageService（阶段时间窗）、
│  │                  AccountService（登录/改密/锁定）、PasswordService（PBKDF2）、TokenStore（会话）
│  ├─ web/            Auth / Catalog / Student / Mentor / Admin 五个控制器 + 统一错误处理
│  └─ config/         DataSeeder（演示数据）
├─ src/main/resources/
│  ├─ application.yml
│  └─ static/         index.html + assets/{app.css, core.js, views.*.js, main.js}
│                     assets/fonts2/  Fredoka + Nunito 自托管字体（8 个 woff2，无外网依赖）
└─ DESIGN.md          视觉系统说明
```

## 与规划书对应的关键实现

| 规划书要点 | 代码位置 |
| --- | --- |
| 阶段时间窗、服务端裁决 | `StageService`、`SelectionService.submitIntentions` 等入口的阶段校验 |
| 平行意向不落志愿序号 | `Intention` 实体无 seq 字段；`mentorApplicants` DTO 不含该字段 |
| 单意向自动确认 / 逾期释放 | `SelectionService.autoConfirmSingles` / `expireUnconfirmed` |
| 递补按顺序补位 | `SelectionService.fillWaitlist` |
| 三级优先统筹调剂 | `SelectionService.adjustPreview` / `adjustCommit` |
| 名额不超录 | `saveDecision` 校验 + `Enrollment` 学生唯一约束 |
| 阶段推进自动结算 | `SelectionService.advanceStage` |
| 账号口令与登录锁定 | `AccountService.authenticate`、`PasswordService` |

## 已知简化（课程作业范围）

- 账号由 `DataSeeder` 生成，密码为演示用初始口令；真实部署应由管理员导入或对接统一身份认证（SSO），并强制首次登录改密（`Account.mustChange` 已预留）。
- 会话为服务端内存 token（重启失效、多实例需换 Redis）；生产建议改用 JWT 或集中式会话。
- 数据库为 H2 内存库，重启即重置；生产可换 MySQL，`ddl-auto` 改为 `update`。
- 面试场次编排、通知推送、Excel 导入导出在规划书中列为模块，本原型未展开实现。
- 阶段回退、审计日志表结构已预留（`AdjustRecord` 等留痕），界面未开放。
