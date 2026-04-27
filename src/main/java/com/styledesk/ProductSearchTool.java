package com.styledesk;

import jakarta.enterprise.context.ApplicationScoped;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;

@ApplicationScoped
public class ProductSearchTool {

    private static final String SERP_API_KEY =
        "5bb018a0341d6ebf3bc13451142a8399a2a2062432adc170569a04563ef2d310";
    private static final String SERP_BASE =
        "https://serpapi.com/search.json";

    private final HttpClient http = HttpClient.newHttpClient();

    public List<Map<String, Object>> searchGoogleShopping(
            String query, String maxPrice, int limit) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = SERP_BASE +
                "?engine=google_shopping" +
                "&q=" + encodedQuery +
                "&api_key=" + SERP_API_KEY +
                "&num=" + limit +
                (maxPrice != null ? "&price_max=" +
                    maxPrice.replaceAll("[^0-9]", "") : "");

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

            HttpResponse<String> response = http.send(
                request, HttpResponse.BodyHandlers.ofString());

            return parseShoppingResults(response.body(), limit);

        } catch (Exception e) {
            System.err.println("[SERP] Error: " + e.getMessage());
            return List.of(Map.of("error", e.getMessage()));
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseShoppingResults(
            String json, int limit) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            // Parse shopping_results array
            int resultsStart = json.indexOf("\"shopping_results\"");
            if (resultsStart < 0) return results;

            int arrayStart = json.indexOf("[", resultsStart);
            if (arrayStart < 0) return results;

            // Extract each product item
            int pos = arrayStart + 1;
            int count = 0;

            while (pos < json.length() && count < limit) {
                int itemStart = json.indexOf("{", pos);
                if (itemStart < 0) break;

                // Find matching closing brace
                int depth = 0;
                int itemEnd = itemStart;
                for (int i = itemStart; i < json.length(); i++) {
                    char c = json.charAt(i);
                    if (c == '{') depth++;
                    else if (c == '}') {
                        depth--;
                        if (depth == 0) { itemEnd = i; break; }
                    }
                }

                String item = json.substring(itemStart, itemEnd + 1);
                Map<String, Object> product = new LinkedHashMap<>();

                product.put("title", extractValue(item, "title"));
                product.put("price", extractValue(item, "price"));
                product.put("source", extractValue(item, "source"));
                product.put("link", extractValue(item, "link"));
                product.put("thumbnail", extractValue(item, "thumbnail"));
                product.put("rating", extractValue(item, "rating"));
                product.put("reviews", extractValue(item, "reviews"));
                product.put("badge", extractValue(item, "badge"));

                // Only add if has title and price
                if (product.get("title") != null &&
                    product.get("price") != null) {
                    product.put("source_type", "Google Shopping");
                    results.add(product);
                    count++;
                }

                pos = itemEnd + 1;
            }

        } catch (Exception e) {
            System.err.println("[SERP] Parse error: " + e.getMessage());
        }
        return results;
    }

    private String extractValue(String json, String key) {
        // Handle string values
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
                    if (next == 'n') { value.append('\n'); i++; continue; }
                    if (next == '\\') { value.append('\\'); i++; continue; }
                }
                if (c == '"') break;
                value.append(c);
            }
            return value.toString();
        }

        // Handle numeric values
        String numSearch = "\"" + key + "\":";
        start = json.indexOf(numSearch);
        if (start >= 0) {
            start += numSearch.length();
            int end = start;
            while (end < json.length() &&
                json.charAt(end) != ',' &&
                json.charAt(end) != '}') end++;
            String val = json.substring(start, end).trim();
            if (!val.startsWith("{") && !val.startsWith("[")) return val;
        }

        return null;
    }
}