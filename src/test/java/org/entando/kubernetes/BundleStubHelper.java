package org.entando.kubernetes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.entando.kubernetes.model.bundle.descriptor.content.ContentAttribute;

public class BundleStubHelper {


    /**
     * receives a List of ContentAttribute representing the content of the file bundle/contents/cng102-descriptor.yaml.
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
