package org.entando.kubernetes.model.entandocore;

import lombok.Data;
import org.apache.logging.log4j.util.Strings;
import org.entando.kubernetes.model.bundle.descriptor.PageDescriptor;

import java.util.List;
import java.util.Map;

@Data
public class EntandoCorePage {

    private String code;
    private String parentCode;
    private Map<String, String> titles;
    private String pageModel;
    private String ownerGroup;
    private List<String> joinGroups;
    private boolean displayedInMenu;
    private boolean seo;
    private String charset;
    private String status;

    private EntandoCorePage() {
    }

    public EntandoCorePage(PageDescriptor pd) {
        this.code = pd.getCode();
        this.parentCode = valueOrDefault(pd.getParentCode(), "homepage");
        this.titles = pd.getTitles();
        this.pageModel = valueOrDefault(pd.getPageModel(), "home");
        this.ownerGroup = valueOrDefault(pd.getOwnerGroup(), "free");
        this.joinGroups = pd.getJoinGroups();
        this.seo = pd.isSeo();
        this.displayedInMenu = pd.isDisplayedInMenu();
        this.charset = valueOrDefault(pd.getCharset(), "utf-8");
        this.status = valueOrDefault(pd.getStatus(), "draft");
    }

    private String valueOrDefault(String value, String defaultValue) {
        return Strings.isBlank(value) ? defaultValue : value;
    }

}
