package com.freshcells.converter.services;

import com.freshcells.converter.config.AppProperties;
import com.freshcells.converter.exceptions.HotelFileSystemException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
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

    public CompletableFuture<Boolean> downloadImage(String url, String hotelId, Path imagesDir) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.debug("Starting download of image from URL: {}", url);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(30))
                        .header("User-Agent", "Mozilla/5.0")
                        .GET()
                        .build();

                HttpResponse<byte[]> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    try (InputStream is = new ByteArrayInputStream(response.body())) {
                        BufferedImage image = ImageIO.read(is);
                        if (image == null) {
                            log.warn("Downloaded file is not a valid image: {}", url);
                            return false;
                        }

                        if (image.getWidth() == 0 || image.getHeight() == 0) {
                            log.warn("Image has invalid dimensions: {}", url);
                            return false;
                        }

                        Path imagePath = imagesDir.resolve(generateImageFilename(hotelId, url));
                        Files.createDirectories(imagePath.getParent());
                        Files.write(imagePath, response.body());
                        log.debug("Successfully downloaded and verified image from {} to {}", url, imagePath);
                        return true;
                    }
                } else {
                    log.warn("Failed to download image from {}, status code: {}",
                            url, response.statusCode());
                    return false;
                }
            } catch (Exception e) {
                log.error("Error processing image from {}: {}", url, e.getMessage());
                return false;
            }
        });
    }

    private String generateImageFilename(String hotelId, String url) {
        String extension = getFileExtension(url);
        return String.format("%s_%s.%s", hotelId, UUID.randomUUID(), extension);
    }

    private String getFileExtension(String url) {
        try {
            String path = new URI(url).getPath();
            int lastDot = path.lastIndexOf('.');
            return lastDot > 0 ? path.substring(lastDot + 1) : "jpg";
        } catch (URISyntaxException e) {
            return "jpg";
        }
    }
}
