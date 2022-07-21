package org.entando.kubernetes.service.digitalexchange.job;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PostInitConfigurationServiceImpl implements PostInitConfigurationService, InitializingBean {

    private final String postInitConfigurationData;
    private static final ObjectMapper mapper = new ObjectMapper();

    private PostInitData configurationData;
    private static final PostInitData DEFAULT_CONFIGURATION_DATA;

    static {
        List<PostInitItem> items = new ArrayList<>();
        items.add(PostInitItem.builder()
                .name("entando-post-init-01")
                .url("docker://docker.io/entando/post-init-01")
                .version("1.0.0")
                .priority(1)
                .build());
        DEFAULT_CONFIGURATION_DATA = PostInitData.builder().frequency(3).items(items).build();
    }

    public PostInitConfigurationServiceImpl(@Value("${entando.ecr.postinit:#{null}}") String postInitConfigurationData) {
        this.postInitConfigurationData = postInitConfigurationData;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        configurationData = parsePostInitConfiguration().orElse(DEFAULT_CONFIGURATION_DATA);
    }


    @Override
    public int getFrequencyInSeconds() {
        return configurationData.getFrequency();
    }

    @Override
    public PostInitData getConfigurationData() {
        return configurationData;
    }

    @Override
    public Optional<Boolean> isEcrActionAllowed(String bundleCode, String action) {
        return configurationData.getItems().stream()
                .filter(item -> StringUtils.equals(PostInitServiceUtility.calculateBundleCode(item), bundleCode))
                .findFirst()
                .map(item -> Boolean.valueOf(ecrActionAllowed(item, action)))
                .or(Optional::empty);
    }


    private boolean ecrActionAllowed(PostInitItem item, String action) {
        boolean isActionAllowed =
                item.getEcrActions() != null && Arrays.stream(item.getEcrActions()).anyMatch(action::equals);
        log.trace("For bundle:'{}' action '{}' is allowed ? '{}'", item.getName(), action, isActionAllowed);
        return isActionAllowed;

    }

    private Optional<PostInitData> parsePostInitConfiguration() {
        if (StringUtils.isBlank(postInitConfigurationData)) {
            return Optional.empty();
        } else {
            try {
                return Optional.ofNullable(mapper.readValue(postInitConfigurationData, PostInitData.class));
            } catch (JsonProcessingException ex) {
                log.warn("Error processing json input configuration data:'{}'", postInitConfigurationData, ex);
                return Optional.empty();
            }
        }
    }

    @ToString
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostInitData {

        @Getter
        @Setter
        private int frequency;
        @Getter
        private List<PostInitItem> items = new ArrayList<>();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PostInitItem {

        private String name;
        private String[] ecrActions;
        private String action;
        private String url;
        private String version;
        private int priority;
    }
}
