package org.entando.kubernetes.model.bundle.installable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallRequest.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;

/**
 * This class will represent something that can be installed on Entando architecture.
 *
 * @param <T> Can be essentially anything. A service, a component or any other part on the Entando architecture
 * @author Sergio Marcelino
 */
@Slf4j
public abstract class Installable<T extends Descriptor> {

    protected final T representation;
    protected final InstallAction action;
    private final ObjectMapper objectMapper = new ObjectMapper();
    protected EntandoBundleComponentJobEntity job;

    public Installable(T representation, InstallAction action) {
        this.representation = representation;
        this.action = action;
    }

    @Deprecated
    public Installable(T representation) {
        this(representation, InstallAction.CREATE);
    }

    /**
     * This method will be called when every component was validated on the Digital Exchange bundle file.
     *
     * @return should return a CompletableFuture with its processing inside. It can be run asynchronously or not.
     */
    public abstract CompletableFuture<Void> install();

    /**
     * This method will be called when every component was validated on the Digital Exchange bundle file.
     *
     * @return should return a CompletableFuture with its processing inside. It can be run asynchronously or not.
     */
    public abstract CompletableFuture<Void> uninstall();

    /**
     * Should return the component type to understand what to do in case of a rollback.
     *
     * @return {@link ComponentType}
     */
    public abstract ComponentType getComponentType();

    public abstract String getName();

    public InstallAction getAction() {
        return action;
    }

    /**
     * Important to understand if something has changed in case of an updated If the checksum didn't change, we don't
     * need to modify this component.
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

    public T getRepresentation() {
        return this.representation;
    }

    public int getPriority() {
        return this.getComponentType().getInstallPriority();
    }

    public EntandoBundleComponentJobEntity getJob() {
        return this.job;
    }

    public void setJob(EntandoBundleComponentJobEntity job) {
        this.job = job;
    }

    public boolean shouldSkip() {
        return action == InstallAction.SKIP;
    }

    public boolean shouldCreate() {
        return action == InstallAction.CREATE;
    }

    public boolean shouldOverride() {
        return action == InstallAction.OVERRIDE;
    }

    protected void logConflictStrategyAction() {

        String actionLogName;

        switch (action) {
            case SKIP:
                actionLogName = "Skipping";
                break;
            case CREATE:
                actionLogName = "Creating";
                break;
            case OVERRIDE:
                actionLogName = "Overriding";
                break;
            default:
                actionLogName = "Conflict strategy action not recognized";
                break;
        }

        log.info("{} {} {}", actionLogName, getComponentType().getTypeName(), getName());
    }
}
