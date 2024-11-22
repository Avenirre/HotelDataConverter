package com.freshcells.converter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freshcells.converter.exceptions.HotelValidationException;
import com.freshcells.converter.model.ProcessingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HotelConverterServiceTest {

    @Mock
    private FileProcessingService fileProcessingService;
    @Mock
    private FileSystemService fileSystemService;
    @Mock
    private ObjectMapper objectMapper;

    private HotelConverterService hotelConverterService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        hotelConverterService = new HotelConverterService(
                fileProcessingService,
                fileSystemService,
                objectMapper
        );
    }

    @Test
    void processFiles_SuccessfulProcessing() throws Exception {
        //given
        Path outputPath = tempDir.resolve("output");
        Path imagesDir = outputPath.resolve("images");

        MockMultipartFile giataFile = new MockMultipartFile(
                "file",
                "123-giata.json",
                "application/json",
                "{\"test\": \"data\"}".getBytes()
        );

        MockMultipartFile coaFile = new MockMultipartFile(
                "file",
                "123-coah.json",
                "application/json",
                "{\"test\": \"data\"}".getBytes()
        );

        // Mock responses
        when(fileSystemService.getOutputPath(any(LocalDateTime.class))).thenReturn(outputPath);

        Map<String, Object> giataContent = new HashMap<>();
        giataContent.put("image", List.of(
                Map.of("url", "https://example.com/image1.jpg"),
                Map.of("url", "https://example.com/image2.jpg")
        ));

        Map<String, Object> coaContent = new HashMap<>();
        coaContent.put("images", List.of("https://example.com/image3.jpg"));

        when(fileProcessingService.processFile(giataFile)).thenReturn(giataContent);
        when(fileProcessingService.processFile(coaFile)).thenReturn(coaContent);

        when(fileSystemService.downloadImage(anyString(), anyString(), any(Path.class)))
                .thenReturn(CompletableFuture.completedFuture(true));

        when(objectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());

        //when
        ProcessingResult result = hotelConverterService.processFiles(List.of(giataFile, coaFile));

        //then
        assertNotNull(result);
        assertEquals(outputPath.resolve("hotels.json"), result.jsonFile());
        assertEquals(imagesDir, result.imagesDirectory());
        assertEquals(2, result.processedFiles());
        assertEquals(3, result.downloadedImages());

        verify(fileSystemService).saveJsonResult(any(), eq(outputPath));
        verify(fileSystemService, times(3))
                .downloadImage(anyString(), eq("123"), any(Path.class));
    }

    @Test
    void processFiles_WithFailedImageDownloads() throws Exception {
        // Given
        Path outputPath = tempDir.resolve("output");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "123-giata.json",
                "application/json",
                "{\"test\": \"data\"}".getBytes()
        );

        Map<String, Object> content = new HashMap<>();
        content.put("image", List.of(
                Map.of("url", "https://example.com/image1.jpg"),
                Map.of("url", "https://example.com/image2.jpg")
        ));

        when(fileSystemService.getOutputPath(any(LocalDateTime.class))).thenReturn(outputPath);
        when(fileProcessingService.processFile(file)).thenReturn(content);
        when(fileSystemService.downloadImage(anyString(), anyString(), any(Path.class)))
                .thenReturn(CompletableFuture.completedFuture(false));
        when(objectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());

        // When
        ProcessingResult result = hotelConverterService.processFiles(List.of(file));

        // Then
        assertEquals(1, result.processedFiles());
        assertEquals(0, result.downloadedImages());
    }

    @Test
    void processFiles_WithInvalidFileName() throws Exception {
        //given
        Path outputPath = tempDir.resolve("output");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid-filename.json",
                "application/json",
                "{\"test\": \"data\"}".getBytes()
        );

        //when & then
        when(fileSystemService.getOutputPath(any(LocalDateTime.class))).thenReturn(outputPath);
        when(fileProcessingService.processFile(any())).thenReturn(new HashMap<>());

        assertThrows(HotelValidationException.class, () ->
                hotelConverterService.processFiles(List.of(file))
        );

        verify(fileProcessingService).processFile(any());

        verify(objectMapper, never()).writeValueAsBytes(any());
        verify(fileSystemService, never()).saveJsonResult(any(), any());
    }

    @Test
    void processFiles_WithInvalidFileType() throws Exception {
        //given
        Path outputPath = tempDir.resolve("output");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "123-invalid.json", // неправильный тип файла (не giata и не coa)
                "application/json",
                "{\"test\": \"data\"}".getBytes()
        );

        //when & then
        when(fileSystemService.getOutputPath(any(LocalDateTime.class))).thenReturn(outputPath);

        when(fileProcessingService.processFile(any())).thenReturn(new HashMap<>());

        assertThrows(HotelValidationException.class, () ->
                hotelConverterService.processFiles(List.of(file))
        );

        verify(fileProcessingService).processFile(any());

        verify(objectMapper, never()).writeValueAsBytes(any());
        verify(fileSystemService, never()).saveJsonResult(any(), any());
    }

    @Test
    void processFiles_WithProcessingError() throws Exception {
        //given
        Path outputPath = tempDir.resolve("output");

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "123-giata.json",
                "application/json",
                "{\"test\": \"data\"}".getBytes()
        );

        //when & then
        when(fileSystemService.getOutputPath(any(LocalDateTime.class))).thenReturn(outputPath);

        RuntimeException expectedException = new RuntimeException("Processing error");
        when(fileProcessingService.processFile(file)).thenThrow(expectedException);

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                hotelConverterService.processFiles(List.of(file))
        );

        assertEquals("Processing error", thrown.getMessage());

        verify(fileSystemService, never()).saveJsonResult(any(), any());
        verify(objectMapper, never()).writeValueAsBytes(any());
    }

    @Test
    void processFiles_EmptyFileList() throws Exception {
        // Given
        Path outputPath = tempDir.resolve("output");
        when(fileSystemService.getOutputPath(any(LocalDateTime.class))).thenReturn(outputPath);
        when(objectMapper.writeValueAsBytes(any())).thenReturn("{}".getBytes());

        // When
        ProcessingResult result = hotelConverterService.processFiles(List.of());

        // Then
        assertEquals(0, result.processedFiles());
        assertEquals(0, result.downloadedImages());
        verify(fileProcessingService, never()).processFile(any());
    }
}