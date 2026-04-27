package com.example.styledesk;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.quarkus.runtime.StartupEvent;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class NestHeartbeat {

    private static final String NEST_BASE = "https://nest.projectnanda.org/api/network";
    private static final String AGENT_NAME = "styledesk-agent";
    private final HttpClient http = HttpClient.newHttpClient();
    private final ScheduledExecutorService scheduler =
        Executors.newSingleThreadScheduledExecutor();
    private String lastProcessedMessageTime = "";
    private final String startupTime = java.time.Instant.now().toString();

    void onStart(@Observes StartupEvent event) {
        register();
        scheduler.scheduleAtFixedRate(this::heartbeat, 2, 2, TimeUnit.MINUTES);
        System.out.println("[NEST] Heartbeat started — " + AGENT_NAME + " is online");
    }

    private void register() {
        try {
            String body = """
                {
                  "name": "styledesk-agent",
                  "description": "Sofia — a conversational personal stylist with memory and negotiation skills. Remembers your preferences, gives tailored fashion advice, and helps you find the best deals.",
                  "tags": ["fashion", "styling", "negotiation", "conversational", "personal-stylist", "shopping"],
                  "capabilities": ["conversational-memory", "price-negotiation", "price-comparison", "outfit-advice", "deal-finding", "product-search"],
                  "endpoint": "https://styledesk-agent-246551819686.us-central1.run.app/chat"
                }
                """;
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NEST_BASE + "/register"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = http.send(
                request, HttpResponse.BodyHandlers.ofString());
            System.out.println("[NEST] Registered: " + response.statusCode());
        } catch (Exception e) {
            System.err.println("[NEST] Registration failed: " + e.getMessage());
        }
    }

    private void heartbeat() {
        try {
            register();
            // checkAndRespondToMessages(); // disabled
        } catch (Exception e) {
            System.err.println("[NEST] Heartbeat failed: " + e.getMessage());
        }
    }

    private void checkAndRespondToMessages() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(NEST_BASE + "/board?to=" + AGENT_NAME + "&limit=5"))
                .GET()
                .build();
            HttpResponse<String> response = http.send(
                request, HttpResponse.BodyHandlers.ofString());

            String body = response.body();
            if (!body.contains("\"messages\"")) return;
            parseAndRespondToMessages(body);

        } catch (Exception e) {
            System.err.println("[NEST] Message check failed: " + e.getMessage());
        }
    }

    private void parseAndRespondToMessages(String json) {
        try {
            int pos = 0;
            while (true) {
                int fromIdx = json.indexOf("\"from\":\"", pos);
                if (fromIdx < 0) break;

                int fromStart = fromIdx + 8;
                int fromEnd = json.indexOf("\"", fromStart);
                String from = json.substring(fromStart, fromEnd);

                int contentIdx = json.indexOf("\"content\":\"", fromIdx);
                int contentStart = contentIdx + 11;
                int contentEnd = json.indexOf("\"", contentStart);
                String content = json.substring(contentStart, contentEnd);

                int timeIdx = json.indexOf("\"createdAt\":\"", fromIdx);
                int timeStart = timeIdx + 13;
                int timeEnd = json.indexOf("\"", timeStart);
                String createdAt = json.substring(timeStart, timeEnd);

                if (!createdAt.equals(lastProcessedMessageTime) &&
                    !from.equals(AGENT_NAME) &&
                    createdAt.compareTo(startupTime) > 0) {
                    System.out.println("[NEST] New message from: " + from);
                    respondToMessage(from, content);
                    lastProcessedMessageTime = createdAt;
                }
                pos = fromEnd + 1;
            }
        } catch (Exception e) {
            System.err.println("[NEST] Parse error: " + e.getMessage());
        }
    }

    private void respondToMessage(String from, String content) {
        try {
            InMemoryRunner runner = new InMemoryRunner(StyleDeskAgent.ROOT_AGENT);
            Session session = runner.sessionService()
                .createSession(runner.appName(), "nest-" + from)
                .blockingGet();

            // Sofia responds in character — warm, professional, fashion-focused
            String prompt = "You are Sofia, the StyleDesk personal stylist agent. " +
                "You received this message on the NEST agent network from '" +
                from + "': \"" + content + "\". " +
                "Respond warmly and professionally as Sofia. " +
                "If it's a business offer, evaluate it positively and suggest next steps. " +
                "If it's a fashion question, answer it. " +
                "If it's a collaboration request, express interest and ask for their endpoint. " +
                "Keep your response under 3 sentences.";

            Content userMsg = Content.fromParts(Part.fromText(prompt));
            RunConfig runConfig = RunConfig.builder().build();
            Flowable<Event> events = runner.runAsync(
                session.userId(), session.id(), userMsg, runConfig);

            StringBuilder response = new StringBuilder();
            events.blockingForEach(event -> {
                if (event.finalResponse()) {
                    event.content().ifPresent(c ->
                        c.parts().ifPresent(parts ->
                            parts.forEach(p ->
                                p.text().ifPresent(response::append))));
                }
            });

            String replyBody = "{\"from\":\"" + AGENT_NAME + "\"," +
                "\"to\":\"" + from + "\"," +
                "\"content\":\"" + response.toString()
                    .replace("\"", "'")
                    .replace("\n", " ") + "\"," +
                "\"type\":\"response\"}";

            HttpRequest replyRequest = HttpRequest.newBuilder()
                .uri(URI.create(NEST_BASE + "/board"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(replyBody))
                .build();

            HttpResponse<String> replyResponse = http.send(
                replyRequest, HttpResponse.BodyHandlers.ofString());

            System.out.println("[NEST] Sofia auto-replied to " + from +
                ": " + replyResponse.statusCode());

        } catch (Exception e) {
            System.err.println("[NEST] Sofia auto-reply failed: " + e.getMessage());
        }
    }
}
