package com.freshcells.converter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.freshcells.converter.exceptions.HotelFileProcessingException;
import com.freshcells.converter.exceptions.HotelValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class FileProcessingServiceTest {

    @Mock
    private ObjectMapper jsonMapper;

    @Mock
    private XmlMapper xmlMapper;

    private FileProcessingService fileProcessingService;

    @BeforeEach
    void setUp() {
        fileProcessingService = new FileProcessingService(jsonMapper, xmlMapper);
    }

    @Test
    void processFile_WithXmlFile_ShouldProcessSuccessfully() throws IOException {
        // given
        Map<String, Object> expectedResult = Map.of("key", "value");
        MockMultipartFile xmlFile = new MockMultipartFile(
                "file",
                "test-giata.xml",
                "application/xml",
                "<test>content</test>".getBytes()
        );
        when(xmlMapper.readValue(any(InputStream.class), eq(Map.class)))
                .thenReturn(expectedResult);

        // when
        Map<String, Object> result = fileProcessingService.processFile(xmlFile);

        // then
        assertEquals(expectedResult, result);
    }

    @Test
    void processFile_WithJsonFile_ShouldProcessSuccessfully() throws IOException {
        // given
        Map<String, Object> expectedResult = Map.of("key", "value");
        MockMultipartFile jsonFile = new MockMultipartFile(
                "file",
                "test-coah.json",
                "application/json",
                "{\"test\":\"content\"}".getBytes()
        );
        when(jsonMapper.readValue(any(InputStream.class), eq(Map.class)))
                .thenReturn(expectedResult);

        // when
        Map<String, Object> result = fileProcessingService.processFile(jsonFile);

        // then
        assertEquals(expectedResult, result);
    }

    @Test
    void processFile_WithNullFilename_ShouldThrowValidationException() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                null,
                "application/json",
                "{}".getBytes()
        );

        // when & then
        assertThrows(HotelValidationException.class,
                () -> fileProcessingService.processFile(file));
    }

    @Test
    void processFile_WithUnsupportedExtension_ShouldThrowValidationException() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "content".getBytes()
        );

        // when & then
        assertThrows(HotelValidationException.class,
                () -> fileProcessingService.processFile(file));
    }

    @Test
    void processFile_WhenMapperThrowsIOException_ShouldThrowProcessingException() throws IOException {
        // given
        MockMultipartFile jsonFile = new MockMultipartFile(
                "file",
                "test-coah.json",
                "application/json",
                "invalid json".getBytes()
        );
        when(jsonMapper.readValue(any(InputStream.class), eq(Map.class)))
                .thenThrow(new IOException("Test exception"));

        // when & then
        assertThrows(HotelFileProcessingException.class,
                () -> fileProcessingService.processFile(jsonFile));
    }
}
