package org.entando.kubernetes.service.digitalexchange.signature;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SignatureUtilTest {

    @Test
    public void testPublicKeyToPemAndBack() {
        final KeyPair keyPair = SignatureUtil.createKeyPair();
        final PublicKey publicKey = keyPair.getPublic();
        final String pemPublicKey = SignatureUtil.publicKeyToPem(publicKey);
        final PublicKey parsedKey = SignatureUtil.publicKeyFromPem(pemPublicKey);

        assertThat(publicKey.getEncoded()).isEqualTo(parsedKey.getEncoded());
    }

    @Test
    public void testPrivateKeyToPemAndBack() {
        final KeyPair keyPair = SignatureUtil.createKeyPair();
        final PrivateKey privateKey = keyPair.getPrivate();
        final String pemPrivateKey = SignatureUtil.privateKeyToPem(privateKey);
        final PrivateKey parsedKey = SignatureUtil.privateKeyFromPem(pemPrivateKey);

        assertThat(privateKey.getEncoded()).isEqualTo(parsedKey.getEncoded());
    }

    @Test
    public void testSignAndVerify() throws IOException {
        final KeyPair keyPair = SignatureUtil.createKeyPair();
        final PrivateKey privateKey = keyPair.getPrivate();
        final PublicKey publicKey = keyPair.getPublic();
        final String content = "content";
        final String signature = SignatureUtil.signPackage(IOUtils.toInputStream(content, "UTF-8"), privateKey);

        assertThat(signature).isNotNull();
        assertThat(SignatureUtil.verifySignature(IOUtils.toInputStream(content, "UTF-8"), publicKey, signature))
                .isTrue();
    }

    @Test
    public void testPrivateKeyToBytes() {
        final KeyPair keyPair = SignatureUtil.createKeyPair();
        final PrivateKey privateKey = keyPair.getPrivate();
        final PrivateKey parsedKey = SignatureUtil.getPrivateKeyFromBytes(privateKey.getEncoded());

        assertThat(privateKey.getEncoded()).isEqualTo(parsedKey.getEncoded());
    }
}
