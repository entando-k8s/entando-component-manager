package org.entando.kubernetes.model.web.request;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

public abstract class RequestListProcessor<T> {

    private final PagedListRequest pagedListRequest;
    private Stream<T> stream;

    public RequestListProcessor(final PagedListRequest pagedListRequest, final Stream<T> stream) {
        this.pagedListRequest = pagedListRequest;
        this.stream = stream;
    }

    public RequestListProcessor(final PagedListRequest pagedListRequest, final List<T> items) {
        this(pagedListRequest, ofNullable(items).map(List::stream).orElseGet(Stream::empty));
    }

    protected abstract Function<Filter, Predicate<T>> getPredicates();

    protected abstract Function<String, Comparator<T>> getComparators();

    public RequestListProcessor<T> filter() {
        Function<Filter, Predicate<T>> predicatesProvider = this.getPredicates();
        if (null != this.pagedListRequest && null != this.pagedListRequest.getFilters()) {
            final Filter[] filters = this.pagedListRequest.getFilters();
            for (final Filter filter : filters) {
                final String filterAttribute = filter.getAttribute();
                final String filterValue = filter.getValue();
                if (filterAttribute != null && !filterAttribute.isEmpty() && (
                        filterValue != null && !filterValue.isEmpty()
                                || filter.getAllowedValues() != null && filter.getAllowedValues().length > 0)) {
                    final Predicate<T> predicate = predicatesProvider.apply(filter);
                    if (null != predicate) {
                        this.stream = this.stream.filter(predicate);
                    }
                }
            }
        }

        return this;
    }

    public RequestListProcessor<T> sort() {
        final Function<String, Comparator<T>> comparatorsProvider = this.getComparators();
        final String sort = this.pagedListRequest.getSort();
        final String direction = this.pagedListRequest.getDirection();

        if (sort != null && direction != null) {
            Comparator<T> comparator = comparatorsProvider.apply(this.pagedListRequest.getSort());
            if (comparator != null) {
                if (direction.equalsIgnoreCase(Filter.DESC_ORDER)) {
                    comparator = comparator.reversed();
                }

                this.stream = this.stream.sorted(comparator);
            }
        }

        return this;
    }

    public RequestListProcessor<T> filterAndSort() {
        return this.filter().sort();
    }

    public Stream<T> getStream() {
        return this.stream;
    }

    public List<T> toList() {
        return this.stream.collect(Collectors.toList());
    }
}