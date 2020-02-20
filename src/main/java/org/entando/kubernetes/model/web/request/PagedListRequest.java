package org.entando.kubernetes.model.web.request;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.apache.commons.lang3.ArrayUtils;

@Data
public class PagedListRequest {

    public static final int PAGE_SIZE_DEFAULT = 100;
    public static final String SORT_VALUE_DEFAULT = "id";
    public static final String DIRECTION_VALUE_DEFAULT = Filter.ASC_ORDER;

    private String sort;
    private String direction;
    private Integer page;
    private Integer pageSize;
    private Filter[] filters;

    public PagedListRequest() {
        this.direction = DIRECTION_VALUE_DEFAULT;
        this.pageSize = PAGE_SIZE_DEFAULT;
        this.sort = SORT_VALUE_DEFAULT;
        this.page = 1;
    }

    public PagedListRequest(int page, int pageSize, String sort, String direction) {
        this.page = page;
        this.pageSize = pageSize;
        this.sort = sort;
        this.direction = direction;
    }

    public void addFilter(final Filter filter) {
        this.filters = ArrayUtils.add(this.filters, filter);
    }

    public <E> List<E> getSublist(final List<E> master) {
        if (null == master) {
            return new ArrayList<>();
        } else if (0 == this.getPage() || master.isEmpty()) {
            return master;
        } else {
            if (null  == pageSize) {
                this.setPageSize(PAGE_SIZE_DEFAULT);
            }
            final int offset = this.getOffset();
            final int size = master.size();
            final int offsetToApply = offset >= size ? size : offset;
            final int limitToApply = offsetToApply + pageSize > size ? size : offsetToApply + pageSize;
            return master.subList(offsetToApply, limitToApply);
        }
    }

    private Integer getOffset() {
        final int p = this.getPage() - 1;
        return null != this.getPage() && this.getPage() != 0 ? this.getPageSize() * p : 0;
    }

}
