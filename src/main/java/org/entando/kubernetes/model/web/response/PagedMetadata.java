package org.entando.kubernetes.model.web.response;

import static java.util.Optional.ofNullable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.Data;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.PagedListRequest;

@Data
public class PagedMetadata<T> {

    private int page;
    private int pageSize;
    private int lastPage;
    private long totalItems;
    private String sort;
    private String direction;
    private Filter[] filters;

    private Map<String, String> additionalParams;

    @JsonIgnore
    private long actualSize;

    @JsonIgnore
    private List<T> body;

    public PagedMetadata() {
        this.filters = new Filter[0];
        this.additionalParams = new HashMap<>();
    }

    /**
     * Constructor to build PagedMetadata using a trimmed body
     *
     * @param req        the page request
     * @param body       the trimmed body list
     * @param totalItems the full body size
     */
    public PagedMetadata(final PagedListRequest req, final List<T> body, final long totalItems) {
        this(req, totalItems);

        this.body = body;
    }

    /**
     * Constructor to build PagedMetadata by trimming the full body
     *
     * @param req      the page request
     * @param fullBody the full body list
     */
    public PagedMetadata(final PagedListRequest req, final List<T> fullBody) {
        this(req, fullBody.size());

        int start = (req.getPage() - 1) * req.getPageSize();
        int end = start + req.getPageSize();
        end = end <= fullBody.size() ? end : fullBody.size();
        this.body = fullBody.subList(start, end);
    }

    public PagedMetadata(final PagedListRequest req, final long totalItems, List<T> body) {
        this(req, totalItems);
        this.body = body;
    }

    public PagedMetadata(final PagedListRequest req, final long totalItems) {
        this();

        this.actualSize = req.getPageSize() == 0 ? totalItems : req.getPageSize();
        this.page = req.getPage();
        this.pageSize = req.getPageSize();
        this.lastPage = (int) Math.ceil((double) totalItems / (double) this.actualSize);
        this.totalItems = totalItems;

        ofNullable(req.getSort()).ifPresent(this::setSort);
        ofNullable(req.getDirection()).ifPresent(this::setDirection);
        ofNullable(req.getFilters()).ifPresent(this::setFilters);
    }

    public PagedMetadata(final int page, final int size, final int last, final int totalItems) {
        this();

        this.page = page;
        this.pageSize = size;
        this.lastPage = last;
        this.totalItems = totalItems;
    }

    public <Y> PagedMetadata<Y> map(final Function<T, Y> mapper) {
        final PagedMetadata<Y> newPaged = new PagedMetadata<>();
        newPaged.page = this.page;
        newPaged.pageSize = this.pageSize;
        newPaged.lastPage = this.lastPage;
        newPaged.totalItems = this.totalItems;
        newPaged.sort = this.sort;
        newPaged.direction = this.direction;
        newPaged.filters = this.filters;
        newPaged.additionalParams = this.additionalParams;
        newPaged.actualSize = this.actualSize;
        newPaged.body = body.stream().map(mapper).collect(Collectors.toList());
        return newPaged;
    }

    public PagedRestResponse<T> toRestResponse() {
        return new PagedRestResponse<>(this);
    }

}
