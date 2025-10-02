package com.futurestack.wellness.Service;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.futurestack.wellness.Model.Summary7d;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class CerebrasService {
    private final OkHttpClient http = new OkHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final HealthService health;

    @Value("${cerebras.api.key:}") String apiKey;
    @Value("${cerebras.model}")     String model;
    @Value("${cerebras.base.url}")  String baseUrl;

    public CerebrasService(HealthService health){ this.health = health; }

    public String getInsights(String userName) throws Exception {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing Cerebras API key. Set CEREBRAS_API_KEY or 'cerebras.api.key'.");
        }
        Summary7d s = health.getLatest();
        if (s == null) throw new IllegalStateException("Upload a CSV first.");

        String system = """
      You are a precise, encouraging wellness coach. Use the provided 7-day summary to produce:
      - 3 bullet insights: (1) activity & energy load, (2) heart & recovery, (3) gait/quality-of-movement.
      - Flags if relevant: low SpO₂, rising resting HR (>5 bpm vs avg), high environmental audio, prolonged low activity.
      - A 7-day action plan with concrete numbers (daily step goal, exercise minutes, stand hours, hydration, sleep window).
      - Non-diagnostic tone; suggest clinician only if patterns persist or are concerning.
      """;

        String user = """
      User: %s

      7-day Summary:
      - Active Energy (total): %.0f kcal; Resting Energy (total): %.0f kcal
      - Exercise: %.1f min/day avg; Stand Hours (total): %.1f; Stand-goal days (>=12h): %d
      - Move-goal days (>=500 kcal active): %d
      - Distance (total): %.2f mi; Steps (total): %d

      - HR (avg/min/max): %s / %s / %s bpm
      - Resting HR (avg/min/max): %s / %s / %s bpm
      - HRV (median): %s ms
      - Walking HR avg: %s bpm

      - Gait: Walk speed avg: %s mph; Step length avg: %s in
      - Double support avg: %s%%; Asymmetry avg: %s%%
      - Stairs: Up avg: %s ft/s; Down avg: %s ft/s

      - Environment: Audio avg: %s dBA
      - SpO₂: avg %s%%; min %s%%
      """.formatted(
                userName,
                s.totalActiveEnergyKcal(), s.totalRestingEnergyKcal(),
                s.avgExerciseMinPerDay(), s.totalStandHours(), s.standGoalDays(),
                s.moveGoalDays(),
                s.totalDistanceMi(), s.totalSteps(),
                n(s.hrAvg()), n(s.hrMin()), n(s.hrMax()),
                n(s.restingHrAvg()), n(s.restingHrMin()), n(s.restingHrMax()),
                n(s.hrvMedianMs()),
                n(s.walkHrAvg()),
                n(s.walkSpeedAvgMph()), n(s.stepLenAvgIn()),
                n(s.doubleSupportAvgPct()), n(s.asymmetryAvgPct()),
                n(s.stairUpAvgFtPerSec()), n(s.stairDownAvgFtPerSec()),
                n(s.envAudioAvgDbA()),
                n(s.spo2AvgPct()), n(s.spo2MinPct())
        );

        ObjectNode root = mapper.createObjectNode();
        root.put("model", model);
        ArrayNode messages = mapper.createArrayNode();
        messages.add(mapper.createObjectNode().put("role","system").put("content", system));
        messages.add(mapper.createObjectNode().put("role","user").put("content", user));
        root.set("messages", messages);
        root.put("temperature", 0.4);
        root.put("max_tokens", 500);

        Request request = new Request.Builder()
                .url(baseUrl + "/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(mapper.writeValueAsString(root), MediaType.parse("application/json")))
                .build();

        try (Response resp = http.newCall(request).execute()) {
            if (!resp.isSuccessful()) throw new RuntimeException("Cerebras error: " + resp.code() + " " + resp.message());
            JsonNode json = mapper.readTree(resp.body().string());
            return json.path("choices").get(0).path("message").path("content").asText();
        }
    }

    private static String n(Object o){ return o==null? "n/a" : o.toString(); }

}


