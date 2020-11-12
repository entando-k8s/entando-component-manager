package org.entando.kubernetes.stubhelper;

import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;

public class PageStubHelper {

    public static final String STUB_SUFFIX = " - STUB";
    public static final String PAGE_CODE = "my-page";
    public static final String PAGE_CHARSET = "iso1923-12";
    public static final String PAGE_PARENT_CODE = "plugins";
    public static final boolean PAGE_DISPLAYED_IN_MENU = true;
    public static final String PAGE_PAGE_TEMPLATE = "service";
    public static final String PAGE_OWNER_GROUP = "administrators";
    public static final String PAGE_STATUS = "published";
    public static final String PAGE_JOIN_GROUP_1 = "free";
    public static final String PAGE_ = "";
    public static final String PAGE_TITLE_IT_KEY = "it";
    public static final String PAGE_TITLE_EN_KEY = "en";
    public static final String PAGE_TITLE_IT_VALUE = "La mia pagina" + STUB_SUFFIX;
    public static final String PAGE_TITLE_EN_VALUE = "My page" + STUB_SUFFIX;
    public static final Entry<String, String> PAGE_TITLE_ENTRY_IT = new SimpleEntry<>(PAGE_TITLE_IT_KEY, PAGE_TITLE_IT_VALUE);
    public static final Entry<String, String> PAGE_TITLE_ENTRY_EN = new SimpleEntry<>(PAGE_TITLE_EN_KEY, PAGE_TITLE_EN_VALUE);

    public static Map<String, String> stubPageTitles() {
        return Map.ofEntries(PAGE_TITLE_ENTRY_IT, PAGE_TITLE_ENTRY_EN);
    }

    public static List<String> stubPageJoinGroups() {
        return Collections.singletonList(PAGE_JOIN_GROUP_1);
    }

    public static PageDescriptor stubPageDescriptor() {
        return PageDescriptor.builder()
                .code(PAGE_CODE)
                .parentCode(PAGE_PARENT_CODE)
                .pageModel(PAGE_PAGE_TEMPLATE)
                .ownerGroup(PAGE_OWNER_GROUP)
                .titles(stubPageTitles())
                .build();
    }

    public static PageDescriptor stubPageConfigurationDescriptor() {
        return PageDescriptor.builder()
                .code(PAGE_CODE)
                .parentCode(PAGE_PARENT_CODE)
                .charset(PAGE_CHARSET)
                .displayedInMenu(PAGE_DISPLAYED_IN_MENU)
                .pageModel(PAGE_PAGE_TEMPLATE)
                .ownerGroup(PAGE_OWNER_GROUP)
                .titles(stubPageTitles())
                .status(PAGE_STATUS)
                .joinGroups(stubPageJoinGroups())
                .widgets(WidgetStubHelper.stubWidgetConfigurationDescriptor())
                .build();
    }
}
