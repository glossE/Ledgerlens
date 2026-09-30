package com.ledgerlens.ledgerlens.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerlens.ledgerlens.model.Transaction;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

@Service
public class GeminiService {

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    private static final String CATEGORIES =
            "Salary, Vendor Payment, Subscription, Tax, Transfer, Other";

    public List<Transaction> categorize(List<Transaction> transactions) throws Exception {
        if (transactions.isEmpty()) return transactions;

        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a financial transaction categorizer for Indian businesses.\n");
        prompt.append("Categorize each transaction description below into exactly one of these categories: ")
              .append(CATEGORIES).append(".\n");
        prompt.append("Respond with ONLY a numbered list matching the input order. Example:\n");
        prompt.append("1. Salary\n2. Vendor Payment\n\n");
        prompt.append("Transactions:\n");

        for (int i = 0; i < transactions.size(); i++) {
            prompt.append(i + 1).append(". ").append(transactions.get(i).getDescription()).append("\n");
        }

        // Build Gemini API request: {"contents":[{"parts":[{"text":"..."}]}]}
        com.fasterxml.jackson.databind.node.ObjectNode requestNode = objectMapper.createObjectNode();
        requestNode.putArray("contents")
                .addObject()
                .putArray("parts")
                .addObject()
                .put("text", prompt.toString());
        String requestBody = objectMapper.writeValueAsString(requestNode);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GEMINI_URL + geminiApiKey))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Gemini API error " + response.statusCode() + ": " + response.body());
            return keywordFallback(transactions);
        }

        String[] lines = parseGeminiResponse(response.body());

        for (int i = 0; i < transactions.size(); i++) {
            String category = (i < lines.length) ? lines[i].trim() : "Other";
            transactions.get(i).setCategory(sanitizeCategory(category));
        }

        return transactions;
    }

    private String[] parseGeminiResponse(String responseBody) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        String text = root
                .path("candidates").get(0)
                .path("content")
                .path("parts").get(0)
                .path("text").asText();

        // Strip leading "1. ", "2. " etc from each line
        return text.lines()
                .filter(l -> !l.isBlank())
                .map(l -> l.replaceFirst("^\\d+\\.\\s*", ""))
                .toArray(String[]::new);
    }

    private String sanitizeCategory(String raw) {
        return List.of("Salary", "Vendor Payment", "Subscription", "Tax", "Transfer")
                .stream()
                .filter(c -> raw.equalsIgnoreCase(c))
                .findFirst()
                .orElse("Other");
    }

    private List<Transaction> keywordFallback(List<Transaction> transactions) {
        for (Transaction tx : transactions) {
            tx.setCategory(classifyByKeyword(tx.getDescription().toLowerCase()));
        }
        return transactions;
    }

    private String classifyByKeyword(String desc) {
        if (desc.contains("salary") || desc.contains("payroll") || desc.contains("wages")) return "Salary";
        if (desc.contains("vendor") || desc.contains("invoice") || desc.contains("purchase")) return "Vendor Payment";
        if (desc.contains("subscription") || desc.contains("netflix") || desc.contains("spotify") || desc.contains("saas")) return "Subscription";
        if (desc.contains("tax") || desc.contains("tds") || desc.contains("gst") || desc.contains("duty")) return "Tax";
        if (desc.contains("transfer") || desc.contains("neft") || desc.contains("rtgs") || desc.contains("imps")) return "Transfer";
        if (desc.contains("aws") || desc.contains("cloud") || desc.contains("hosting")) return "Subscription";
        return "Other";
    }

}
