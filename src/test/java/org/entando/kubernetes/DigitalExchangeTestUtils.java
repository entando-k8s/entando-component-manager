package org.entando.kubernetes;

import org.apache.logging.log4j.util.Strings;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DigitalExchangeTestUtils {

    private static String DE_PUBLIC_KEY;
    private static String DE_PRIVATE_KEY;

    public static String getTestPublicKey() {
        if (Strings.isEmpty(DE_PUBLIC_KEY)) {
            try {
                final Path publicKeyPath = Paths.get(DigitalExchangeTestUtils.class.getResource("/de_test_public_key.txt").toURI());
                DE_PUBLIC_KEY = new String(Files.readAllBytes(publicKeyPath));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return DE_PUBLIC_KEY;
    }

    public static String getTestPrivateKey() {
        if (Strings.isEmpty(DE_PRIVATE_KEY)) {
            try {
                final Path publicKeyPath = Paths.get(DigitalExchangeTestUtils.class.getResource("/de_test_private_key.txt").toURI());
                DE_PRIVATE_KEY = new String(Files.readAllBytes(publicKeyPath));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return DE_PRIVATE_KEY;
    }

}
