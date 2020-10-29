package org.entando.kubernetes.model.bundle.processor;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;

/**
 * Any classes that is called a Component Processor will be found automatically on the context to process the Zip File
 * with the Component Descriptor.
 *
 */
public interface ComponentProcessor<T extends Descriptor> {

    /**
     * This method will process the component descriptor and should return an empty list or a list of all components
     * that should be installed.
     *
     *
     * @param bundleReader npm package reader zip file being processed
     * @return Should return a list of Installables
     * @throws EntandoComponentManagerException in case of any error while reading any the file from the Zip package
     */
    List<Installable<T>> process(BundleReader bundleReader);

    /**
     * This method extracts a list of installable components from a list of {@link org.entando.kubernetes.model.job.EntandoBundleComponentJob}.
     */
    List<Installable<T>> process(List<EntandoBundleComponentJobEntity> components);

    default Installable<T> process(EntandoBundleComponentJobEntity componentJob) {
        if (supportComponent(componentJob.getComponentType())) {
            return process(Collections.singletonList(componentJob)).get(0);
        }
        return null;
    }

    T buildDescriptorFromComponentJob(EntandoBundleComponentJobEntity component);

    ComponentType getSupportedComponentType();

    default boolean supportComponent(ComponentType ct) {
        return this.getSupportedComponentType().equals(ct);
    }

    default String getRelativePath(String referenceFile, String fileName) {
        return Paths.get(referenceFile).resolveSibling(fileName).toString();
    }

}
