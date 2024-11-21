package com.freshcells.converter.controllers;

import com.freshcells.converter.model.ProcessingResult;
import com.freshcells.converter.services.HotelConverterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/converter")
@RequiredArgsConstructor
public class HotelConverterController {
    private final HotelConverterService hotelConverterService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProcessingResult> convertFiles(
            @RequestParam("files") List<MultipartFile> files) {
        ProcessingResult result = hotelConverterService.processFiles(files);
        return ResponseEntity.ok(result);
    }
}
