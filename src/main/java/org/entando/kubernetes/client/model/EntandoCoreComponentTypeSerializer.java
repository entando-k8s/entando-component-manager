package org.entando.kubernetes.client.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import org.entando.kubernetes.model.bundle.ComponentType;

public class EntandoCoreComponentTypeSerializer extends StdSerializer<ComponentType> {

    public EntandoCoreComponentTypeSerializer() {
        this(null);
    }

    public EntandoCoreComponentTypeSerializer(Class<ComponentType> t) {
        super(t);
    }

    @Override
    public void serialize(ComponentType componentType,
                          JsonGenerator jsonGenerator,
                          SerializerProvider serializerProvider) throws IOException {
        jsonGenerator.writeString(componentType.getAppEngineTypeName());
    }
}
