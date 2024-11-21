package com.freshcells.converter.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.freshcells.converter.exceptions.HotelFileProcessingException;
import com.freshcells.converter.exceptions.HotelValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Service
public class FileProcessingService {
    private final ObjectMapper jsonMapper;
    private final XmlMapper xmlMapper;

    public FileProcessingService(
            @Qualifier("jsonMapper") ObjectMapper jsonMapper,
            @Qualifier("xmlMapper") XmlMapper xmlMapper) {
        this.jsonMapper = jsonMapper;
        this.xmlMapper = xmlMapper;
    }

    public Map<String, Object> processFile(MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            if (filename == null) {
                throw new HotelValidationException("Filename is missing");
            }

            String fileExtension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

            return switch (fileExtension) {
                case "xml" -> xmlMapper.readValue(file.getInputStream(), Map.class);
                case "json" -> jsonMapper.readValue(file.getInputStream(), Map.class);
                default -> throw new HotelValidationException("Unsupported file type: " + filename);
            };
        } catch (IOException e) {
            throw new HotelFileProcessingException("Failed to process file: " + file.getOriginalFilename(), e);
        }
    }
}

