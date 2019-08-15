package org.entando.kubernetes.service.digitalexchange.installable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class Installable<T> {

    protected final T representation;
    private DigitalExchangeJobComponent component;

    public Installable(T representation) {
        this.representation = representation;
    }

    private final ObjectMapper objectMapper = new ObjectMapper();

    public abstract CompletableFuture install();

    public abstract ComponentType getComponentType();

    public abstract String getName();

    public String getChecksum() {
        try {
            return DigestUtils.md5Hex(objectMapper.writeValueAsString(representation));
        } catch (JsonProcessingException e) {
            log.error("Problem while processing checksum", e);
        }
        return null;
    }

    public DigitalExchangeJobComponent getComponent() {
        return component;
    }

    public void setComponent(final DigitalExchangeJobComponent component) {
        this.component = component;
    }
}
