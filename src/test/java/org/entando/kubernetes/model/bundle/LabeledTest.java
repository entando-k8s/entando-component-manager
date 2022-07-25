package org.entando.kubernetes.model.bundle;

import static org.assertj.core.api.Java6Assertions.assertThat;

import java.util.List;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.stubhelper.BundleInfoStubHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class LabeledTest {

    @Test
    void shouldCreateTheExpectedLabeledWithASetOfValues() {

        EntandoBundleEntity entity = new EntandoBundleEntity()
                .setPbcList(String.join(",", BundleInfoStubHelper.GROUPS_NAME));

        DummyLabeled target = new DummyLabeled();
        target.setPbcLabelsFrom(entity);

        String[] expected = BundleInfoStubHelper.GROUPS_NAME.toArray(String[]::new);
        List<String> actual = target.getLabels().getPbcNames();

        assertThat(actual).containsExactlyInAnyOrder(expected);
    }

    @Test
    void shouldSetAnEmptyPbcListWhileDealingWithAnEmptyOrNullPbcString() {

        DummyLabeled target = new DummyLabeled();

        // empty string
        EntandoBundleEntity entity = new EntandoBundleEntity().setPbcList("");
        target.setPbcLabelsFrom(entity);
        List<String> actual = target.getLabels().getPbcNames();
        assertThat(actual).isNull();

        // null string
        entity.setPbcList(null);
        target.setPbcLabelsFrom(entity);
        actual = target.getLabels().getPbcNames();
        assertThat(actual).isNull();
    }



    private static class DummyLabeled implements Labeled {

        private Labels labels;

        @Override
        public Labels getLabels() {
            return labels;
        }

        @Override
        public void setLabels(Labels labels) {
            this.labels = labels;
        }
    }
}
