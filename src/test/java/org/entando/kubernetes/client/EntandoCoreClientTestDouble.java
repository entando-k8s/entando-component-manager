package org.entando.kubernetes.client;

import java.io.File;
import java.util.List;
import org.entando.kubernetes.client.core.EntandoCoreClient;
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
import org.junit.jupiter.api.Tag;

@Tag("unit")
public class EntandoCoreClientTestDouble implements EntandoCoreClient {


    @Override
    public void createWidget(WidgetDescriptor descriptor) {

    }

    @Override
    public void updateWidget(WidgetDescriptor descriptor) {

    }

    @Override
    public void deleteWidget(String code) {

    }

    @Override
    public EntandoCoreComponentUsage getWidgetUsage(String code) {
        return null;
    }

    @Override
    public void createFragment(FragmentDescriptor descriptor) {

    }

    @Override
    public void updateFragment(FragmentDescriptor descriptor) {

    }

    @Override
    public void deleteFragment(String code) {

    }

    @Override
    public EntandoCoreComponentUsage getFragmentUsage(String code) {
        return null;
    }

    @Override
    public void createLabel(LabelDescriptor descriptor) {

    }

    @Override
    public void updateLabel(LabelDescriptor descriptor) {

    }

    @Override
    public void deleteLabel(String code) {

    }

    @Override
    public void enableLanguage(LanguageDescriptor descriptor) {

    }

    @Override
    public void disableLanguage(String code) {

    }

    @Override
    public void createGroup(GroupDescriptor descriptor) {

    }

    @Override
    public void updateGroup(GroupDescriptor descriptor) {

    }

    @Override
    public void deleteGroup(String code) {

    }

    @Override
    public EntandoCoreComponentUsage getGroupUsage(String code) {
        return null;
    }

    @Override
    public void createPage(PageDescriptor pageDescriptor) {

    }

    @Override
    public void updatePageConfiguration(PageDescriptor pageDescriptor) {

    }

    @Override
    public void configurePageWidget(PageDescriptor pageDescriptor, WidgetConfigurationDescriptor widgetDescriptor) {

    }

    @Override
    public void setPageStatus(String code, String status) {

    }

    @Override
    public void deletePage(String code) {

    }

    @Override
    public EntandoCoreComponentUsage getPageUsage(String code) {
        return null;
    }

    @Override
    public void createPageTemplate(PageTemplateDescriptor descriptor) {

    }

    @Override
    public void updatePageTemplate(PageTemplateDescriptor descriptor) {

    }

    @Override
    public void deletePageModel(String code) {

    }

    @Override
    public EntandoCoreComponentUsage getPageModelUsage(String code) {
        return null;
    }

    @Override
    public void deleteContentModel(String code) {

    }

    @Override
    public void createContentTemplate(ContentTemplateDescriptor descriptor) {

    }

    @Override
    public void updateContentTemplate(ContentTemplateDescriptor descriptor) {

    }

    @Override
    public EntandoCoreComponentUsage getContentModelUsage(String code) {
        return null;
    }

    @Override
    public void createContentType(ContentTypeDescriptor descriptor) {

    }

    @Override
    public void updateContentType(ContentTypeDescriptor descriptor) {

    }

    @Override
    public void deleteContentType(String code) {

    }

    @Override
    public EntandoCoreComponentUsage getContentTypeUsage(String code) {
        return null;
    }

    @Override
    public void createContent(ContentDescriptor descriptor) {

    }

    @Override
    public void updateContent(ContentDescriptor descriptor) {

    }

    @Override
    public void publishContent(ContentDescriptor descriptor) {

    }

    @Override
    public void deleteContent(String code) {

    }

    @Override
    public void createAsset(AssetDescriptor descriptor, File file) {

    }

    @Override
    public void updateAsset(AssetDescriptor descriptor, File file) {

    }

    @Override
    public void deleteAsset(String code) {

    }

    @Override
    public void createFolder(String folder) {

    }

    @Override
    public void deleteFolder(String code) {

    }

    @Override
    public void createFile(FileDescriptor descriptor) {

    }

    @Override
    public void updateFile(FileDescriptor descriptor) {

    }

    @Override
    public void createCategory(CategoryDescriptor representation) {

    }

    @Override
    public void updateCategory(CategoryDescriptor representation) {

    }

    @Override
    public void deleteCategory(String code) {

    }

    @Override
    public EntandoCoreComponentUsage getCategoryUsage(String code) {
        return null;
    }

    @Override
    public AnalysisReport getEngineAnalysisReport(List<Reportable> reportableList) {
        return null;
    }

    @Override
    public AnalysisReport getCMSAnalysisReport(List<Reportable> reportableList) {
        return null;
    }
}
