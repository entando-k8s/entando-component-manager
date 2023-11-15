package org.entando.kubernetes.liquibase.helper;

import java.util.Arrays;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@UtilityClass
public class UpdateUtils {

    public static String getSchemaFromJdbc(String jdbcUrl) {
        String[] result = {null};

        String[] tokens = jdbcUrl.split("://");
        if (tokens.length > 1
                && StringUtils.isNotBlank(tokens[1])) {
            String[] url = tokens[1].split("\\?");
            if (url.length > 1
                    && StringUtils.isNotBlank(url[1])) {
                String[] query = url[1].split("&");
                Arrays.asList(query).forEach(p -> {
                    if (p.contains("currentSchema")) {
                        String[] args = p.split("=");

                        result[0] = args[1];
                    }
                });
            }
        }
        return result[0];
    }

}
