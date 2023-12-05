package org.entando.kubernetes.liquibase.helper;

import java.security.SecureRandom;
import java.util.Arrays;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
@UtilityClass
public class DbMigrationUtils {

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

    private static final String SYMBOL_POOL = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public static String generateSecureRandomHash(int length) {
        SecureRandom secureRandom = new SecureRandom();
        StringBuilder randomStringBuilder = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int idx = secureRandom.nextInt(SYMBOL_POOL.length());
            char currentChar = SYMBOL_POOL.charAt(idx);
            randomStringBuilder.append(currentChar);
        }
        return randomStringBuilder.toString();
    }


}
