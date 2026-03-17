package com.caffeinesoft.github.client;

import org.json.JSONObject;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;

public class GitHubReleaseClient {
    private final HttpClient httpClient;
    private final String githubToken;
    private final String repository;

    public GitHubReleaseClient(String githubToken, String repository) {
        this.githubToken = githubToken;
        this.repository = repository;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
    }

    public JSONObject createRelease(String tag, String name, String body, boolean draft, boolean prerelease) throws Exception {
        JSONObject payload = new JSONObject()
                .put("tag_name", tag)
                .put("name", name != null && !name.isBlank() ? name : tag)
                .put("body", body != null ? body : "")
                .put("draft", draft)
                .put("prerelease", prerelease)
                .put("generate_release_notes", body == null || body.isBlank()); // Auto-generate if empty

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/repos/" + repository + "/releases"))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .header("X-GitHub-Api-Version", "2022-11-28")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201 && response.statusCode() != 200) {
            throw new RuntimeException("Failed to create release: " + response.body());
        }
        return new JSONObject(response.body());
    }

    public void uploadArtifact(String uploadUrlTemplate, Path file) throws Exception {
        String cleanUrl = uploadUrlTemplate.split("\\{")[0];
        String finalUrl = cleanUrl + "?name=" + file.getFileName().toString();

        System.out.println("Uploading asset: " + file.getFileName());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(finalUrl))
                .header("Authorization", "Bearer " + githubToken)
                .header("Content-Type", "application/octet-stream")
                .POST(HttpRequest.BodyPublishers.ofFile(file))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 201) {
            System.err.println("Warning: Failed to upload " + file.getFileName() + " -> " + response.body());
        } else {
            System.out.println("Successfully uploaded " + file.getFileName());
        }
    }
}