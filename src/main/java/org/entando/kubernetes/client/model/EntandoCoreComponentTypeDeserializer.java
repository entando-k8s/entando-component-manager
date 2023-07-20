package org.entando.kubernetes.client.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.io.IOException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.model.bundle.ComponentType;

@Slf4j
public class EntandoCoreComponentTypeDeserializer extends StdDeserializer<ComponentType> {

    public EntandoCoreComponentTypeDeserializer() {
        this(null);
    }

    public EntandoCoreComponentTypeDeserializer(Class<ComponentType> t) {
        super(t);
    }

    @Override
    public ComponentType deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {

        String appEngineValue = jsonParser.getText();
        return getComponentTypeFromAppEngineValue(appEngineValue);

    }

    public static ComponentType getComponentTypeFromAppEngineValue(String appEngineValue) {
        return Arrays.stream(ComponentType.values())
                .filter(c -> c.getAppEngineTypeName().equals(appEngineValue))
                .findFirst()
                .orElseThrow(() -> {
                    log.warn("error retrieving a valid ComponentType for:'{}'", appEngineValue);
                    throw new IllegalArgumentException(
                            String.format("error retrieving a valid ComponentType for:'%s'", appEngineValue));
                });
    }

}
