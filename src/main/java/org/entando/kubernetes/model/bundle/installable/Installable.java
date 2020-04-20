package org.entando.kubernetes.model.bundle.installable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleComponentJob;

/**
 * This class will represent something that can be installed on Entando architecture.
 *
 * @param <T> Can be essentially anything. A service, a component or any other part on the Entando architecture
 * @author Sergio Marcelino
 */
@Slf4j
public abstract class Installable<T> {

    protected final T representation;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private EntandoBundleComponentJob component;

    public Installable(T representation) {
        this.representation = representation;
    }

    /**
     * This method will be called when every component was validated on the Digital Exchange bundle file
     *
     * @return should return a CompletableFuture with its processing inside. It can be run asynchronously or not.
     */
    public abstract CompletableFuture install();

    /**
     * Should return the component type to understand what to do in case of a rollback
     *
     * @return {@link ComponentType}
     */
    public abstract ComponentType getComponentType();

    public abstract String getName();

    /**
     * Important to understand if something has changed in case of an updated If the checksum didn't change, we don't
     * need to modify this component
     *
     * @return md5 checksum of the component's payload
     */
    public String getChecksum() {
        try {
            return DigestUtils.md5Hex(objectMapper.writeValueAsString(representation));
        } catch (JsonProcessingException e) {
            log.error("Problem while processing checksum", e);
        }
        return null;
    }

    public EntandoBundleComponentJob getComponent() {
        return component;
    }

    public void setComponent(final EntandoBundleComponentJob component) {
        this.component = component;
    }
}
