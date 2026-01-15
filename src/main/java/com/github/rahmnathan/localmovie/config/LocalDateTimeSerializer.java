package com.github.rahmnathan.localmovie.config;

import org.springframework.boot.jackson.JacksonComponent;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@JacksonComponent
public class LocalDateTimeSerializer extends ValueSerializer<LocalDateTime> {

    @Override
    public void serialize(LocalDateTime localDateTime, JsonGenerator jsonGenerator, SerializationContext context) {
        jsonGenerator.writeString(String.valueOf(localDateTime.toEpochSecond(ZoneOffset.UTC)));
    }
}
