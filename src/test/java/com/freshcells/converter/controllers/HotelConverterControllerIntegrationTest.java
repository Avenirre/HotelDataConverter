package com.freshcells.converter.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class HotelConverterControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void processHotelFiles_ValidatesResponse() throws Exception {
        List<MockMultipartFile> files = List.of(
                createMockFile("411144-giata.xml"),
                createMockFile("594608-coah.json"),
                createMockFile("3956-coah.xml"),
                createMockFile("162838-coah.xml"),
                createMockFile("162838-giata.xml"),
                createMockFile("3956-giata.xml")
        );

        var requestBuilder = MockMvcRequestBuilders
                .multipart("/api/v1/converter");

        files.forEach(requestBuilder::file);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jsonFile", notNullValue()))
                .andExpect(jsonPath("$.imagesDirectory", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.processedFiles", is(6)))
                .andExpect(jsonPath("$.downloadedImages", is(3)));
    }

    @Test
    void processHotelFiles_WithWrongFormat_ReturnsError() throws Exception {
        MockMultipartFile wrongFile = createMockFile("wrong-format.txt");

        var requestBuilder = MockMvcRequestBuilders
                .multipart("/api/v1/converter")
                .file(wrongFile);

        mockMvc.perform(requestBuilder)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", notNullValue()))
                .andExpect(jsonPath("$.message", containsString("Unsupported file type")));
    }

    private MockMultipartFile createMockFile(String filename) throws Exception {
        File file = ResourceUtils.getFile("classpath:testFiles/" + filename);
        byte[] content = Files.readAllBytes(file.toPath());
        return new MockMultipartFile(
                "files",
                filename,
                determineContentType(filename),
                content
        );
    }

    private String determineContentType(String filename) {
        if (filename.endsWith(".xml")) {
            return "application/xml";
        }
        return "application/json";
    }
}