package com.freshcells.converter.model;

import java.util.HashMap;
import java.util.Map;

public record HotelData(
        Map<String, Object> giata,
        Map<String, Object> coa
) {
    public static HotelData empty() {
        return new HotelData(new HashMap<>(), new HashMap<>());
    }

    public HotelData withGiata(Map<String, Object> giata) {
        return new HotelData(giata, this.coa);
    }

    public HotelData withCoa(Map<String, Object> coa) {
        return new HotelData(this.giata, coa);
    }
}
