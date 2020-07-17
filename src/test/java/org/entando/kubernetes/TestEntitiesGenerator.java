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
                .addToLabels("widget", "true")
                .endMetadata()
                .withSpec(TestEntitiesGenerator.getTestEntandoComponentBundleSpec()).build();

    }

    public static EntandoComponentBundleSpec getTestEntandoComponentBundleSpec() {
        return new EntandoComponentBundleSpecBuilder()
                .withTitle("My Demo Bundle")
                .withDescription("A bundle containing some demo components for Entano6")
                .withCode("my-bundle")
                .withOrganization("entando")
                .withThumbnail("resources/thumbail.png")
                .addNewVersion()
                .withVersion("0.0.1")
                .withIntegrity(
                        "sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withUrl("http://localhost:8081/repository/npm-internal/my-bundle/-/my-bundle-0.0.1.tgz")
                .withTimestamp("2020-07-19T16:20:00.000Z")
                .endVersion()
                .build();
    }

    public static EntandoComponentBundleSpec getBundleSpecWithName(String name) {
        return new EntandoComponentBundleSpecBuilder()
                .withCode(name)
                .addNewVersion()
                .withVersion("0.0.1")
                .withIntegrity("sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withUrl("http://localhost:8081/repository/npm-internal/my-bundle/-/my-bundle-0.0.1.tgz")
                .endVersion()
                .build();
    }
}