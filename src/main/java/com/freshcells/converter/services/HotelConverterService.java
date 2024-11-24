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
    private static final String IMAGE_TAG_NAME = "image";
    private static final String URL_TAG_NAME = "url";
    private static final String IMAGES_FOLDER_NAME = "images";
    private static final String RESULT_FILE_NAME = "hotels.json";

    private Set<String> extractAllImageUrls(Object obj, Set<String> urls) {
        if (obj instanceof Map<?,?> map) {
            if (map.containsKey(IMAGE_TAG_NAME) && map.get(IMAGE_TAG_NAME) instanceof List<?> images) {
                images.forEach(img -> {
                    if (img instanceof Map<?,?> imageMap) {
                        Object urlObj = imageMap.get(URL_TAG_NAME);
                        if (urlObj instanceof String url) {
                            urls.add(url);
                        }
                    }
                });
            }

            map.values().forEach(value -> extractAllImageUrls(value, urls));
        } else if (obj instanceof Collection<?> collection) {
            collection.forEach(item -> extractAllImageUrls(item, urls));
        } else if (obj instanceof String str && IMAGE_URL_PATTERN.matcher(str).matches()) {
            urls.add(str);
        }
        return urls;
    }

    public ProcessingResult processFiles(List<MultipartFile> files) {
        LocalDateTime timestamp = LocalDateTime.now();
        Path outputPath = fileSystemService.getOutputPath(timestamp);
        Path imagesDir = outputPath.resolve(IMAGES_FOLDER_NAME);

        Map<String, HotelData> hotels = new HashMap<>();
        List<CompletableFuture<Boolean>> imageDownloads = new ArrayList<>();

        //process files
        for (MultipartFile file : files) {
            String filename = file.getOriginalFilename();
            if (filename == null) continue;

            String hotelId = filename.split("-")[0];
            Map<String, Object> content = fileProcessingService.processFile(file);

            //update hotel data
            HotelData hotelData = hotels.computeIfAbsent(hotelId, k -> HotelData.empty());
            hotels.put(hotelId, switch(FileType.fromFilename(filename)) {
                case GIATA -> hotelData.withGiata(content);
                case COA -> hotelData.withCoa(content);
            });

            //extract and download images
            Set<String> imageUrls = extractAllImageUrls(content, new HashSet<>());
            imageUrls.forEach(url ->
                    imageDownloads.add(fileSystemService.downloadImage(url, hotelId, imagesDir))
            );
        }

        try {
            //wait for all downloads to complete and count successful ones
            CompletableFuture.allOf(imageDownloads.toArray(CompletableFuture[]::new))
                    .get(5, TimeUnit.MINUTES);

            long successfulDownloads = imageDownloads.stream()
                    .map(future -> {
                        try {
                            return future.get();
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .filter(success -> success)
                    .count();

            //save result
            fileSystemService.saveJsonResult(objectMapper.writeValueAsBytes(hotels), outputPath);

            return new ProcessingResult(
                    outputPath.resolve(RESULT_FILE_NAME),
                    imagesDir,
                    timestamp,
                    files.size(),
                    (int) successfulDownloads
            );

        } catch (Exception e) {
            throw new HotelFileProcessingException("Failed to complete processing", e);
        }
    }
}
