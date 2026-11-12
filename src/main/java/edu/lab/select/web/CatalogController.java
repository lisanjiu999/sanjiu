package edu.lab.select.web;

import edu.lab.select.service.SelectionService;
import edu.lab.select.service.StageService;
import edu.lab.select.service.TokenStore;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class CatalogController extends BaseController {

    private final SelectionService service;
    private final StageService stage;

    public CatalogController(TokenStore tokens, SelectionService service, StageService stage) {
        super(tokens);
        this.service = service;
        this.stage = stage;
    }

    @GetMapping("/labs")
    public Map<String, Object> labs(@RequestHeader(value = "X-Token", required = false) String token) {
        session(token);
        return service.labCards();
    }

    @GetMapping("/labs/{id}")
    public Map<String, Object> lab(@RequestHeader(value = "X-Token", required = false) String token,
                                   @PathVariable Long id) {
        session(token);
        return service.labDetail(id);
    }

    @GetMapping("/mentors")
    public Map<String, Object> mentors(@RequestHeader(value = "X-Token", required = false) String token) {
        session(token);
        return Map.of("mentors", service.allMentorCards());
    }

    @GetMapping("/mentors/{id}")
    public Map<String, Object> mentor(@RequestHeader(value = "X-Token", required = false) String token,
                                      @PathVariable Long id) {
        session(token);
        return service.mentorDetail(id);
    }

    @GetMapping("/stages")
    public Map<String, Object> stages(@RequestHeader(value = "X-Token", required = false) String token) {
        session(token);
        return Map.of("stages", stage.stages(), "current", stage.current(), "demoNow", stage.now());
    }

    @GetMapping("/announcement")
    public Map<String, Object> announcement(@RequestHeader(value = "X-Token", required = false) String token) {
        session(token);
        return service.announcement();
    }
}
