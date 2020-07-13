package org.entando.kubernetes;

import org.entando.kubernetes.model.bundle.EntandoComponentBundle;
import org.entando.kubernetes.model.bundle.EntandoComponentBundleBuilder;
import org.entando.kubernetes.model.bundle.EntandoComponentBundleSpec;
import org.entando.kubernetes.model.bundle.EntandoComponentBundleSpecBuilder;

public class TestEntitiesGenerator {

    public static final String DEFAULT_BUNDLE_NAMESPACE = "entando-de-bundles";

    public static EntandoComponentBundle getTestBundle() {
        return new EntandoComponentBundleBuilder()
                .withNewMetadata()
                .withName("my-bundle")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(TestEntitiesGenerator.getTestEntandoComponentBundleSpec()).build();

    }

    public static EntandoComponentBundleSpec getTestEntandoComponentBundleSpec() {
        return new EntandoComponentBundleSpecBuilder()
                .withDescription("A bundle containing some demo components for Entano6")
                .withCode("my-bundle")
                .addNewVersion("0.0.1")
                .addNewKeyword("entando6")
                .addNewKeyword("digital-exchange")
                .addNewDistTag("latest", "0.0.1")
                .and()
                .addNewTag()
                .withVersion("0.0.1")
                .withIntegrity(
                        "sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball("http://localhost:8081/repository/npm-internal/my-bundle/-/my-bundle-0.0.1.tgz")
                .endTag()
                .build();
    }

    public static EntandoComponentBundleSpec getBundleSpecWithName(String name) {
        return new EntandoComponentBundleSpecBuilder()
                .withCode(name)
                .addNewVersion("0.0.1")
                .addNewDistTag("latest", "0.0.1")
                .and()
                .addNewTag()
                .withVersion("0.0.1")
                .withIntegrity(
                        "sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball("http://localhost:8081/repository/npm-internal/my-bundle/-/my-bundle-0.0.1.tgz")
                .endTag()
                .build();
    }
}