package edu.lab.select.web;

import edu.lab.select.service.SelectionService;
import edu.lab.select.service.TokenStore;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student")
public class StudentController extends BaseController {

    private final SelectionService service;

    public StudentController(TokenStore tokens, SelectionService service) {
        super(tokens);
        this.service = service;
    }

    @GetMapping("/home")
    public Map<String, Object> home(@RequestHeader(value = "X-Token", required = false) String token) {
        return service.studentHome(self(token, "student"));
    }

    @GetMapping("/intentions")
    public Map<String, Object> intentions(@RequestHeader(value = "X-Token", required = false) String token) {
        return service.myIntentions(self(token, "student"));
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/intentions")
    public Map<String, Object> submit(@RequestHeader(value = "X-Token", required = false) String token,
                                      @RequestBody Map<String, Object> body) {
        Long sid = self(token, "student");
        List<Long> ids = new ArrayList<>();
        Object raw = body.get("mentorIds");
        if (raw instanceof List<?> list) {
            for (Object o : list) ids.add(Long.valueOf(String.valueOf(o)));
        }
        boolean inner = Boolean.TRUE.equals(body.get("acceptInner"));
        boolean cross = Boolean.TRUE.equals(body.get("acceptCross"));
        return service.submitIntentions(sid, ids, inner, cross);
    }

    @GetMapping("/offers")
    public Map<String, Object> offers(@RequestHeader(value = "X-Token", required = false) String token) {
        return service.myOffers(self(token, "student"));
    }

    @PostMapping("/offers/{id}/confirm")
    public Map<String, Object> confirm(@RequestHeader(value = "X-Token", required = false) String token,
                                       @PathVariable Long id) {
        return service.confirmOffer(self(token, "student"), id);
    }
}
