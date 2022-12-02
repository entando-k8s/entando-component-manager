package org.entando.kubernetes.model.bundle.descriptor;

import java.util.Optional;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Getter
@Setter
@Builder
@Slf4j
public class DockerImage {

    private static final String ERROR_MSG = "Impossible to read DockerImage from ";
    private String registry;
    private String organization;
    private String name;
    private String tag;
    private String sha256;

    public static DockerImage fromString(String imageAddress) {

        if (StringUtils.isBlank(imageAddress) || imageAddress.contains("//")) {
            throw new MalformedDockerImageException(ERROR_MSG + imageAddress);
        }

        String[] segments = imageAddress.split("/");
        String registry = null;
        String name = null;
        String organization = null;
        String tag = null;
        String sha = null;

        // extract repo and tag
        if (StringUtils.contains(segments[segments.length - 1], "@")) {
            String[] split4Tag = StringUtils.split(segments[segments.length - 1], "@");
            name = split4Tag[0];
            sha = split4Tag[1]; // tag == sha256:92ae85a2740161f8b534e0b85ad267624ea88def5691742008f9353cc72ec060
        } else {
            String[] repositorySegments = segments[segments.length - 1].split(":");
            if (repositorySegments.length == 1) {
                name = repositorySegments[0];
                tag = BundleUtilities.LATEST_VERSION;
            } else if (repositorySegments.length == 2) {
                name = repositorySegments[0];
                tag = repositorySegments[1];
            } else {
                log.debug("The repository '{}' is not supported. At most one colon (:) allowed.",
                        segments[segments.length - 1]);
                throw new MalformedDockerImageException(ERROR_MSG + imageAddress);
            }
        }

        // extract organization
        if (segments.length >= 2) {
            organization = joinOrganizationPath(segments, imageAddress);
        } else {
            log.debug("Docker organization is required:'{}'", imageAddress);
            throw new MalformedDockerImageException(ERROR_MSG + imageAddress);
        }

        // extract registry
        if (segments.length >= 2 && StringUtils.contains(segments[0], ".")) {
            registry = segments[0];
        }

        return DockerImage.builder()
                .name(name)
                .tag(tag)
                .sha256(sha)
                .organization(organization)
                .registry(registry)
                .build();

    }

    private static String joinOrganizationPath(String[] paths, String imageAddress) {
        StringBuilder org = new StringBuilder("");
        for (int i = 0; i < paths.length - 1; i++) {
            if (i > 1 || (i == 1 && !StringUtils.contains(paths[0], "."))) {
                org.append("/");
            }
            if ((i == 0 && !StringUtils.contains(paths[i], ".")) || i > 0) {
                org.append(paths[i]);
            }
        }
        if (StringUtils.isBlank(org.toString())) {
            log.debug("Docker organization is required:'{}'", imageAddress);
            throw new MalformedDockerImageException(ERROR_MSG + imageAddress);
        }
        return org.toString();
    }


    public String toString() {
        String reg = Optional.ofNullable(registry).map(r -> r + "/").orElse("");

        return Optional.ofNullable(sha256)
                .map(sha -> String.format("%s%s/%s@%s", reg, organization, name, sha))
                .orElseGet(() -> String.format("%s%s/%s:%s", reg, organization, name, tag));
    }

    public static class MalformedDockerImageException extends RuntimeException {

        public MalformedDockerImageException(String s) {
            super(s);
        }
    }
}
