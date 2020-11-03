package org.entando.kubernetes.client.core;

import java.io.File;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.client.ECMClient;
import org.entando.kubernetes.controller.digitalexchange.job.model.AnalysisReport;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.bundle.descriptor.AssetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.CategoryDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FileDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.FragmentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.GroupDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LabelDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.LanguageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.PageTemplateDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetConfigurationDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.contenttype.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;

public interface EntandoCoreClient extends ECMClient {

    void registerWidget(WidgetDescriptor descriptor);

    void deleteWidget(String code);

    EntandoCoreComponentUsage getWidgetUsage(String code);

    void registerFragment(FragmentDescriptor descriptor);

    void deleteFragment(String code);

    EntandoCoreComponentUsage getFragmentUsage(String code);

    void registerLabel(LabelDescriptor descriptor);

    void deleteLabel(String code);

    void enableLanguage(LanguageDescriptor descriptor);

    void disableLanguage(String code);

    void registerGroup(GroupDescriptor descriptor);

    void deleteGroup(String code);

    EntandoCoreComponentUsage getGroupUsage(String code);

    void registerPage(PageDescriptor pageDescriptor);

    void registerPageWidget(PageDescriptor pageDescriptor, WidgetConfigurationDescriptor widgetDescriptor);

    void deletePage(String code);

    EntandoCoreComponentUsage getPageUsage(String code);

    void registerPageModel(PageTemplateDescriptor descriptor);

    void deletePageModel(String code);

    EntandoCoreComponentUsage getPageModelUsage(String code);

    void deleteContentModel(String code);

    void registerContentModel(ContentTemplateDescriptor descriptor);

    EntandoCoreComponentUsage getContentModelUsage(String code);

    void registerContentType(ContentTypeDescriptor descriptor);

    void deleteContentType(String code);

    EntandoCoreComponentUsage getContentTypeUsage(String code);

    void createContent(ContentDescriptor descriptor);

    void updateContent(ContentDescriptor descriptor);

    void deleteContent(String code);

    void createAsset(AssetDescriptor descriptor, File file);

    void deleteAsset(String code);

    void createFolder(String folder);

    void deleteFolder(String code);

    void uploadFile(FileDescriptor descriptor);

    void registerCategory(CategoryDescriptor representation);

    void deleteCategory(String code);

    EntandoCoreComponentUsage getCategoryUsage(String code);

    AnalysisReport getEngineAnalysisReport(List<Reportable> reportableList);

    AnalysisReport getCMSAnalysisReport(List<Reportable> reportableList);
}
