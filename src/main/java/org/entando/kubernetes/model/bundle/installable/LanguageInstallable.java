package org.entando.kubernetes.model.bundle.installable;

import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;
import org.entando.kubernetes.client.core.EntandoCoreClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.LanguageDescriptor;

@Slf4j
public class LanguageInstallable extends Installable<LanguageDescriptor> {

    private final EntandoCoreClient engineService;

    public LanguageInstallable(EntandoCoreClient engineService, LanguageDescriptor languageDescriptor,
            InstallAction action) {
        super(languageDescriptor, action);
        this.engineService = engineService;
    }

    @Override
    public CompletableFuture<Void> install() {
        return CompletableFuture.runAsync(() -> {

            logConflictStrategyAction();

            if (!shouldCreate()) {
                return; //Do nothing
            }

            engineService.enableLanguage(representation);
        });
    }

    @Override
    public CompletableFuture<Void> uninstall() {
        return CompletableFuture.runAsync(() -> {
            log.info("Disabling Language {}", getName());
            if (shouldCreate()) {
                engineService.disableLanguage(getName());
            }
        });
    }

    @Override
    public ComponentType getComponentType() {
        return ComponentType.LANGUAGE;
    }

    @Override
    public String getName() {
        return representation.getCode();
    }

}
