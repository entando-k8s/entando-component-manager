package org.entando.kubernetes.assertionhelper;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.IntStream;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentAttribute;
import org.springframework.util.CollectionUtils;

public class ContentAssertionHelper {


    public static void assertOnContentAttributesList(List<ContentAttribute> actualList, List<ContentAttribute> expectedContentAttributeList) {

        assertThat(expectedContentAttributeList).hasSize(actualList.size());
        IntStream.range(0, actualList.size())
                .forEach(i -> assertOnContentAttributes(actualList.get(i), expectedContentAttributeList.get(i)));
    }

    public static void assertOnContentAttributes(ContentAttribute actual, ContentAttribute expected) {

        assertThat(expected.getCode()).isEqualTo(actual.getCode());
        assertThat(expected.getValue()).isEqualTo(actual.getValue());
        // values
        if (CollectionUtils.isEmpty(expected.getValues())) {
            assertThat(actual.getValues()).isEmpty();
        } else {
            assertThat(expected.getValues().keySet())
                    .containsExactlyElementsOf(actual.getValues().keySet());
            assertThat(expected.getValues().values())
                    .containsExactlyElementsOf(actual.getValues().values());
        }
        // elements
        if (CollectionUtils.isEmpty(expected.getElements())) {
            assertThat(actual.getElements()).isEmpty();
        } else {
            assertOnContentAttributesList(actual.getElements(), expected.getElements());
        }
        // compositeelements
        if (CollectionUtils.isEmpty(expected.getCompositeelements())) {
            assertThat(actual.getCompositeelements()).isEmpty();
        } else {
            assertOnContentAttributesList(actual.getCompositeelements(), expected.getCompositeelements());
        }
        // listelements
        if (CollectionUtils.isEmpty(expected.getListelements())) {
            assertThat(actual.getCompositeelements()).isEmpty();
        } else {
            assertThat(expected.getListelements().keySet())
                    .containsExactlyElementsOf(actual.getListelements().keySet());

            expected.getListelements().keySet()
                    .forEach(key ->
                            assertOnContentAttributesList(
                                    actual.getListelements().get(key),
                                    expected.getListelements().get(key)));
        }
    }
}
