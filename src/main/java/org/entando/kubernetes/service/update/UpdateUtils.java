package org.entando.kubernetes.service.update;

import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class UpdateUtils {

    public static String getSchemaFromJdbc(String jdbcUrl) {
        String[] result = {null};

        String[] tokens = jdbcUrl.split("://");
        if (StringUtils.isNotBlank(tokens[1])) {
            String[] url = tokens[1].split("\\?");
            if (StringUtils.isNotBlank(url[1])) {
                String[] query = url[1].split("\\&");
                Arrays.asList(query).forEach(p -> {
                    // works with Postgres and Oracle
                    if (p.contains("currentSchema")) {
                        String[] args = p.split("\\=");

                        result[0] = args[1];
                    }
                });
            }
        }
        return result[0];
    }

}
