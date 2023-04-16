package org.entando.kubernetes.config;

import org.entando.kubernetes.config.JavaNativeHintConfiguration.MyRuntimeHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(MyRuntimeHints.class)
public class JavaNativeHintConfiguration {

    public static class MyRuntimeHints implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.resources().registerPattern("db/changelog/db.changelog-master.yaml");
            hints.resources().registerPattern("db/changelog/changes/*");
        }

    }
}
