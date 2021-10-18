package org.entando.kubernetes;

import java.util.UUID;
import org.entando.kubernetes.model.bundle.EntandoBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundle;
import org.entando.kubernetes.model.debundle.EntandoDeBundleBuilder;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpec;
import org.entando.kubernetes.model.debundle.EntandoDeBundleSpecBuilder;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.model.job.EntandoBundleJob;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;

public class TestEntitiesGenerator {

    public static final String DEFAULT_BUNDLE_NAMESPACE = "entando-de-bundles";
    public static final String LATEST_VERSION = "0.0.15";
    public static final String BUNDLE_TARBALL_URL = "http://localhost:8081/repository/npm-internal/my-bundle/-/my-bundle-0.0.1.tgz";

    public static EntandoDeBundle getTestBundle() {
        return new EntandoDeBundleBuilder()
                .withNewMetadata()
                .withName("my-bundle")
                .withNamespace(DEFAULT_BUNDLE_NAMESPACE)
                .endMetadata()
                .withSpec(TestEntitiesGenerator.getTestEntandoDeBundleSpec()).build();

    }

    public static EntandoDeBundleSpec getTestEntandoDeBundleSpec() {
        return getTestEntandoDeBundleSpec(BUNDLE_TARBALL_URL);
    }

    public static EntandoDeBundleSpec getTestEntandoDeBundleSpec(String tarballUrl) {
        return new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withDescription("A bundle containing some demo components for Entano6")
                .withName("my-bundle")
                .addNewVersion("0.0.1")
                .addNewKeyword("entando6")
                .addNewKeyword("digital-exchange")
                .addNewDistTag("latest", LATEST_VERSION)
                .and()
                .addNewTag()
                .withVersion("0.0.1")
                .withIntegrity(
                        "sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball(tarballUrl)
                .endTag()
                .build();
    }

    public static EntandoDeBundleSpec getBundleSpecWithName(String name) {
        return new EntandoDeBundleSpecBuilder()
                .withNewDetails()
                .withName(name)
                .addNewVersion("0.0.1")
                .addNewDistTag("latest", "0.0.17")
                .and()
                .addNewTag()
                .withVersion("0.0.1")
                .withIntegrity(
                        "sha512-n4TEroSqg/sZlEGg2xj6RKNtl/t3ZROYdNd99/dl3UrzCUHvBrBxZ1rxQg/sl3kmIYgn3+ogbIFmUZYKWxG3Ag==")
                .withShasum("4d80130d7d651176953b5ce470c3a6f297a70815")
                .withTarball(BUNDLE_TARBALL_URL)
                .endTag()
                .build();
    }

    public static EntandoBundleEntity getTestComponent() {
        EntandoBundleEntity component = new EntandoBundleEntity();
        component.setId("my-bundle");
        component.setName("My Bundle Title");
        component.setInstalled(true);
        component.setJob(getTestJobEntity());
        return component;
    }

    public static EntandoBundleEntity getTestComponent(String code, String title) {
        EntandoBundleEntity component = getTestComponent();
        component.setId(code);
        component.setName(title);
        return component;
    }

    public static EntandoBundle getTestEntandoBundle() {
        return getTestEntandoBundle(null);
    }

    public static EntandoBundle getTestEntandoBundle(EntandoBundleJob installJob) {
        return EntandoBundle.builder()
                .code("my-bundle")
                .title("My Bundle Title")
                .installedJob(installJob)
                .lastJob(installJob)
                .build();
    }

    public static EntandoBundleJob getTestJob() {
        return EntandoBundleJob.fromEntity(getTestJobEntity());
    }

    public static EntandoBundleJobEntity getTestJobEntity(String code, String title) {
        EntandoBundleJobEntity job = getTestJobEntity();
        job.setComponentId(code);
        job.setComponentName(title);
        return job;
    }

    public static EntandoBundleJobEntity getTestJobEntity() {
        return EntandoBundleJobEntity.builder()
                .id(UUID.randomUUID())
                .componentId("my-bundle")
                .componentName("My Bundle Title")
                .status(JobStatus.INSTALL_COMPLETED)
                .customInstallation(true)
                .build();
    }
}
