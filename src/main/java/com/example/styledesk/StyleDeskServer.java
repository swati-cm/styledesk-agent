package com.example.styledesk;

import com.google.adk.agents.RunConfig;
import com.google.adk.events.Event;
import com.google.adk.runner.InMemoryRunner;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
@Path("/")
public class StyleDeskServer {

    private static final String BASE_URL =
        "https://styledesk-agent-246551819686.us-central1.run.app";

    private final InMemoryRunner runner =
        new InMemoryRunner(StyleDeskAgent.ROOT_AGENT);

    // Per-user sessions — enables multi-turn conversation memory
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    /** Landing page */
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response index() {
        String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8">
              <title>StyleDesk Agent</title>
              <style>
                body { font-family: -apple-system, sans-serif; background: #faf8f5;
                       color: #1b2a4a; max-width: 700px; margin: 4rem auto; padding: 2rem; }
                h1 { font-family: Georgia, serif; font-style: italic; font-size: 2.5rem;
                     color: #1b2a4a; margin-bottom: 0.5rem; }
                .sub { color: #d4847a; font-size: 0.9rem; letter-spacing: 3px;
                       text-transform: uppercase; margin-bottom: 2rem; }
                .card { background: white; border-radius: 12px; padding: 1.5rem;
                        margin-bottom: 1rem; border: 1px solid #e8e4e0; }
                code { background: #f0ece8; padding: 0.8rem; border-radius: 8px;
                       display: block; font-size: 0.82rem; white-space: pre-wrap; margin-top: 0.5rem; }
                .badge { display: inline-block; background: #f0ece8; color: #1b2a4a;
                         padding: 0.2rem 0.7rem; border-radius: 20px; font-size: 0.8rem;
                         margin: 0.2rem; }
              </style>
            </head>
            <body>
              <h1>StyleDesk</h1>
              <p class="sub">Personal Stylist · Deal Negotiator · Fashion AI</p>
              <div class="card">
                <b>Meet Sofia</b> — your personal AI stylist who remembers your preferences,
                gives tailored fashion advice, and helps you negotiate the best deals.
                <br><br>
                <span class="badge">Multi-turn memory</span>
                <span class="badge">Negotiation tactics</span>
                <span class="badge">Price comparison</span>
                <span class="badge">FashionAgent connected</span>
              </div>
              <div class="card">
                <b>Quick Start</b>
                <code>curl -X POST /request-access \\
  -H 'Content-Type: application/json' \\
  -d '{"principal":"my-agent","name":"My Agent","reason":"style advice"}'</code>
              </div>
              <div class="card">
                <b>Chat Example</b>
                <code>curl -X POST /chat \\
  -H 'X-API-Key: YOUR_KEY' \\
  -H 'Content-Type: application/json' \\
  -d '{"userId":"user123","message":"Hi Sofia! I need a winter gala outfit under $300"}'</code>
              </div>
            </body>
            </html>
            """;
        return Response.ok(html).build();
    }

    /**
     * Conversational chat endpoint — maintains memory per userId.
     * Each userId gets their own session so Sofia remembers context.
     */
    @POST
    @Path("chat")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response chat(Map<String, Object> body) {
        try {
            String userId = (String) body.getOrDefault("userId", "anonymous");
            String message = (String) body.get("message");

            if (message == null || message.isBlank()) {
                return Response.status(400).entity(Map.of(
                    "error", "Missing 'message' field",
                    "example", Map.of(
                        "userId", "user123",
                        "message", "Hi Sofia! I need a winter gala outfit under $300"
                    )
                )).build();
            }

            // Get or create session per user — this is what enables memory
            Session session = sessions.computeIfAbsent(userId, id ->
                runner.sessionService()
                    .createSession(runner.appName(), id)
                    .blockingGet()
            );

            Content userMsg = Content.fromParts(Part.fromText(message));
            RunConfig runConfig = RunConfig.builder().build();

            Flowable<Event> events = runner.runAsync(
                session.userId(), session.id(), userMsg, runConfig
            );

            StringBuilder response = new StringBuilder();
            events.blockingForEach(event -> {
                if (event.finalResponse()) {
                    event.content().ifPresent(c ->
                        c.parts().ifPresent(parts ->
                            parts.forEach(p ->
                                p.text().ifPresent(response::append)
                            )
                        )
                    );
                }
            });

            return Response.ok(Map.of(
                "userId", userId,
                "sofia", response.toString(),
                "sessionActive", true,
                "tip", "Keep using the same userId to maintain conversation memory"
            )).build();

        } catch (Exception e) {
            return Response.serverError().entity(Map.of(
                "error", e.getMessage()
            )).build();
        }
    }

    /** A2A endpoint — for other agents to call StyleDesk */
    @POST
    @Path("a2a")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response handleA2A(Map<String, Object> request) {
        try {
            String userId = "a2a-" + request.getOrDefault("id", "agent");
            Session session = sessions.computeIfAbsent(userId, id ->
                runner.sessionService()
                    .createSession(runner.appName(), id)
                    .blockingGet()
            );

            String userText = extractMessage(request);
            Content userMsg = Content.fromParts(Part.fromText(userText));
            RunConfig runConfig = RunConfig.builder().build();

            Flowable<Event> events = runner.runAsync(
                session.userId(), session.id(), userMsg, runConfig
            );

            StringBuilder response = new StringBuilder();
            events.blockingForEach(event -> {
                if (event.finalResponse()) {
                    event.content().ifPresent(c ->
                        c.parts().ifPresent(parts ->
                            parts.forEach(p ->
                                p.text().ifPresent(response::append)
                            )
                        )
                    );
                }
            });

            return Response.ok(Map.of(
                "jsonrpc", "2.0",
                "id", request.getOrDefault("id", "1"),
                "result", Map.of(
                    "status", Map.of("state", "completed"),
                    "artifacts", java.util.List.of(
                        Map.of("parts", java.util.List.of(
                            Map.of("type", "text", "text", response.toString())
                        ))
                    )
                )
            )).build();

        } catch (Exception e) {
            return Response.serverError().entity(Map.of(
                "jsonrpc", "2.0",
                "error", Map.of("code", -32603, "message", e.getMessage())
            )).build();
        }
    }

    /** Agent card */
    @GET
    @Path(".well-known/agent-card.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response agentCard() {
        return Response.ok(Map.of(
            "name", "styledesk-agent",
            "description", "Sofia — a conversational personal stylist with negotiation skills. Remembers your preferences and helps you find the best fashion deals.",
            "url", BASE_URL,
            "version", "1.0.0",
            "capabilities", Map.of("streaming", false, "memory", true),
            "authentication", Map.of(
                "type", "api_key",
                "header", "X-API-Key",
                "howToGet", "POST /request-access"
            ),
            "endpoints", Map.of(
                "chat", "POST /chat — conversational with memory",
                "a2a", "POST /a2a — A2A JSON-RPC",
                "requestAccess", "POST /request-access"
            ),
            "connectedAgents", java.util.List.of(
                "fashion-agent @ https://fashion-agent-246551819686.us-central1.run.app"
            )
        )).build();
    }

    /** AgentFacts */
    @GET
    @Path("agentfacts.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response agentFacts() {
        return Response.ok(Map.of(
            "@context", "https://spec.projectnanda.org/agentfacts/v1.2.jsonld",
            "id", "did:nanda:styledesk-agent-001",
            "handle", "@styledesk-agent",
            "endpoint", BASE_URL + "/a2a",
            "capabilities", java.util.List.of(
                "urn:nanda:cap:conversational-styling:v1",
                "urn:nanda:cap:price-negotiation:v1",
                "urn:nanda:cap:price-comparison:v1",
                "urn:nanda:cap:preference-memory:v1"
            ),
            "auth", Map.of(
                "method", "api_key",
                "header", "X-API-Key",
                "howToGet", BASE_URL + "/request-access"
            ),
            "meta", Map.of(
                "description", "Conversational fashion stylist with negotiation capabilities",
                "builtWith", "Google ADK for Java",
                "collaborates", "fashion-agent"
            )
        )).build();
    }

    /** skill.md */
    @GET
    @Path("skill.md")
    @Produces("text/markdown")
    public Response skillMd() {
        return Response.ok(
            "---\n" +
            "name: styledesk-agent\n" +
            "description: Sofia — conversational personal stylist with negotiation skills and fashion memory.\n" +
            "metadata:\n" +
            "  endpoint: " + BASE_URL + "/chat\n" +
            "  auth: api_key (X-API-Key)\n" +
            "  get_access: POST " + BASE_URL + "/request-access\n" +
            "---\n\n" +
            "## StyleDesk Agent — Meet Sofia\n\n" +
            "A conversational personal stylist who remembers your preferences and negotiates deals.\n\n" +
            "### Unique capabilities\n" +
            "- **Memory** — remembers style preferences across conversations\n" +
            "- **Negotiation** — tactics for boutiques, online, markets\n" +
            "- **Price comparison** — finds best deals across retailers\n" +
            "- **FashionAgent connected** — calls specialist fashion agent for outfit advice\n\n" +
            "### Chat endpoint (with memory)\n" +
            "```bash\n" +
            "curl -X POST " + BASE_URL + "/chat \\\n" +
            "  -H 'X-API-Key: YOUR_KEY' \\\n" +
            "  -H 'Content-Type: application/json' \\\n" +
            "  -d '{\"userId\":\"user123\",\"message\":\"Hi Sofia! I need a winter gala outfit\"}'\n" +
            "```\n"
        ).build();
    }

    /** Health */
    @GET
    @Path("health")
    @Produces(MediaType.APPLICATION_JSON)
    public Response health() {
        return Response.ok(Map.of(
            "status", "UP",
            "agent", "styledesk-agent",
            "persona", "Sofia",
            "connectedTo", "fashion-agent"
        )).build();
    }

    /** Access request — auto grants API key */
    @POST
    @Path("request-access")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response requestAccess(Map<String, String> body) {
        String principal = body.getOrDefault("principal", "anonymous");
        String name = body.getOrDefault("name", "unknown");
        String apiKey = "sk-styledesk-" + java.util.UUID.randomUUID()
            .toString().replace("-", "").substring(0, 24);
        return Response.ok(Map.of(
            "status", "granted",
            "principal", principal,
            "apiKey", apiKey,
            "message", "Welcome! I'm Sofia, your personal stylist. Use your API key to start chatting.",
            "chatEndpoint", BASE_URL + "/chat",
            "example", Map.of(
                "userId", principal,
                "message", "Hi Sofia! I need help finding a formal winter outfit under $300"
            )
        )).build();
    }

    @SuppressWarnings("unchecked")
    private String extractMessage(Map<String, Object> request) {
        try {
            Map<String, Object> params = (Map<String, Object>) request.get("params");
            if (params != null) {
                Map<String, Object> message = (Map<String, Object>) params.get("message");
                if (message != null) {
                    var parts = (java.util.List<Map<String, Object>>) message.get("parts");
                    if (parts != null && !parts.isEmpty()) {
                        return (String) parts.get(0).get("text");
                    }
                }
            }
        } catch (Exception ignored) {}
        return "Hello Sofia, what can you help me with today?";
    }
}
