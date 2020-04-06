package org.entando.kubernetes.model.bundle.processor;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import org.entando.kubernetes.model.bundle.BundleReader;
import org.entando.kubernetes.model.bundle.descriptor.ComponentDescriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.digitalexchange.ComponentType;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJob;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeJobComponent;

/**
 * Any classes that is called a Component Processor will be found automatically on the context to process the Zip File with the Component
 * Descriptor.
 */
public interface ComponentProcessor {

    /**
     * This method will process the component descriptor and should return an empty list or a list of all components that should be
     * installed.
     *
     * @param job the job being executed in this processing
     * @param bundleReader npm package reader zip file being processed
     * @param descriptor The component descriptor being processed
     * @return Should return a list of Installables
     * @throws IOException in case of any error while reading any the file from the Zip package
     */
    List<Installable> process(DigitalExchangeJob job, BundleReader bundleReader,
            ComponentDescriptor descriptor) throws IOException;

    /**
     * This method will be executed on the uninstallation or modification of a component.
     *
     * @param componentType The component type being processed
     * @return true if this ComponentType can be processed by this processor
     */
    boolean shouldProcess(ComponentType componentType);

    /**
     * Performs the uninstallation of this component.
     *
     * @param component the component to be uninstalled
     */
    void uninstall(DigitalExchangeJobComponent component);

    default String getRelativePath(String referenceFile, String fileName) {
        return Paths.get(referenceFile).resolveSibling(fileName).toString();
    }

}
