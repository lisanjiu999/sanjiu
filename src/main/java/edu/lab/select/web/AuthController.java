package edu.lab.select.web;

import edu.lab.select.model.Account;
import edu.lab.select.model.LabRoom;
import edu.lab.select.model.Mentor;
import edu.lab.select.model.StageConfig;
import edu.lab.select.model.Student;
import edu.lab.select.service.AccountService;
import edu.lab.select.service.ApiException;
import edu.lab.select.service.SelectionService;
import edu.lab.select.service.StageService;
import edu.lab.select.service.TokenStore;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class AuthController extends BaseController {

    private final SelectionService service;
    private final StageService stage;
    private final AccountService accounts;

    public AuthController(TokenStore tokens, SelectionService service, StageService stage, AccountService accounts) {
        super(tokens);
        this.service = service;
        this.stage = stage;
        this.accounts = accounts;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String username = str(body.get("username"));
        String password = str(body.get("password"));
        if (username.isEmpty() || password.isEmpty()) throw new ApiException("请输入账号和密码");

        Account a = accounts.authenticate(username, password);
        String name;
        String sub;
        switch (a.role) {
            case "student" -> {
                Student s = service.student(a.refId);
                if (s == null) throw new ApiException("账号关联的学生不存在");
                name = s.name;
                sub = s.no + " · " + s.className;
            }
            case "mentor" -> {
                Mentor m = service.mentor(a.refId);
                LabRoom l = m == null ? null : service.lab(m.labId);
                name = m == null ? "" : m.name;
                sub = (m == null ? "" : m.title) + " · " + (l == null ? "" : l.code + " " + l.name);
            }
            default -> {
                name = "学院管理员";
                sub = "教学办公室";
            }
        }
        String token = tokens.issue(a.role, a.refId, a.id);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("token", token);
        out.put("role", a.role);
        out.put("id", a.refId);
        out.put("name", name);
        out.put("sub", sub);
        out.put("mustChange", a.mustChange);
        return out;
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "X-Token", required = false) String token) {
        tokens.revoke(token);
        return Map.of("ok", true);
    }

    @PostMapping("/password")
    public Map<String, Object> changePassword(@RequestHeader(value = "X-Token", required = false) String token,
                                              @RequestBody Map<String, Object> body) {
        TokenStore.Session s = session(token);
        accounts.changePassword(s.accountId(), str(body.get("oldPassword")), str(body.get("newPassword")));
        tokens.revokeAccount(s.accountId());
        return Map.of("ok", true);
    }

    @GetMapping("/bootstrap")
    public Map<String, Object> bootstrap(@RequestHeader(value = "X-Token", required = false) String token) {
        TokenStore.Session s = session(token);
        List<StageConfig> stages = stage.stages();
        Map<String, Object> me = new LinkedHashMap<>();
        me.put("role", s.role());
        me.put("id", s.id());
        switch (s.role()) {
            case "student" -> {
                Student st = service.student(s.id());
                me.put("name", st == null ? "" : st.name);
                me.put("sub", st == null ? "" : st.no + " · " + st.className);
            }
            case "mentor" -> {
                Mentor m = service.mentor(s.id());
                LabRoom l = m == null ? null : service.lab(m.labId);
                me.put("name", m == null ? "" : m.name);
                me.put("sub", (m == null ? "" : m.title) + " · " + (l == null ? "" : l.code + " " + l.name));
            }
            default -> {
                me.put("name", "学院管理员");
                me.put("sub", "教学办公室");
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("me", me);
        out.put("stages", stages);
        out.put("current", stage.current());
        out.put("demoNow", stage.now());
        return out;
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o).trim();
    }
}
