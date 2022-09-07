package org.entando.kubernetes.model.bundle.processor;

import static java.util.Optional.ofNullable;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallAction;
import org.entando.kubernetes.controller.digitalexchange.job.model.InstallPlan;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.Descriptor;
import org.entando.kubernetes.model.bundle.installable.Installable;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;

/**
 * Any classes that is called a Component Processor will be found automatically on the context to process the Zip File
 * with the Component Descriptor.
 */
public interface ComponentProcessor<T extends Descriptor> {

    /**
     * a method to centralize the current descriptor concrete class managed by the concrete Processor class.
     *
     * @return the class of the descriptor managed by the current processor
     */
    Class<T> getDescriptorClass();

    /**
     * a method to centralize the function to access the components needed by the concrete Processor.
     *
     * @return the method of the ComponentSpecDescriptor needed to access the component managed bu the current processor
     */
    Optional<Function<ComponentSpecDescriptor, List<String>>> getComponentSelectionFn();

    default boolean doesComponentDscriptorContainMoreThanOneSingleEntity() {
        return false;
    }

    /**
     * This method will process the component descriptor and should return an empty list or a list of all components
     * that should be installed.
     *
     * @param bundleReader bundle reader capable of reading the bundle using it's descriptor
     * @return Should return a list of Installables
     * @throws EntandoComponentManagerException in case of any error while reading any the file from the Zip package
     */
    List<Installable<T>> process(BundleReader bundleReader);

    /**
     * This method will process the component descriptor and should return an empty list or a list of all components
     * that should be installed.
     *
     * @param bundleReader     bundle reader capable of reading the bundle using it's descriptor
     * @param conflictStrategy default action in case of a component conflict
     * @param installPlan     the InstallPlan for the current bundle
     * @return Should return a list of Installables
     * @throws EntandoComponentManagerException in case of any error while reading any the file from the Zip package
     */
    List<Installable<T>> process(BundleReader bundleReader, InstallAction conflictStrategy, InstallPlan installPlan);

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

    /**
     * read the list of the desired component descriptors filenames from the bundle descriptor and return it.
     *
     * @param bundleReader the BundleReader to use to read the bundle descriptor
     * @return the list of the desired component descriptors filenames read from the bundle descriptor
     * @throws EntandoComponentManagerException if the param getComponentSelectionFnOpt is empty
     */
    default List<String> getDescriptorList(BundleReader bundleReader) {

        if (this.getComponentSelectionFn().isEmpty()) {
            throw new EntandoComponentManagerException(
                    "Extracting components from BundleDescriptor for null componentSelectionFunction");
        }

        BundleDescriptor descriptor = bundleReader.readBundleDescriptor();
        return ofNullable(descriptor.getComponents())
                .map(componentSpecDescriptor -> getComponentSelectionFn().get().apply(componentSpecDescriptor))
                .orElse(new ArrayList<>());
    }
}
