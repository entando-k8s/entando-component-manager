package org.entando.kubernetes.validator;

import java.net.URL;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.exception.EntandoValidationException;
import org.entando.kubernetes.model.debundle.EntandoDeBundleTag;

@Getter
public class ImageValidator {

    public static final String DOCKER_TRANSPORT = "docker";
    public static final String DOCKER_OFFICIAL_LIBRARY = "library";
    public static final String TAG_LATEST = "latest";

    private static final Pattern TAG_REGEXP = Pattern.compile("^[\\w][\\w.-]{0,127}$");
    private static final Pattern DIGEST_REGEXP = Pattern.compile("^sha256:[a-z0-9]{32,}$");
    @Getter(AccessLevel.NONE)
    private final boolean isValid;
    private final String originalUrl;
    private String transport;
    private String domainRegistry;
    private String organization;
    private String repository;
    private String tag;
    private boolean isDigest;

    // inspired by https://github.com/containers/skopeo/blob/02ae5c2af5d198fa178917899370f42d11e6b206/vendor/github.com/containers/image/v5/docker/reference/normalize.go#L32
    private ImageValidator(String url) {
        originalUrl = url;
        SplitResult res = splitTransport(url);
        if (res.isValid()) {
            String remain = res.getRemain();
            res = splitDomain(remain);
            if (res.isValid()) {
                res = splitOrganization(res.getRemain());
                if (res.isValid() && splitTag(res.getRemain())) {
                    isValid = true;
                    return;
                }
            }
        }
        isValid = false;
    }

    /**
     * This method parses a "fully qualified" docker image URL to obtain an object that can be used to validate the
     * individual fields that make it up. Example of full docker image URL:
     * "docker://quay.io/centos7/nginx-116-centos7:1.2.3"
     *
     * @param url the string containing the fully qualified docker url to parse and validate
     * @return an ImageValidator object with the parsed url
     */
    public static ImageValidator parse(String url) {
        return new ImageValidator(url);
    }

    public static ImageValidator parse(EntandoDeBundleTag tag) {
        String fullyQualifiedImageUrl = generateFullyQualifiedWithTag(tag);
        return new ImageValidator(fullyQualifiedImageUrl);
    }

    public static ImageValidator parse(String url, String version) {
        String fullyQualifiedImageUrl = generateFullyQualifiedWithTag(url, version);
        return new ImageValidator(fullyQualifiedImageUrl);
    }

    private SplitResult splitTransport(String url) {
        String[] split = StringUtils.split(url, "://", 2);
        if (split.length == 2 && StringUtils.equalsIgnoreCase(split[0], DOCKER_TRANSPORT)) {
            transport = DOCKER_TRANSPORT;
            return new SplitResult(true, split[1]);
        } else {
            return new SplitResult(false, null);
        }
    }

    private SplitResult splitDomain(String remain) {
        String[] split4Domain = StringUtils.split(remain, "/", 2);
        if (split4Domain.length == 2) {
            domainRegistry = split4Domain[0];
            return new SplitResult(true, split4Domain[1]);
        } else {
            return new SplitResult(false, null);
        }
    }

    private SplitResult splitOrganization(String remain) {
        String[] split4Organization = StringUtils.split(remain, "/");
        switch (split4Organization.length) {
            case 1:
                organization = DOCKER_OFFICIAL_LIBRARY;
                return new SplitResult(true, remain);
            case 2:
                organization = split4Organization[0];
                return new SplitResult(true, split4Organization[1]);
            default:
                return new SplitResult(false, null);
        }
    }

    private boolean splitTag(String remain) {
        if (StringUtils.contains(remain, "@")) {
            String[] split4Tag = StringUtils.split(remain, "@");
            repository = split4Tag[0];
            tag = split4Tag[1]; // tag == sha256:92ae85a2740161f8b534e0b85ad267624ea88def5691742008f9353cc72ec060
            isDigest = true;

        } else {
            String[] split4Tag = StringUtils.split(remain, ":");
            switch (split4Tag.length) {
                case 1:
                    repository = split4Tag[0];
                    // manage use case docker://docker.io/nginx: as not valid
                    tag = StringUtils.contains(remain, ":") ? null : TAG_LATEST;
                    isDigest = false;
                    break;
                case 2:
                    repository = split4Tag[0];
                    tag = split4Tag[1];
                    isDigest = false;
                    break;
                default:
                    return false;
            }
        }
        return true;
    }

    /**
     * This method checks if the transport section is correct.
     *
     * @return returns true if transport is equal to "docker://" otherwise returns false
     */
    public boolean isTransportValid() {
        return StringUtils.equals(transport, DOCKER_TRANSPORT);
    }

    /**
     * This method checks if the transport section is correct.
     *
     * @param invalidError the error string to use within EntandoValidationException error message
     * @return returns true if transport is equal to "docker://" otherwise raises an EntandoValidationException
     *     exception
     */
    public boolean isTransportValid(String invalidError) {
        if (StringUtils.equals(transport, DOCKER_TRANSPORT)) {
            return true;
        } else {
            String errorMsg = String.format("%s: into url:'%s' organization:'%s' not valid",
                    invalidError, originalUrl, organization);
            throw new EntandoValidationException(errorMsg);
        }
    }

    /**
     * This method checks  if the domain registry section is correct.
     *
     * @param invalidError the error string to use within EntandoValidationException error message
     * @return returns true if domain registry is correct otherwise raises an EntandoValidationException exception
     */
    public boolean isDomainRegistryValid(String invalidError) {
        if (StringUtils.isNotBlank(domainRegistry) && isURIok(domainRegistry) && checkValidStartChar(domainRegistry)) {
            return true;
        } else {
            String errorMsg = String.format("%s: into url:'%s' domainRegistry:'%s' not valid",
                    invalidError, originalUrl, domainRegistry);
            throw new EntandoValidationException(errorMsg);
        }
    }

    private boolean isURIok(String domainWithoutSchema) {
        URL url;
        try {
            url = new URL("http://" + domainWithoutSchema);
            url.toURI();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkValidStartChar(String domainWithoutSchema) {
        return ValidationFunctions.HOST_MUST_START_AND_END_WITH_ALPHANUMERIC_REGEX_PATTERN.matcher(domainWithoutSchema)
                .matches();
    }

    /**
     * This method checks if the organization section is correct.
     *
     * @param invalidError the error string to use within EntandoValidationException error message
     * @return returns true if organization is correct otherwise raises an EntandoValidationException exception
     */
    public boolean isOrganizationValid(String invalidError) {
        if (StringUtils.isNotBlank(organization) && checkValidStartChar(organization)) {
            return true;
        } else {
            String errorMsg = String.format("%s: into url:'%s' organization:'%s' not valid",
                    invalidError, originalUrl, organization);
            throw new EntandoValidationException(errorMsg);
        }
    }

    /**
     * This method checks  if the repository section is correct.
     *
     * @param invalidError the error string to use within EntandoValidationException error message
     * @return returns true if repository is correct otherwise raises an EntandoValidationException exception
     */
    public boolean isRepositoryValid(String invalidError) {
        if (StringUtils.isNotBlank(repository) && checkValidStartChar(repository)) {
            return true;
        } else {
            String errorMsg = String.format("%s: into url:'%s' repository:'%s' not valid",
                    invalidError, originalUrl, repository);
            throw new EntandoValidationException(errorMsg);
        }
    }

    /**
     * This method checks  if the tag section is correct.
     *
     * @param invalidError the error string to use within EntandoValidationException error message
     * @return returns true if tag is correct otherwise raises an EntandoValidationException exception
     */
    public boolean isTagValid(String invalidError) {
        if (StringUtils.isNotBlank(tag) && (isValidTag(tag) || isValidDigest(tag))) {
            return true;
        } else {
            String errorMsg = String.format("%s: into url:'%s' tag:'%s' not valid",
                    invalidError, originalUrl, tag);
            throw new EntandoValidationException(errorMsg);
        }
    }

    private boolean isValidTag(String tag) {
        return !isDigest && TAG_REGEXP.matcher(tag).matches();
    }

    private boolean isValidDigest(String tag) {
        return isDigest && DIGEST_REGEXP.matcher(tag).matches();
    }

    /**
     * This method checks if the all sections are correct.
     *
     * @param invalidError the error string to use within EntandoValidationException error message
     * @return returns true if all sections are correct otherwise raises an EntandoValidationException exception
     */
    public boolean isValidOrThrow(String invalidError) {
        return isTransportValid(invalidError) && isDomainRegistryValid(invalidError) && isOrganizationValid(
                invalidError)
                && isRepositoryValid(invalidError)
                && isTagValid(invalidError);
    }

    /**
     * This method checks if the all sections are correct and then compose the common url to use to generate PluginID.
     *
     * @param invalidError the error string to use within EntandoValidationException error message
     * @return returns the common Url to use to generate PluginID if all sections are correct otherwise raises an
     *     EntandoValidationException exception
     */
    public String composeCommonUrlOrThrow(String invalidError) {
        this.isValidOrThrow(invalidError);
        return DOCKER_TRANSPORT + "://" + Paths.get(domainRegistry, organization, repository);
    }

    public String composeCommonUrlWithoutTransportWithoutTagOrThrow(String invalidError) {
        this.isValidOrThrow(invalidError);
        return Paths.get(domainRegistry, organization, repository).toString();
    }

    public String composeCommonUrlWithoutTransportOrThrow(String invalidError) {
        this.isValidOrThrow(invalidError);
        String url = Paths.get(domainRegistry, organization, repository).toString();

        return generateFullyQualifiedWithTag(url);
    }

    private String generateFullyQualifiedWithTag(String url) {
        if (tag != null) {
            String sep = ":";
            if (isDigest) {
                sep = "@";
            }
            url = url + sep + tag;

        }
        return url;
    }

    public static String generateFullyQualifiedWithTag(EntandoDeBundleTag tag) {
        return generateFullyQualifiedWithTag(tag.getTarball(), tag.getVersion());
    }

    public static String generateFullyQualifiedWithTag(String url, String version) {
        String fullyQualified = url;
        if (version != null) {
            String sep = ":";
            if (StringUtils.startsWithIgnoreCase(version, "sha256:")) {
                sep = "@";
            }
            fullyQualified = fullyQualified + sep + version;

        }
        return fullyQualified;
    }


    @AllArgsConstructor
    @Getter
    private static class SplitResult {

        private boolean isValid;
        private String remain;

    }
}
