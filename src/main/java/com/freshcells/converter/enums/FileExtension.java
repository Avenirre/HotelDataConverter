package com.freshcells.converter.enums;

import com.freshcells.converter.exceptions.HotelValidationException;
import lombok.Getter;

import java.util.Arrays;

@Getter
public enum FileExtension {
    XML("xml"),
    JSON("json");

    private final String extension;

    FileExtension(String extension) {
        this.extension = extension;
    }

    public static FileExtension fromExtension(String extension) {
        return Arrays.stream(values())
                .filter(type -> type.extension.equalsIgnoreCase(extension))
                .findFirst()
                .orElseThrow(() -> new HotelValidationException("Unsupported file type: " + extension));
    }
}
