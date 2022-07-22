package org.entando.kubernetes.utils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TestUtils {

    /**
     * use the received map as source to inject some variables in the environment.
     *
     * @param envMap the map from which get data to populate the environment variables
     * @throws Exception if an error occurr during the injection
     */
    public static void setEnv(Map<String, String> envMap) throws Exception {

        try {
            Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            theEnvironmentField.setAccessible(true);
            Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
            env.putAll(envMap);
            Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField(
                    "theCaseInsensitiveEnvironment");
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            cienv.putAll(envMap);
        } catch (NoSuchFieldException e) {
            Class[] classes = Collections.class.getDeclaredClasses();
            Map<String, String> env = System.getenv();
            for (Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    Object obj = field.get(env);
                    Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(envMap);
                }
            }
        }
    }
}
