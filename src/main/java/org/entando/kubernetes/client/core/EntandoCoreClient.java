package org.entando.kubernetes.client.core;

import java.io.File;
import java.util.List;
import org.entando.kubernetes.client.ECMClient;
import org.entando.kubernetes.client.model.AnalysisReport;
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
import org.entando.kubernetes.model.bundle.descriptor.content.ContentDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.contenttype.ContentTypeDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetConfigurationDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.widget.WidgetDescriptor;
import org.entando.kubernetes.model.bundle.reportable.Reportable;
import org.entando.kubernetes.model.entandocore.EntandoCoreComponentUsage;

public interface EntandoCoreClient extends ECMClient {

    String PUBLISHED = "published";
    String DRAFT = "draft";

    void createWidget(WidgetDescriptor descriptor);

    void updateWidget(WidgetDescriptor descriptor);

    void deleteWidget(String code);

    EntandoCoreComponentUsage getWidgetUsage(String code);

    void createFragment(FragmentDescriptor descriptor);

    void updateFragment(FragmentDescriptor descriptor);

    void deleteFragment(String code);

    EntandoCoreComponentUsage getFragmentUsage(String code);

    void createLabel(LabelDescriptor descriptor);

    void updateLabel(LabelDescriptor descriptor);

    void deleteLabel(String code);

    void enableLanguage(LanguageDescriptor descriptor);

    void disableLanguage(String code);

    void createGroup(GroupDescriptor descriptor);

    void updateGroup(GroupDescriptor descriptor);

    void deleteGroup(String code);

    EntandoCoreComponentUsage getGroupUsage(String code);

    void createPage(PageDescriptor pageDescriptor);

    void updatePageConfiguration(PageDescriptor pageDescriptor);

    void configurePageWidget(PageDescriptor pageDescriptor, WidgetConfigurationDescriptor widgetDescriptor);

    void setPageStatus(String code, String status);

    void deletePage(String code);

    EntandoCoreComponentUsage getPageUsage(String code);

    void createPageTemplate(PageTemplateDescriptor descriptor);

    void updatePageTemplate(PageTemplateDescriptor descriptor);

    void deletePageModel(String code);

    EntandoCoreComponentUsage getPageModelUsage(String code);

    void deleteContentModel(String code);

    void createContentTemplate(ContentTemplateDescriptor descriptor);

    void updateContentTemplate(ContentTemplateDescriptor descriptor);

    EntandoCoreComponentUsage getContentModelUsage(String code);

    void createContentType(ContentTypeDescriptor descriptor);

    void updateContentType(ContentTypeDescriptor descriptor);

    void deleteContentType(String code);

    EntandoCoreComponentUsage getContentTypeUsage(String code);

    void createContent(ContentDescriptor descriptor);

    void updateContent(ContentDescriptor descriptor);

    void publishContent(ContentDescriptor descriptor);

    void deleteContent(String code);

    void createAsset(AssetDescriptor descriptor, File file);

    void updateAsset(AssetDescriptor descriptor, File file);

    void deleteAsset(String code);

    void createFolder(String folder);

    void deleteFolder(String code);

    void createFile(FileDescriptor descriptor);

    void updateFile(FileDescriptor descriptor);

    void createCategory(CategoryDescriptor representation);

    void updateCategory(CategoryDescriptor representation);

    void deleteCategory(String code);

    EntandoCoreComponentUsage getCategoryUsage(String code);

    AnalysisReport getEngineAnalysisReport(List<Reportable> reportableList);

    AnalysisReport getCMSAnalysisReport(List<Reportable> reportableList);
}
