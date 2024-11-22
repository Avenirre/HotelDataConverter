package com.freshcells.converter.services;

import com.freshcells.converter.config.AppProperties;
import com.freshcells.converter.exceptions.HotelFileSystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileSystemServiceTest {

    @Mock
    private AppProperties appProperties;

    private FileSystemService fileSystemService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        fileSystemService = new FileSystemService(appProperties);
    }

    @Test
    void getOutputPath_CreatesDirectoryWithCorrectTimestamp() {
        //given
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 20, 15, 30, 45);
        String expectedDirName = timestamp.format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        when(appProperties.outputDir()).thenReturn(tempDir.toString());

        //when
        Path outputPath = fileSystemService.getOutputPath(timestamp);

        //then
        assertTrue(Files.exists(outputPath));
        assertEquals(tempDir.resolve(expectedDirName), outputPath);
    }

    @Test
    void getOutputPath_ThrowsException_WhenDirectoryCreationFails() throws IOException {
        //given
        LocalDateTime timestamp = LocalDateTime.of(2024, 1, 20, 15, 30, 45);

        //create a file with the same name where directory should be created
        //this will cause directory creation to fail
        Path parentDir = Files.createTempDirectory("test");
        Path blockingFile = parentDir.resolve("20240120_153045");
        Files.createFile(blockingFile);

        //when
        when(appProperties.outputDir()).thenReturn(parentDir.toString());

        //then
        assertThrows(HotelFileSystemException.class, () -> fileSystemService.getOutputPath(timestamp));

        //cleanup
        Files.deleteIfExists(blockingFile);
        Files.deleteIfExists(parentDir);
    }

    @Test
    void saveJsonResult_SavesDataToCorrectFile() throws IOException {
        //given
        byte[] testData = "{\"test\": \"data\"}".getBytes();
        Path outputPath = tempDir.resolve("test_output");
        Files.createDirectories(outputPath);

        //when
        fileSystemService.saveJsonResult(testData, outputPath);

        //then
        Path jsonFile = outputPath.resolve("hotels.json");
        assertTrue(Files.exists(jsonFile));
        assertArrayEquals(testData, Files.readAllBytes(jsonFile));
    }

    @Test
    void downloadImage_SuccessfullyDownloadsValidImage() {
        //given
        String imageUrl = "https://download.samplelib.com/jpeg/sample-clouds-400x300.jpg";
        String hotelId = "hotel123";
        Path imagesDir = tempDir.resolve("images");

        //when
        Boolean result = fileSystemService.downloadImage(imageUrl, hotelId, imagesDir)
                .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .join();

        //then
        assertTrue(result, "Download should complete successfully");

        //verify that image directory exists
        assertTrue(Files.exists(imagesDir), "Images directory should exist");

        try {
            //find the downloaded file
            Path downloadedImage = Files.list(imagesDir)
                    .filter(file -> file.getFileName().toString().startsWith(hotelId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Downloaded file not found"));

            //verify file name format
            String fileName = downloadedImage.getFileName().toString();
            assertTrue(fileName.startsWith(hotelId), "File should start with hotel ID");
            assertTrue(fileName.endsWith(".jpg"), "File should end with .jpg");

            //verify file is not empty and is a valid image
            byte[] downloadedBytes = Files.readAllBytes(downloadedImage);
            assertTrue(downloadedBytes.length > 0, "Downloaded file should not be empty");

        } catch (IOException e) {
            fail("Failed to verify downloaded file: " + e.getMessage());
        }
    }

    @Test
    void downloadImage_ReturnsFalse_ForInvalidUrl() throws ExecutionException, InterruptedException {
        //given
        String invalidUrl = "invalid-url";
        String hotelId = "hotel123";
        Path imagesDir = tempDir.resolve("images");

        //when
        CompletableFuture<Boolean> result = fileSystemService.downloadImage(invalidUrl, hotelId, imagesDir);

        //then
        assertFalse(result.get());
    }
}
