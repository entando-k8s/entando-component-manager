package org.entando.kubernetes.model.web.request;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.collections4.comparators.TransformingComparator;

public class FilterUtils {
    private FilterUtils() {
    }

    public static TransformingComparator createCaseInsensitiveComparator() {
        final Transformer caseInsensitiveTransformer = (input) -> input instanceof String ? ((String) input).toLowerCase() : input;
        return new TransformingComparator(caseInsensitiveTransformer);
    }

    public static boolean filterString(final Filter filter, final String value) {
        final FilterOperator operator = getFilterOperator(filter);
        if (value == null) {
            return false;
        }

        boolean result = false;
        for (final String filterValue : getFilterValues(filter)) {
            switch (operator) {
                case EQUAL:
                    result |= value.equals(filterValue);
                    break;
                case NOT_EQUAL:
                    result |= !value.equals(filterValue);
                    break;
                case LIKE:
                    result |= value.toLowerCase().contains(filterValue.toLowerCase());
                    break;
                case GREATER:
                    result |= value.compareTo(filterValue) >= 0;
                    break;
                case LOWER:
                    result |= value.compareTo(filterValue) <= 0;
                    break;
                default:
                    throw new UnsupportedOperationException(getUnsupportedOperatorMessage(filter));
            }
        }

        return result;
    }

    public static boolean filterBoolean(Filter filter, boolean value) {
        final FilterOperator operator = getFilterOperator(filter);
        final Iterator iterator = getTypedAllowedValues(filter, (v) -> Boolean.parseBoolean(v.toLowerCase())).iterator();
        boolean result = false;

        while (iterator.hasNext()) {
            boolean filterValue = (Boolean)iterator.next();
            switch(operator) {
            case EQUAL:
            case LIKE:
                result |= value == filterValue;
                break;
            case NOT_EQUAL:
                result |= value != filterValue;
                break;
            default:
                throw new UnsupportedOperationException(getUnsupportedOperatorMessage(filter));
            }
        }

        return result;
    }

    public static boolean filterInt(Filter filter, Integer value) {
        return filterDouble(filter, value.doubleValue());
    }

    public static boolean filterLong(Filter filter, Long value) {
        return filterDouble(filter, value.doubleValue());
    }

    public static boolean filterDate(Filter filter, LocalDateTime value) {
        final List<Double> filterValues = getTypedAllowedValues(filter, (v) -> {
            try {
                return (double) LocalDateTime.parse(v).toEpochSecond(ZoneOffset.UTC);
            } catch (Exception var3) {
                throw new RuntimeException(var3);
            }
        });
        return filterDouble(filter, filterValues, (double) value.toEpochSecond(ZoneOffset.UTC));
    }

    public static boolean filterDate(Filter filter, Date value) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        final List<Double> filterValues = getTypedAllowedValues(filter, v -> {
            try {
                return (double) sdf.parse(v).getTime();
            } catch (ParseException var3) {
                throw new RuntimeException(var3);
            }
        });
        return filterDouble(filter, filterValues, (double) value.getTime());
    }

    public static boolean filterDouble(Filter filter, double value) {
        return filterDouble(filter, getTypedAllowedValues(filter, Double::parseDouble), value);
    }

    private static boolean filterDouble(Filter filter, List<Double> filterValues, double value) {
        final FilterOperator operator = getFilterOperator(filter);
        boolean result = false;

        for (final double filterValue : filterValues) {
            switch (operator) {
                case EQUAL:
                case LIKE:
                    result |= value == filterValue;
                    break;
                case NOT_EQUAL:
                    result |= value != filterValue;
                    break;
                case GREATER:
                    result |= value >= filterValue;
                    break;
                case LOWER:
                    result |= value <= filterValue;
                    break;
                default:
                    throw new UnsupportedOperationException(getUnsupportedOperatorMessage(filter));
            }
        }

        return result;
    }

    private static <T> List<T> getTypedAllowedValues(Filter filter, Function<String, T> converter) {
        return getFilterValues(filter).stream()
                .map(converter)
                .collect(Collectors.toList());
    }

    private static FilterOperator getFilterOperator(Filter filter) {
        return FilterOperator.parse(filter.getOperator());
    }

    private static String getUnsupportedOperatorMessage(Filter filter) {
        return "Operator '" + filter.getOperator() + "' is not supported";
    }

    private static List<String> getFilterValues(Filter filter) {
        if (filter.getAllowedValues() != null && filter.getAllowedValues().length != 0) {
            return Arrays.asList(filter.getAllowedValues());
        } else {
            final List<String> values = new ArrayList<>();
            if (filter.getValue() != null) {
                values.add(filter.getValue());
            }
            return values;
        }
    }
}