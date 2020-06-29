package org.entando.kubernetes.service.digitalexchange.job;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.digitalexchange.EntandoBundleJob;
import org.entando.kubernetes.model.web.request.Filter;
import org.entando.kubernetes.model.web.request.FilterUtils;
import org.entando.kubernetes.model.web.request.PagedListRequest;
import org.entando.kubernetes.model.web.request.RequestListProcessor;

public class EntandoBundleJobListProcessor extends RequestListProcessor<EntandoBundleJob> {

    public static final String ID = "id";
    public static final String STATUS = "status";
    public static final String STARTED_AT = "startedAt";
    public static final String FINISHED_AT = "finishedAt";
    public static final String COMPONENT_ID = "componentId";
    public static final String COMPONENT_NAME = "componentName";
    public static final String COMPONENT_VERSION = "componentVersion";

    public EntandoBundleJobListProcessor(PagedListRequest pagedListRequest,
            Stream<EntandoBundleJob> stream) {
        super(pagedListRequest, stream);
    }

    public EntandoBundleJobListProcessor(PagedListRequest pagedListRequest,
            List<EntandoBundleJob> items) {
        super(pagedListRequest, items);
    }

    @Override
    protected Function<Filter, Predicate<EntandoBundleJob>> getPredicates() {
        return filter -> {
            switch (filter.getAttribute()) {
                case ID:
                    return c -> FilterUtils.filterString(filter, c.getId().toString());
                case COMPONENT_ID:
                    return c -> FilterUtils.filterString(filter, c.getComponentId());
                case COMPONENT_NAME:
                    return c -> FilterUtils.filterString(filter, c.getComponentName());
                case COMPONENT_VERSION:
                    return c -> FilterUtils.filterString(filter, c.getComponentVersion());
                case STATUS:
                    return c -> FilterUtils.filterString(filter, c.getStatus().name());
                case STARTED_AT:
                    return c -> FilterUtils.filterDate(filter, c.getStartedAt());
                case FINISHED_AT:
                    return c -> FilterUtils.filterDate(filter, c.getFinishedAt());
                default:
                    return null;
            }
        };
    }

    @Override
    protected Function<String, Comparator<EntandoBundleJob>> getComparators() {
        return sort -> {
            switch (sort) {
                case COMPONENT_ID:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getComponentId(), b.getComponentId());
                case COMPONENT_NAME:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getComponentName(), b.getComponentName());
                case COMPONENT_VERSION:
                    return (a, b) -> StringUtils.compareIgnoreCase(a.getComponentVersion(), b.getComponentVersion());
                case STARTED_AT:
                    return Comparator.comparing(EntandoBundleJob::getStartedAt);
                case FINISHED_AT:
                    return Comparator.comparing(EntandoBundleJob::getFinishedAt);
                case ID:
                default:
                    return Comparator.comparing(EntandoBundleJob::getId);
            }
        };
    }
}
