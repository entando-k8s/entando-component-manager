/**
 * this interface has to be implemented by those processors we want they create a report in order to manage conflicts.
 */

package org.entando.kubernetes.model.bundle.reportable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.entando.kubernetes.model.bundle.processor.ComponentProcessor;
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
                idList.addAll(componentProcessor.readDescriptorKeys(bundleReader, fileName));
            }

            return new Reportable(componentProcessor.getSupportedComponentType(), idList,
                    this.getReportableRemoteHandler());

        } catch (IOException e) {
            throw new EntandoComponentManagerException("Error reading bundle", e);
        }
    }
}

