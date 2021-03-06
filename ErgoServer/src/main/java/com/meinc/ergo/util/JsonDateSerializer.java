package com.meinc.ergo.util;

import java.io.IOException;
import java.util.Date;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class JsonDateSerializer
extends JsonSerializer<Date>
{
    @Override
    public void serialize(Date date, JsonGenerator gen, SerializerProvider provider)
    throws IOException, JsonProcessingException
    {
        gen.writeNumber(date.getTime());
    }

}
