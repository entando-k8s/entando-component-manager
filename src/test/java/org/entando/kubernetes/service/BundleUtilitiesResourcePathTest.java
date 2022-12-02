package org.entando.kubernetes.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.io.IOException;
import org.entando.kubernetes.model.bundle.BundleProperty;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.reader.BundleReader;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

@Tag("unit")
class BundleUtilitiesResourcePathTest {

    private static final String BUNDLE_NAME = "entando-ab-core-navigation";
    private static final String BUNDLE_ID = "b3a2d562";

    private BundleReader bundleReader;
    private BundleDescriptor bundleDescriptor;

    @BeforeEach
    public void setup() throws IOException {
        bundleReader = Mockito.mock(BundleReader.class);
        when(bundleReader.getBundleName()).thenReturn(BUNDLE_NAME);
        when(bundleReader.getCode()).thenReturn(BUNDLE_NAME + "-" + BUNDLE_ID);

        bundleDescriptor = Mockito.mock(BundleDescriptor.class);
        when(bundleDescriptor.isVersion1()).thenReturn(false);
        when(bundleReader.isBundleV1()).thenReturn(false);
        when(bundleReader.readBundleDescriptor()).thenReturn(bundleDescriptor);
    }

    @Test
    void buildFullBundleResourcePath_shouldReplaceOnceBundleV5WithWidget() throws IOException {
        final String file = "widgets/app-builder-menu/app-builder-menu.umd.js";
        final String expectedResource = "bundles/entando-ab-core-navigation-b3a2d562/widgets/app-builder-menu-b3a2d562/app-builder-menu.umd.js";

        String path = BundleUtilities.buildFullBundleResourcePath(bundleReader, BundleProperty.WIDGET_FOLDER_PATH, file, BUNDLE_ID);
        assertThat(path).isEqualTo(expectedResource);
    }

    @Test
    void buildFullBundleResourcePath_shouldReplaceOnceBundleV5WithResource() throws IOException {
        final String file = "resources/text.txt";
        final String expectedResource = "bundles/entando-ab-core-navigation-b3a2d562/" + file;

        String path = BundleUtilities.buildFullBundleResourcePath(bundleReader, BundleProperty.RESOURCES_FOLDER_PATH, file, BUNDLE_ID);
        assertThat(path).isEqualTo(expectedResource);
    }

    @Test
    void buildFullBundleResourcePath_shouldReplaceOnceBundleV1() throws IOException {
        final String file = "resources/text.txt";
        final String expectedResource = "bundles/entando-ab-core-navigation/" + file;

        when(bundleDescriptor.isVersion1()).thenReturn(true);
        when(bundleReader.isBundleV1()).thenReturn(true);

        String path = BundleUtilities.buildFullBundleResourcePath(bundleReader, BundleProperty.RESOURCES_FOLDER_PATH, file, BUNDLE_ID);
        assertThat(path).isEqualTo(expectedResource);
    }

}
