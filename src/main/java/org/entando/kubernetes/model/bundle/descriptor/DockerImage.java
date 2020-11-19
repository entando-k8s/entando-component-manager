package org.entando.kubernetes.model.bundle.descriptor;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class DockerImage {

    private static final String orgGroup = "o";
    private static final String nameGroup = "n";
    private static final String versGroup = "v";
    private static final Pattern imagePattern = Pattern
            .compile("(?<o>[a-z0-9]{4,30})/(?<n>[a-zA-Z0-9_.\\-]+)(?::(?<v>.*))?");


    private String organization;
    private String name;
    private String version;

    public static DockerImage fromString(String s) {
        Matcher m = imagePattern.matcher(s);
        if (!m.find()) {
            throw new MalformedDockerImageException("Impossible to read DockerImage from " + s);
        }
        String name = m.group(nameGroup);
        String organization = m.group(orgGroup);
        String version = m.group(versGroup);
        if (version == null || version.isEmpty()) {
            version = "latest";
        }

        return DockerImage.builder()
                .name(name)
                .organization(organization)
                .version(version)
                .build();
    }

    public String toString() {
        return String.format("%s/%s:%s", organization, name, version);
    }

    public static class MalformedDockerImageException extends RuntimeException {

        public MalformedDockerImageException(String s) {
            super(s);
        }
    }
}

