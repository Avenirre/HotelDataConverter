package com.freshcells.converter.enums;

import com.freshcells.converter.exceptions.HotelValidationException;

public enum FileType {
    GIATA,
    COA;

    public static FileType fromFilename(String filename) {
        if (filename.contains("-giata.")) return GIATA;
        if (filename.contains("-coah.")) return COA;
        throw new HotelValidationException("Unknown file type: " + filename);
    }
}
