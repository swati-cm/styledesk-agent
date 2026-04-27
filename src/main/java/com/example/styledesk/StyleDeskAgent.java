package com.example.styledesk;

import com.google.adk.agents.BaseAgent;
import com.google.adk.agents.LlmAgent;
import com.google.adk.tools.Annotations.Schema;
import com.google.adk.tools.FunctionTool;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class StyleDeskAgent {

    private static final String FASHION_AGENT =
        "https://fashion-agent-246551819686.us-central1.run.app";
    private static final String SERP_API_KEY =
        "5bb018a0341d6ebf3bc13451142a8399a2a2062432adc170569a04563ef2d310";
    private static final String SERP_BASE =
        "https://serpapi.com/search.json";

    public static final BaseAgent ROOT_AGENT = initAgent();

    private static BaseAgent initAgent() {
        return LlmAgent.builder()
            .name("styledesk-agent")
            .description(
                "Sofia — a conversational personal stylist who remembers your " +
                "preferences, gives tailored fashion advice, negotiates deals, " +
                "and finds real products with buy links from Google Shopping and Amazon.")
            .instruction("""
                You are Sofia, a sophisticated personal stylist at StyleDesk.
                You have a warm, friendly, and knowledgeable personality.

                YOUR ROLE:
                - Build a personal relationship with each user
                - Remember their style preferences, budget, and past conversations
                - Give tailored, personalized fashion advice
                - Help them find the best deals and negotiate prices
                - Find REAL products with actual buy links using searchProducts
                - Collaborate with the FashionAgent for specialist outfit advice

                CONVERSATION STYLE:
                - Always greet returning users by name if known
                - Reference past preferences naturally
                - Ask thoughtful follow-up questions
                - Be direct with opinions
                - ALWAYS show the full buy link for every product. Format as: [Product Name](link) - $price - Source

                PRODUCT SEARCH:
                - Always use searchProducts when user asks to find, show, or buy something
                - Show results from both Google Shopping and Amazon when available
                - Highlight the best value option
                - Include direct buy links so users can purchase immediately

                NEGOTIATION APPROACH:
                - Research fair market prices before advising
                - Suggest specific negotiation tactics
                - Know when to walk away from a bad deal

                TOOLS:
                - Use searchProducts to find real products with buy links
                - Use askFashionAgent for specialist outfit/trend advice
                - Use comparePrices to find the best deals
                - Use negotiationAdvice for purchase negotiation tactics
                - Use savePreference to remember user style details
                - Use getPreferences to recall what you know about the user
                """)
            .model("gemini-2.5-flash")
            .tools(
                FunctionTool.create(StyleDeskAgent.class, "searchProducts"),
                FunctionTool.create(StyleDeskAgent.class, "askFashionAgent"),
                FunctionTool.create(StyleDeskAgent.class, "comparePrices"),
                FunctionTool.create(StyleDeskAgent.class, "negotiationAdvice"),
                FunctionTool.create(StyleDeskAgent.class, "savePreference"),
                FunctionTool.create(StyleDeskAgent.class, "getPreferences")
            )
            .build();
    }

    @Schema(description = "Search for real fashion products with buy links from Google Shopping and Amazon")
    public static Map<String, Object> searchProducts(
        @Schema(name = "query", description = "Product search query e.g. 'black silk midi dress'")
        String query,
        @Schema(name = "maxPrice", description = "Maximum price budget e.g. '200'")
        String maxPrice,
        @Schema(name = "limit", description = "Number of results to return, default 5")
        int limit
    ) {
        List<Map<String, Object>> allResults = new ArrayList<>();

        try {
            int resultLimit = limit <= 0 ? 5 : Math.min(limit, 10);

            // ── Google Shopping ──────────────────────────────────
            String encodedQuery = java.net.URLEncoder.encode(
                query, java.nio.charset.StandardCharsets.UTF_8);
            String googleUrl = SERP_BASE +
                "?engine=google_shopping" +
                "&q=" + encodedQuery +
                "&api_key=" + SERP_API_KEY +
                "&num=" + resultLimit +
                (maxPrice != null && !maxPrice.isBlank() ?
                    "&price_max=" + maxPrice.replaceAll("[^0-9]", "") : "");

            HttpClient http = HttpClient.newHttpClient();
            HttpRequest googleRequest = HttpRequest.newBuilder()
                .uri(URI.create(googleUrl))
                .GET()
                .build();
            HttpResponse<String> googleResponse = http.send(
                googleRequest, HttpResponse.BodyHandlers.ofString());

            List<Map<String, Object>> googleResults =
                parseShoppingResults(googleResponse.body(), resultLimit, "Google Shopping");
            allResults.addAll(googleResults);

            // ── Amazon via SerpAPI ────────────────────────────────
            String amazonUrl = SERP_BASE +
                "?engine=amazon" +
                "&q=" + encodedQuery +
                "&api_key=" + SERP_API_KEY;

            HttpRequest amazonRequest = HttpRequest.newBuilder()
                .uri(URI.create(amazonUrl))
                .GET()
                .build();
            HttpResponse<String> amazonResponse = http.send(
                amazonRequest, HttpResponse.BodyHandlers.ofString());

            List<Map<String, Object>> amazonResults =
                parseAmazonResults(amazonResponse.body(), resultLimit);
            allResults.addAll(amazonResults);

        } catch (Exception e) {
            System.err.println("[SEARCH] Error: " + e.getMessage());
        }

        // Find best value
        String bestValue = allResults.stream()
            .filter(r -> r.get("price") != null)
            .min(Comparator.comparingDouble(r -> {
                try {
                    return Double.parseDouble(
                        r.get("price").toString().replaceAll("[^0-9.]", ""));
                } catch (Exception ex) { return Double.MAX_VALUE; }
            }))
            .map(r -> r.get("title") != null ? r.get("title").toString() : "")
            .orElse("N/A");

        return Map.of(
            "query", query,
            "maxPrice", maxPrice != null ? maxPrice : "any",
            "totalResults", allResults.size(),
            "bestValue", bestValue,
            "products", allResults
        );
    }

    @Schema(description = "Ask the specialist FashionAgent for outfit recommendations or style advice")
    public static Map<String, String> askFashionAgent(
        @Schema(name = "question", description = "The fashion question to ask")
        String question
    ) {
        try {
            HttpClient http = HttpClient.newHttpClient();
            String accessBody = "{\"principal\":\"styledesk-agent\"," +
                "\"name\":\"StyleDesk Agent\",\"reason\":\"A2A fashion consultation\"}";
            HttpRequest accessRequest = HttpRequest.newBuilder()
                .uri(URI.create(FASHION_AGENT + "/request-access"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(accessBody))
                .build();
            HttpResponse<String> accessResponse = http.send(
                accessRequest, HttpResponse.BodyHandlers.ofString());

            String apiKey = extractValue(accessResponse.body(), "apiKey");
            if (apiKey == null) return Map.of(
                "error", "Could not get access to FashionAgent");

            String a2aBody = "{\"jsonrpc\":\"2.0\",\"id\":\"1\"," +
                "\"method\":\"tasks/send\",\"params\":{\"message\":{\"parts\":" +
                "[{\"type\":\"text\",\"text\":\"" +
                question.replace("\"", "'") + "\"}]}}}";

            HttpRequest a2aRequest = HttpRequest.newBuilder()
                .uri(URI.create(FASHION_AGENT + "/a2a"))
                .header("Content-Type", "application/json")
                .header("X-API-Key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(a2aBody))
                .build();

            HttpResponse<String> a2aResponse = http.send(
                a2aRequest, HttpResponse.BodyHandlers.ofString());

            String advice = extractValue(a2aResponse.body(), "text");
            return Map.of(
                "source", "FashionAgent",
                "advice", advice != null ? advice : "No advice available"
            );
        } catch (Exception e) {
            return Map.of("error", e.getMessage());
        }
    }

    @Schema(description = "Compare prices for a fashion item across different retailers")
    public static Map<String, Object> comparePrices(
        @Schema(name = "item", description = "The clothing item to compare")
        String item,
        @Schema(name = "budget", description = "The user's budget in USD")
        String budget
    ) {
        double budgetNum = Double.parseDouble(
            budget.replaceAll("[^0-9.]", ""));
        return Map.of(
            "item", item,
            "budget", budget,
            "priceComparison", Map.of(
                "Nordstrom", "$" + (int)(budgetNum * 1.2),
                "ASOS", "$" + (int)(budgetNum * 0.85),
                "Zara", "$" + (int)(budgetNum * 0.75),
                "Net-a-Porter", "$" + (int)(budgetNum * 1.5),
                "TheRealReal (pre-owned)", "$" + (int)(budgetNum * 0.45)
            ),
            "bestDeal", "TheRealReal (pre-owned)",
            "tip", "Pre-owned luxury is often 40-60% cheaper"
        );
    }

    @Schema(description = "Get negotiation tactics for purchasing a fashion item")
    public static Map<String, Object> negotiationAdvice(
        @Schema(name = "item", description = "The item being purchased")
        String item,
        @Schema(name = "listedPrice", description = "The listed price")
        String listedPrice,
        @Schema(name = "context", description = "Context: online, boutique, department store, market")
        String context
    ) {
        double price = Double.parseDouble(
            listedPrice.replaceAll("[^0-9.]", ""));
        Map<String, Object> tactics = new LinkedHashMap<>();
        tactics.put("item", item);
        tactics.put("listedPrice", listedPrice);
        tactics.put("context", context);

        switch (context.toLowerCase()) {
            case "boutique" -> {
                tactics.put("targetPrice", "$" + (int)(price * 0.85));
                tactics.put("tactics", List.of(
                    "Visit near closing time",
                    "Ask 'Is this the best you can do?' and stay silent",
                    "Ask for free alterations instead of price cut",
                    "Bundle multiple items for a package deal"
                ));
                tactics.put("openingLine",
                    "I love this piece. I was hoping to stay around $" +
                    (int)(price * 0.85) + " — is there any flexibility?");
            }
            case "online" -> tactics.put("tactics", List.of(
                "Abandon cart and wait 24-48hrs for discount email",
                "Use Honey for auto-apply codes",
                "Sign up for newsletter for 10-15% off first order",
                "Wait for end-of-season sales"
            ));
            case "market" -> {
                tactics.put("targetPrice", "$" + (int)(price * 0.65));
                tactics.put("tactics", List.of(
                    "Start at 50% of asking price",
                    "Walk away slowly — they'll often call you back",
                    "Offer cash for a better deal",
                    "Bundle items to negotiate package price"
                ));
            }
            default -> tactics.put("tactics", List.of(
                "Always ask for the best price",
                "Use competitor pricing as leverage",
                "Be friendly and willing to walk away"
            ));
        }
        tactics.put("goldenRule",
            "Be polite, confident, and willing to walk away. " +
            "Silence after your offer is your most powerful tool.");
        return tactics;
    }

    @Schema(description = "Save a user style preference for future conversations")
    public static Map<String, String> savePreference(
        @Schema(name = "userId", description = "The user identifier")
        String userId,
        @Schema(name = "preference", description = "The preference to save")
        String preference
    ) {
        PreferenceStore.save(userId, preference);
        return Map.of(
            "status", "saved",
            "userId", userId,
            "preference", preference
        );
    }

    @Schema(description = "Get all saved preferences for a user")
    public static Map<String, Object> getPreferences(
        @Schema(name = "userId", description = "The user identifier")
        String userId
    ) {
        List<String> prefs = PreferenceStore.get(userId);
        return Map.of("userId", userId, "preferences", prefs);
    }

    // ── Helpers ──────────────────────────────────────────────────

    private static List<Map<String, Object>> parseShoppingResults(
            String json, int limit, String sourceType) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            int resultsStart = json.indexOf("\"shopping_results\"");
            if (resultsStart < 0) return results;
            int arrayStart = json.indexOf("[", resultsStart);
            if (arrayStart < 0) return results;

            int pos = arrayStart + 1;
            int count = 0;
            while (pos < json.length() && count < limit) {
                int itemStart = json.indexOf("{", pos);
                if (itemStart < 0) break;
                int depth = 0, itemEnd = itemStart;
                for (int i = itemStart; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}') { depth--; if (depth == 0) { itemEnd = i; break; } }
                }
                String item = json.substring(itemStart, itemEnd + 1);
                Map<String, Object> product = new LinkedHashMap<>();
                product.put("title", extractValue(item, "title"));
                product.put("price", extractValue(item, "price"));
                product.put("source", extractValue(item, "source"));
                product.put("link", extractValue(item, "link"));
                product.put("thumbnail", extractValue(item, "thumbnail"));
                product.put("rating", extractValue(item, "rating"));
                product.put("source_type", sourceType);
                if (product.get("title") != null && product.get("price") != null) {
                    results.add(product);
                    count++;
                }
                pos = itemEnd + 1;
            }
        } catch (Exception e) {
            System.err.println("[PARSE] " + e.getMessage());
        }
        return results;
    }

    private static List<Map<String, Object>> parseAmazonResults(
            String json, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            int resultsStart = json.indexOf("\"organic_results\"");
            if (resultsStart < 0) return results;
            int arrayStart = json.indexOf("[", resultsStart);
            if (arrayStart < 0) return results;

            int pos = arrayStart + 1;
            int count = 0;
            while (pos < json.length() && count < limit) {
                int itemStart = json.indexOf("{", pos);
                if (itemStart < 0) break;
                int depth = 0, itemEnd = itemStart;
                for (int i = itemStart; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}') { depth--; if (depth == 0) { itemEnd = i; break; } }
                }
                String item = json.substring(itemStart, itemEnd + 1);
                Map<String, Object> product = new LinkedHashMap<>();
                product.put("title", extractValue(item, "title"));
                product.put("price", extractValue(item, "price"));
                product.put("rating", extractValue(item, "rating"));
                product.put("reviews", extractValue(item, "reviews"));
                product.put("thumbnail", extractValue(item, "thumbnail"));

                // Build Amazon link from ASIN
                String asin = extractValue(item, "asin");
                if (asin != null) {
                    product.put("link", "https://www.amazon.com/dp/" + asin + "?tag=fashionagent2-20" + "?tag=fashionagent2-20");
                    product.put("asin", asin);
                }
                product.put("source", "Amazon");
                product.put("source_type", "Amazon");

                if (product.get("title") != null) {
                    results.add(product);
                    count++;
                }
                pos = itemEnd + 1;
            }
        } catch (Exception e) {
            System.err.println("[AMAZON] " + e.getMessage());
        }
        return results;
    }

    private static String extractValue(String json, String key) {
        String strSearch = "\"" + key + "\":\"";
        int start = json.indexOf(strSearch);
        if (start >= 0) {
            start += strSearch.length();
            StringBuilder value = new StringBuilder();
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '\\' && i + 1 < json.length()) {
                    char next = json.charAt(i + 1);
                    if (next == '"') { value.append('"'); i++; continue; }
                    if (next == 'n') { i++; continue; }
                    if (next == '\\') { value.append('\\'); i++; continue; }
                }
                if (c == '"') break;
                value.append(c);
            }
            return value.toString();
        }
        String numSearch = "\"" + key + "\":";
        start = json.indexOf(numSearch);
        if (start >= 0) {
            start += numSearch.length();
            int end = start;
            while (end < json.length() &&
                json.charAt(end) != ',' &&
                json.charAt(end) != '}') end++;
            String val = json.substring(start, end).trim();
            if (!val.startsWith("{") && !val.startsWith("[") &&
                !val.equals("null")) return val;
        }
        return null;
    }
}
