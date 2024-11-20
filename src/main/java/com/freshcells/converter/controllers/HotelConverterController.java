package com.freshcells.converter.controllers;

import com.freshcells.converter.services.HotelConverterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE;

@RestController
@RequestMapping("/api/v1/converter")
@RequiredArgsConstructor
public class HotelConverterController {

    private final HotelConverterService converterService;

    @PostMapping(consumes = MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> convertFiles(
            @RequestParam("files") List<MultipartFile> files) {
        converterService.processFiles(files);
        return ResponseEntity.ok("Processing completed successfully");
    }
}
