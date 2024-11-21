package com.freshcells.converter.model;

import java.util.HashMap;
import java.util.Map;

public record HotelData(
        String hotelId,
        Map<String, Object> giata,
        Map<String, Object> coa
) {
    public static HotelData empty(String hotelId) {
        return new HotelData(hotelId, new HashMap<>(), new HashMap<>());
    }

    public HotelData withGiata(Map<String, Object> giata) {
        return new HotelData(hotelId, giata, this.coa);
    }

    public HotelData withCoa(Map<String, Object> coa) {
        return new HotelData(hotelId, this.giata, coa);
    }
}
