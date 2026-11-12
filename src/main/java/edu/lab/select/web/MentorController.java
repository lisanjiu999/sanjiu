package edu.lab.select.web;

import edu.lab.select.service.SelectionService;
import edu.lab.select.service.TokenStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mentor")
public class MentorController extends BaseController {

    private final SelectionService service;

    public MentorController(TokenStore tokens, SelectionService service) {
        super(tokens);
        this.service = service;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview(@RequestHeader(value = "X-Token", required = false) String token) {
        return service.mentorOverview(self(token, "mentor"));
    }

    @GetMapping("/applicants")
    public Map<String, Object> applicants(@RequestHeader(value = "X-Token", required = false) String token) {
        return service.mentorApplicants(self(token, "mentor"));
    }

    @PostMapping("/score")
    public Map<String, Object> score(@RequestHeader(value = "X-Token", required = false) String token,
                                     @RequestBody Map<String, Object> body) {
        Long mid = self(token, "mentor");
        return service.saveScore(mid,
                Long.valueOf(String.valueOf(body.get("studentId"))),
                num(body.get("attitude")), num(body.get("logic")),
                num(body.get("coding")), num(body.get("timeInput")),
                body.get("comment") == null ? "" : String.valueOf(body.get("comment")));
    }

    @PostMapping("/decision")
    public Map<String, Object> decision(@RequestHeader(value = "X-Token", required = false) String token,
                                        @RequestBody Map<String, Object> body) {
        Long mid = self(token, "mentor");
        Integer rank = body.get("waitRank") == null ? null : num(body.get("waitRank"));
        return service.saveDecision(mid,
                Long.valueOf(String.valueOf(body.get("studentId"))),
                String.valueOf(body.get("decision")), rank);
    }

    @PutMapping("/profile")
    public Map<String, Object> profile(@RequestHeader(value = "X-Token", required = false) String token,
                                       @RequestBody Map<String, Object> body) {
        Long mid = self(token, "mentor");
        return service.updateProfile(mid,
                str(body.get("title")), str(body.get("tags")), str(body.get("intro")));
    }

    private int num(Object o) {
        return o == null ? 3 : (int) Double.parseDouble(String.valueOf(o));
    }

    private String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
