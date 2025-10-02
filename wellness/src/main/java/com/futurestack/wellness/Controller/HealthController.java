package com.futurestack.wellness.Controller;


import com.futurestack.wellness.Model.Summary7d;
import com.futurestack.wellness.Service.CerebrasService;
import com.futurestack.wellness.Service.HealthService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin
public class HealthController {

    private final HealthService health;
    private final CerebrasService cerebras;

    public HealthController(HealthService health, CerebrasService cerebras) {
        this.health = health; this.cerebras = cerebras;
    }

    @PostMapping(path="/health/upload", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public Summary7d upload(@RequestParam("file") MultipartFile file) throws Exception {
        return health.uploadAndSummarize(file);
    }

    @GetMapping("/health/summary")
    public Summary7d summary() { return health.getLatest(); }

    @PostMapping("/insights")
    public String insights(@RequestParam(value="user", required=false) String user) throws Exception {
        return cerebras.getInsights(user==null? "Guest" : user);
    }



}
