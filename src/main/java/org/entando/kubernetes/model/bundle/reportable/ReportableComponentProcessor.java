/**
 * this interface has to be implemented by those processors we want they create a report in order to manage conflicts.
 */

package org.entando.kubernetes.model.bundle.reportable;

import static org.entando.kubernetes.service.digitalexchange.BundleUtilities.removeProtocolAndGetBundleId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
import org.entando.kubernetes.model.bundle.processor.ProcessorHelper;
import org.entando.kubernetes.model.bundle.reader.BundleReader;

public interface ReportableComponentProcessor {

    /**
     * a function to centralize the ReportableRemoteHandler used by tge concrete processor.
     *
     * @return the ReportableRemoteHandler to use with the current reportableProcessor
     */
    ReportableRemoteHandler getReportableRemoteHandler();

    /**
     * This method will process the component descriptor and should return a Reportable containing - the componenttype -
     * the list of the components' identifiers that should be analysed. - the remote handler (e.g. entando-core,
     * k8s-service)
     *
     * @param bundleReader bundle reader capable of reading the bundle using it's descriptor
     * @return Should return a list of String representing the identifier of every read component
     * @throws EntandoComponentManagerException in case of any error while reading any the file from the Zip package
     */
    default Reportable getReportable(BundleReader bundleReader, ComponentProcessor<?> componentProcessor) {

        List<String> idList = new ArrayList<>();

        try {
            List<String> contentDescriptorList = componentProcessor.getDescriptorList(bundleReader);
            for (String fileName : contentDescriptorList) {
                idList.addAll(this.readDescriptorKeys(bundleReader, fileName, componentProcessor));
            }

            return new Reportable(componentProcessor.getSupportedComponentType(), idList,
                    this.getReportableRemoteHandler());

        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format("Error generating Reportable for %s type",
                    componentProcessor.getSupportedComponentType().getTypeName()), e);
        }
    }

    /**
     * reads the keys of the components from the descriptor identified by the received filename.
     *
     * @param bundleReader the bundler reader to use in order to read the bundle
     * @param fileName     the filename identifying the descriptor file to read
     * @return the list of the keys of the components read from the descriptor
     */
    default List<String> readDescriptorKeys(BundleReader bundleReader, String fileName,
            ComponentProcessor<?> componentProcessor) {

        final String bundleId = removeProtocolAndGetBundleId(bundleReader.getBundleUrl());

        try {
            if (componentProcessor.doesComponentDscriptorContainMoreThanOneSingleEntity()) {
                return bundleReader.readListOfDescriptorFile(fileName, componentProcessor.getDescriptorClass())
                        .stream().map(descriptor ->
                                ProcessorHelper.replaceBundleIdPlaceholder(descriptor.getComponentKey().getKey(),
                                        bundleId)
                        )
                        .collect(Collectors.toList());
            } else {
                final String key = bundleReader.readDescriptorFile(fileName, componentProcessor.getDescriptorClass())
                        .getComponentKey().getKey();
                return Arrays.asList(ProcessorHelper.replaceBundleIdPlaceholder(key, bundleId));
            }
        } catch (IOException e) {
            throw new EntandoComponentManagerException(String.format(
                    "Error parsing content type %s from descriptor %s",
                    componentProcessor.getSupportedComponentType(), fileName), e);
        }
    }
}

