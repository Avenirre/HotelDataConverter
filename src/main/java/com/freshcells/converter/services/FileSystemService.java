package com.freshcells.converter.services;

import com.freshcells.converter.config.AppProperties;
import com.freshcells.converter.exceptions.HotelFileSystemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileSystemService {
    private final AppProperties appProperties;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public Path getOutputPath(LocalDateTime timestamp) {
        try {
            //create Path from outputDir (application.yaml)
            Path outputPath = Path.of(appProperties.outputDir())
                    //add formatted timestamp to the base path ('/home/user/output/20240120_153045')
                    .resolve(timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")))
                    //remove redundant separators
                    .normalize();
            //create directories
            Files.createDirectories(outputPath);
            return outputPath;
        } catch (IOException e) {
            throw new HotelFileSystemException("Failed to create output directory", e);
        }
    }

    public void saveJsonResult(byte[] data, Path outputPath) {
        try {
            Path filePath = outputPath.resolve("hotels.json");
            Files.write(filePath, data);
            log.info("Saved result to: {}", filePath);
        } catch (IOException e) {
            throw new HotelFileSystemException("Failed to save JSON result", e);
        }
    }

    public CompletableFuture<Void> downloadImage(String url, String hotelId, Path imagesDir) {
        return CompletableFuture.runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .GET()
                        .build();

                Path imagePath = imagesDir.resolve(generateImageFilename(hotelId, url));

                HttpResponse<byte[]> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    Files.createDirectories(imagePath.getParent());
                    Files.write(imagePath, response.body());
                    log.debug("Downloaded image: {}", imagePath);
                } else {
                    log.warn("Failed to download image {}: {}", url, response.statusCode());
                }
            } catch (Exception e) {
                log.error("Failed to download image: " + url, e);
            }
        });
    }

    private String generateImageFilename(String hotelId, String url) {
        String extension = getFileExtension(url);
        return String.format("%s_%s.%s", hotelId, UUID.randomUUID(), extension);
    }

    private String getFileExtension(String url) {
        String filename = URI.create(url).getPath();
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "jpg";
    }
}
