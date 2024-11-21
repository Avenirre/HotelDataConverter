package com.freshcells.converter.model;

import java.nio.file.Path;
import java.time.LocalDateTime;

public record ProcessingResult(
        Path jsonFile,
        Path imagesDirectory,
        LocalDateTime timestamp,
        int processedFiles,
        int downloadedImages
) {}
