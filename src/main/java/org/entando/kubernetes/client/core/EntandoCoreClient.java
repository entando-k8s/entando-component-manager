package org.entando.kubernetes.client.core;

import org.entando.kubernetes.model.bundle.descriptor.ContentModelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageModelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;

public interface EntandoCoreClient {

    void registerWidget(WidgetDescriptor descriptor);

    void deleteWidget(String code);

    EntandoCoreComponentUsage getWidgetUsage(String code);

    void registerFragment(FragmentDescriptor descriptor);

    void deleteFragment(String code);

    EntandoCoreComponentUsage getFragmentUsage(String code);

    void registerLabel(LabelDescriptor descriptor);

    void deleteLabel(String code);

    void registerPage(PageDescriptor pageDescriptor);

    void deletePage(String code);

    EntandoCoreComponentUsage getPageUsage(String code);

    void registerPageModel(PageModelDescriptor descriptor);

    void deletePageModel(String code);

    EntandoCoreComponentUsage getPageModelUsage(String code);

    void deleteContentModel(String code);

    void registerContentModel(ContentModelDescriptor descriptor);

    EntandoCoreComponentUsage getContentModelUsage(String code);

    void registerContentType(ContentTypeDescriptor descriptor);

    void deleteContentType(String code);

    EntandoCoreComponentUsage getContentTypeUsage(String code);

    void createFolder(String folder);

    void deleteFolder(String code);

    void uploadFile(FileDescriptor descriptor);
}
