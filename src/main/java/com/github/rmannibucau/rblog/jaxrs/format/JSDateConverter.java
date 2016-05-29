package com.github.rmannibucau.rblog.jaxrs.format;

import org.apache.johnzon.mapper.Converter;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Date;

public class JSDateConverter implements Converter<Date> {
    @Override
    public String toString(final Date instance) {
        final Instant instant = Instant.ofEpochMilli(instance.getTime());
        final ZoneOffset offset = ZoneId.of("UTC").getRules().getOffset(instant); // ensure we'll not add [UTC] as suffix
        return ZonedDateTime.ofInstant(instant, offset).toString();
    }

    @Override
    public Date fromString(final String text) {
        final ZonedDateTime parsed = ZonedDateTime.parse(text);
        return new Date(parsed.toLocalDateTime().toInstant(parsed.getOffset()).toEpochMilli());
    }
}
