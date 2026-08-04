package com.coe.b04.server.utils;

import com.coe.b04.server.enums.TravelClass;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TravelClassConverter implements Converter<String, TravelClass> {

    @Override
    public TravelClass convert(String source) {
        if (source == null || source.isBlank()) {
            return null;
        }

        return TravelClass.fromValue(source);
    }
}
