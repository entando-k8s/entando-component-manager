package org.entando.kubernetes.service.digitalexchange.component;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.job.EntandoBundleComponentJob;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterUtils;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.request.RequestListProcessor;

public class EntandoBundleComponentJobListProcessor extends RequestListProcessor<EntandoBundleComponentJob> {

    public static final String ID = "id";
    public static final String TYPE = "type";

    public EntandoBundleComponentJobListProcessor(PagedListRequest pagedListRequest,
            Stream<EntandoBundleComponentJob> stream) {
        super(pagedListRequest, stream);
    }

    public EntandoBundleComponentJobListProcessor(PagedListRequest pagedListRequest,
            List<EntandoBundleComponentJob> items) {
        super(pagedListRequest, items);
    }

    @Override
    protected Function<Filter, Predicate<EntandoBundleComponentJob>> getPredicates() {
        return filter -> {
            switch (filter.getAttribute()) {
                case ID:
                    return c -> FilterUtils.filterString(filter, c.getId().toString());
                case TYPE:
                    return c -> FilterUtils.filterString(filter, c.getType().name());
                default:
                    return null;
            }
        };
    }

    @Override
    protected Function<String, Comparator<EntandoBundleComponentJob>> getComparators() {
        return sort -> {
            switch (sort) {
                case TYPE:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getType().getTypeName(),
                            b.getType().getTypeName());
                case ID:
                default:
                    return Comparator.comparing(EntandoBundleComponentJob::getId);
            }
        };
    }
}
