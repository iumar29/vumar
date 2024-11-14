package com.Jhipster.Jhipster_project.Controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.Jhipster.Jhipster_project.Model.JdlRequest;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.*;
import java.util.Base64;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@RestController
public class JhipsterController {

    private static final Logger logger = LoggerFactory.getLogger(JhipsterController.class);

    @PostMapping("/generate-project")
    public ResponseEntity<String> generateJHipsterProject(@RequestBody JdlRequest jdlRequest,
                                                          @RequestParam String projectPath,
                                                          @RequestParam String githubUsername,
                                                          @RequestParam String githubToken) {
        try {
            logger.info("Starting project generation for baseName: {}", jdlRequest.getBaseName());

            String appBaseName = jdlRequest.getBaseName();
            String newProjectPath = projectPath + File.separator + appBaseName;
            Files.createDirectories(Paths.get(newProjectPath));

            // Download the generated JHipster project ZIP
            String zipFilePath = newProjectPath + File.separator + "sample.zip";
            downloadJHipsterProject(jdlRequest, zipFilePath);

            // Unzip the downloaded project
            unzipProject(zipFilePath, newProjectPath);

            // Create GitHub repository and push the unzipped project to GitHub
            String repoUrl = createGitHubRepo(githubUsername, githubToken, appBaseName);
            if (repoUrl != null) {
                initGitAndPushToGitHub(newProjectPath, repoUrl, githubUsername, githubToken);
                logger.info("Project generated, unzipped, and pushed to GitHub successfully.");
                return ResponseEntity.ok("Project generated and pushed to GitHub successfully: " + repoUrl);
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Project generated, but failed to create GitHub repository.");
            }
        } catch (IOException | InterruptedException e) {
            logger.error("An error occurred: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred: " + e.getMessage());
        }
    }

    private void downloadJHipsterProject(JdlRequest jdlRequest, String zipFilePath) throws IOException {
        String url = "https://start.jhipster.tech/api/download-application";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String jsonPayload = "{\"generator-jhipster\":" + jdlRequest.toJsonString() + "}";
        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonPayload.getBytes());
        }

        if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
            try (InputStream in = connection.getInputStream();
                 FileOutputStream out = new FileOutputStream(zipFilePath)) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            logger.info("Downloaded JHipster project ZIP to {}", zipFilePath);
        } else {
            throw new IOException("Failed to download JHipster project. HTTP response code: " + connection.getResponseCode());
        }
    }

    private void unzipProject(String zipFilePath, String destDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                File file = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    new File(file.getParent()).mkdirs();
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = zipIn.read(buffer)) > 0) {
                            fos.write(buffer, 0, length);
                        }
                    }
                }
                zipIn.closeEntry();
            }
        }
        Files.deleteIfExists(Paths.get(zipFilePath)); // Delete the ZIP file after extraction
        logger.info("Unzipped JHipster project to {}", destDir);
    }

    private String createGitHubRepo(String username, String token, String repoName) throws IOException {
        String apiUrl = "https://api.github.com/user/repos";
        String jsonPayload = "{\"name\":\"" + repoName + "\", \"private\":false}";

        HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + token).getBytes()));
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonPayload.getBytes("utf-8"));
        }

        if (connection.getResponseCode() == HttpURLConnection.HTTP_CREATED) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String responseBody = br.lines().reduce("", (acc, line) -> acc + line.trim());
                return responseBody.split("\"clone_url\":\"")[1].split("\"")[0];
            }
        } else {
            logger.error("Failed to create GitHub repository. Response code: {}", connection.getResponseCode());
            return null;
        }
    }

    private void initGitAndPushToGitHub(String projectPath, String repoUrl, String username, String token) throws IOException, InterruptedException {
        runCommand(new File(projectPath), "git", "init");
        runCommand(new File(projectPath), "git", "add", ".");
        runCommand(new File(projectPath), "git", "commit", "-m", "Initial commit");
        runCommand(new File(projectPath), "git", "branch", "-M", "main");
        String remoteUrlWithAuth = repoUrl.replace("https://", "https://" + username + ":" + token + "@");
        runCommand(new File(projectPath), "git", "remote", "add", "origin", remoteUrlWithAuth);
        runCommand(new File(projectPath), "git", "push", "-u", "origin", "main");
    }

    private void runCommand(File workingDir, String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(workingDir);
        Process process = processBuilder.start();
        process.waitFor();
    }
}
