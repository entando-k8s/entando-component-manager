package org.entando.kubernetes.model.bundle.descriptor;

import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.ObjectUtils;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;

@Getter
@Setter
@Builder
public class DockerImage {

    private static final String ORG_GROUP = "o";
    private static final String NAME_GROUP = "n";
    private static final String TAG_GROUP = "t";
    private static final String SHA_GROUP = "s";
    // regex to group with names (tag and sha are mutually exclusive)
    private static final Pattern IMAGE_PATTERN = Pattern
            .compile("(?<o>[a-z0-9]{4,30})/(?<n>[a-zA-Z0-9_.\\-]+)((?::(?<t>.*))|(?:@(?<s>sha256:(.*))))?");


    private String organization;
    private String name;
    private String tag;
    private String sha256;

    public static DockerImage fromString(String s) {

        if (s == null) {
            throw new MalformedDockerImageException("Impossible to read DockerImage from a null value");
        }

        var m = IMAGE_PATTERN.matcher(s);
        if (m.matches()) {
            String name = m.group(NAME_GROUP);
            String organization = m.group(ORG_GROUP);
            String tag = m.group(TAG_GROUP);
            String sha = m.group(SHA_GROUP);

            if (ObjectUtils.isEmpty(tag) && ObjectUtils.isEmpty(sha)) {
                tag = BundleUtilities.LATEST_VERSION;
            }

            return DockerImage.builder()
                    .name(name)
                    .tag(tag)
                    .sha256(sha)
                    .organization(organization)
                    .build();
        }

        throw new MalformedDockerImageException("Impossible to read DockerImage from " + s);
    }

    public String toString() {
        return Optional.ofNullable(sha256)
                .map(sha -> String.format("%s/%s@%s", organization, name, sha))
                .orElseGet(() -> String.format("%s/%s:%s", organization, name, tag));
    }

    public static class MalformedDockerImageException extends RuntimeException {

        public MalformedDockerImageException(String s) {
            super(s);
        }
    }
}

