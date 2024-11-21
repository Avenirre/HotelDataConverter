package com.freshcells.converter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshcells.converter.enums.FileType;
import com.freshcells.converter.exceptions.HotelFileProcessingException;
import com.freshcells.converter.model.HotelData;
import com.freshcells.converter.model.ProcessingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class HotelConverterService {
    private final FileProcessingService fileProcessingService;
    private final FileSystemService fileSystemService;
    private final ObjectMapper objectMapper;

    private static final Pattern IMAGE_URL_PATTERN =
            Pattern.compile(".*\\.(jpg|jpeg|png|gif)$", Pattern.CASE_INSENSITIVE);

    public ProcessingResult processFiles(List<MultipartFile> files) {
        LocalDateTime timestamp = LocalDateTime.now();
        Path outputPath = fileSystemService.getOutputPath(timestamp);
        Path imagesDir = outputPath.resolve("images");

        Map<String, HotelData> hotels = new HashMap<>();
        List<CompletableFuture<Void>> imageDownloads = new ArrayList<>();
        int totalImages = 0;

        // Process files
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            if (filename == null) continue;

            String hotelId = extractHotelId(filename);
            Map<String, Object> content = fileProcessingService.processFile(file);

            // Update hotel data
            HotelData hotelData = hotels.computeIfAbsent(hotelId, HotelData::empty);
            hotels.put(hotelId, switch(FileType.fromFilename(filename)) {
                case GIATA -> hotelData.withGiata(content);
                case COA -> hotelData.withCoa(content);
            });

            // Extract and download images
            Set<String> imageUrls = extractImageUrls(content);
            totalImages += imageUrls.size();

            imageUrls.forEach(url ->
                    imageDownloads.add(fileSystemService.downloadImage(url, hotelId, imagesDir))
            );
        }

        try {
            // Wait for image downloads
            CompletableFuture.allOf(imageDownloads.toArray(CompletableFuture[]::new))
                    .get(5, TimeUnit.MINUTES);

            // Save result
            fileSystemService.saveJsonResult(objectMapper.writeValueAsBytes(hotels), outputPath);

            return new ProcessingResult(
                    outputPath.resolve("hotels.json"),
                    imagesDir,
                    timestamp,
                    files.size(),
                    totalImages
            );

        } catch (Exception e) {
            throw new HotelFileProcessingException("Failed to complete processing", e);
        }
    }

    private String extractHotelId(String filename) {
        return filename.split("-")[0];
    }

    private Set<String> extractImageUrls(Map<String, Object> content) {
        Set<String> urls = new HashSet<>();
        extractUrls(content, urls);
        return urls;
    }

    private void extractUrls(Object obj, Set<String> urls) {
        if (obj instanceof Map<?,?> map) {
            map.forEach((k, v) -> {
                if (k instanceof String key && key.equals("url")
                        && v instanceof String url
                        && IMAGE_URL_PATTERN.matcher(url).matches()) {
                    urls.add(url);
                } else {
                    extractUrls(v, urls);
                }
            });
        } else if (obj instanceof Collection<?> collection) {
            collection.forEach(item -> extractUrls(item, urls));
        }
    }
}
