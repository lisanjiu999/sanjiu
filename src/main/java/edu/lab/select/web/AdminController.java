package edu.lab.select.web;

import edu.lab.select.service.SelectionService;
import edu.lab.select.service.TokenStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController extends BaseController {

    private final SelectionService service;

    public AdminController(TokenStore tokens, SelectionService service) {
        super(tokens);
        this.service = service;
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestHeader(value = "X-Token", required = false) String token) {
        self(token, "admin");
        return service.dashboard();
    }

    @PostMapping("/stage/advance")
    public Map<String, Object> advance(@RequestHeader(value = "X-Token", required = false) String token) {
        self(token, "admin");
        return service.advanceStage();
    }

    /** 拨动演示时钟：整站的「现在」由它决定。 */
    @PutMapping("/stage/clock")
    public Map<String, Object> clock(@RequestHeader(value = "X-Token", required = false) String token,
                                     @RequestBody Map<String, Object> body) {
        self(token, "admin");
        return service.setClock(parseTime(body.get("at")));
    }

    /** 改某个阶段的时间窗。 */
    @PutMapping("/stage/{id}/window")
    public Map<String, Object> window(@RequestHeader(value = "X-Token", required = false) String token,
                                      @PathVariable Long id, @RequestBody Map<String, Object> body) {
        self(token, "admin");
        return service.updateStageWindow(id, parseTime(body.get("startAt")), parseTime(body.get("endAt")));
    }

    @PostMapping("/labs")
    public Map<String, Object> addLab(@RequestHeader(value = "X-Token", required = false) String token,
                                      @RequestBody Map<String, Object> body) {
        self(token, "admin");
        return service.addLab(str(body.get("code")), str(body.get("name")), str(body.get("direction")),
                body.get("capacity") == null ? 10 : (int) Double.parseDouble(String.valueOf(body.get("capacity"))));
    }

    @PostMapping("/mentors")
    public Map<String, Object> addMentor(@RequestHeader(value = "X-Token", required = false) String token,
                                         @RequestBody Map<String, Object> body) {
        self(token, "admin");
        return service.addMentor(Long.valueOf(String.valueOf(body.get("labId"))), str(body.get("name")),
                str(body.get("title")), str(body.get("tags")),
                body.get("quota") == null ? 3 : (int) Double.parseDouble(String.valueOf(body.get("quota"))));
    }

    @PutMapping("/mentors/{id}/move")
    public Map<String, Object> move(@RequestHeader(value = "X-Token", required = false) String token,
                                    @PathVariable Long id, @RequestBody Map<String, Object> body) {
        self(token, "admin");
        return service.moveMentor(id, Long.valueOf(String.valueOf(body.get("labId"))));
    }

    @DeleteMapping("/mentors/{id}")
    public Map<String, Object> remove(@RequestHeader(value = "X-Token", required = false) String token,
                                      @PathVariable Long id) {
        self(token, "admin");
        return service.deleteMentor(id);
    }

    @GetMapping("/adjust/preview")
    public Map<String, Object> preview(@RequestHeader(value = "X-Token", required = false) String token) {
        self(token, "admin");
        return service.adjustPreview();
    }

    @PostMapping("/adjust/commit")
    public Map<String, Object> commit(@RequestHeader(value = "X-Token", required = false) String token) {
        self(token, "admin");
        return service.adjustCommit();
    }

    @PostMapping("/announce")
    public Map<String, Object> announce(@RequestHeader(value = "X-Token", required = false) String token,
                                        @RequestBody Map<String, Object> body) {
        self(token, "admin");
        return service.publish(str(body.get("title")));
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    /** 接受 "2026-11-02T09:30" / "2026-11-02T09:30:00" 两种写法。 */
    private java.time.LocalDateTime parseTime(Object o) {
        String s = str(o).trim();
        if (s.isEmpty()) throw new edu.lab.select.service.ApiException("请提供时间");
        try {
            return java.time.LocalDateTime.parse(s.length() == 16 ? s : s.substring(0, 16));
        } catch (Exception e) {
            throw new edu.lab.select.service.ApiException("时间格式不对，应为 2026-11-02T09:30");
        }
    }
}
