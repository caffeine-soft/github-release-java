package com.caffeinesoft.github.release;

import com.caffeinesoft.github.client.GitHubReleaseClient;
import org.json.JSONObject;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Main {
    public static void main(String[] args) {
        try {
            runAction();
        } catch (Exception e) {
            System.err.println("::error::Action failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void runAction() throws Exception {
        String token = getInput("GITHUB_TOKEN");
        String repository = System.getenv("GITHUB_REPOSITORY");
        String tag = getInput("TAG");
        String name = getInput("NAME");
        String body = getInput("BODY");
        boolean draft = Boolean.parseBoolean(getInput("DRAFT"));
        boolean prerelease = Boolean.parseBoolean(getInput("PRERELEASE"));
        String artifacts = getInput("ARTIFACTS");

        if (tag.isBlank()) throw new IllegalArgumentException("Tag is required.");

        GitHubReleaseClient client = new GitHubReleaseClient(token, repository);

        System.out.println("Creating release for tag: " + tag);
        JSONObject release = client.createRelease(tag, name, body, draft, prerelease);

        String uploadUrl = release.getString("upload_url");

        System.out.println("::set-output name=release_id::" + release.getLong("id"));
        System.out.println("::set-output name=release_url::" + release.getString("html_url"));

        if (!artifacts.isBlank()) {
            List<Path> filesToUpload = resolveArtifacts(artifacts);
            for (Path file : filesToUpload) {
                client.uploadArtifact(uploadUrl, file);
            }
        }
    }

    private static List<Path> resolveArtifacts(String artifactsInput) throws IOException {
        List<Path> matchedFiles = new ArrayList<>();
        Path baseDir = Paths.get(".");

        for (String pattern : artifactsInput.split(",")) {
            pattern = pattern.trim();
            if (pattern.isEmpty()) continue;

            String globPattern = pattern.contains("*") ? "glob:" + pattern : "glob:**/" + pattern;
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(globPattern);

            Files.walk(baseDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> matcher.matches(baseDir.relativize(p)))
                    .forEach(matchedFiles::add);
        }
        return matchedFiles;
    }

    private static String getInput(String name) {
        return Optional.ofNullable(System.getenv("INPUT_" + name)).orElse("");
    }
}
