package org.entando.kubernetes.stubhelper;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.model.bundle.BundleType;
import org.entando.kubernetes.model.bundle.descriptor.BundleDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.ComponentSpecDescriptor;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentAttribute;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;

public class BundleStubHelper {

    public static final String BUNDLE_CODE = "my-component";
    public static final String BUNDLE_DESCRIPTION = "desc";
    public static final BundleType BUNDLE_TYPE = BundleType.SYSTEM_LEVEL_BUNDLE;
//    public static final String BUNDLE_TYPE = "SYSTEM_LEVEL_BUNDLE";

    public static EntandoDeBundle stubEntandoDeBundle() {
        ObjectMeta metadata = new ObjectMeta();
        metadata.setLabels(Map.of("widgets", "true", "bundle-type", BUNDLE_TYPE.toString()));
        EntandoDeBundle entandoDeBundle = new EntandoDeBundle();
        entandoDeBundle.setMetadata(metadata);
        return entandoDeBundle;
    }


    public static BundleDescriptor stubBundleDescriptor(ComponentSpecDescriptor spec) {
        return new BundleDescriptor(BUNDLE_CODE, BUNDLE_DESCRIPTION, BUNDLE_TYPE, spec);
    }

    /**
     * receives a List of ContentAttribute representing the content of the file bundle/contents/cng102-descriptor.yaml.
     *
     * @return
     */
    public static List<ContentAttribute> stubContentAttributeList() {

        ContentAttribute contentAttribute1 = new ContentAttribute()
                .setCode("title")
                .setValues(Map.of("en", "Learn about checking and savings accounts"))
                .setElements(Arrays.asList(
                        new ContentAttribute()
                                .setCode("title elements")
                                .setValue("test value")
                                .setValues(Map.of("it", "Learn about checking and savings accounts in ita"))))
                .setCompositeelements(Arrays.asList(
                        new ContentAttribute()
                                .setCode("title composite elements")
                                .setValue("test composite")
                                .setValues(Map.of("gb", "Learn about checking and savings accounts in gb"))))
                .setListelements(Map.of(
                        "myel",
                        Arrays.asList(
                                new ContentAttribute()
                                        .setCode("title list elements")
                                        .setValue("test value list elements")
                                        .setValues(Map.of("fr", "Learn about checking and savings accounts in fr")))));

        ContentAttribute contentAttribute2 = new ContentAttribute()
                .setCode("subtitle")
                .setValues(Map.of("en", "Learn about checking and savings accounts sub"));

        ContentAttribute contentAttribute3 = new ContentAttribute()
                .setCode("descr");

        return Arrays.asList(contentAttribute1, contentAttribute2, contentAttribute3);
    }
}
