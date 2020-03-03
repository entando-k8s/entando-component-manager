package org.entando.kubernetes.service.digitalexchange.signature;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("unit")
public class SignatureUtilTest {

    @Test
    public void testPublicKeyToPEMAndBack() {
        final KeyPair keyPair = SignatureUtil.createKeyPair();
        final PublicKey publicKey = keyPair.getPublic();
        final String pemPublicKey = SignatureUtil.publicKeyToPEM(publicKey);
        final PublicKey parsedKey = SignatureUtil.publicKeyFromPEM(pemPublicKey);

        assertThat(publicKey.getEncoded()).isEqualTo(parsedKey.getEncoded());
    }

    @Test
    public void testPrivateKeyToPEMAndBack() {
        final KeyPair keyPair = SignatureUtil.createKeyPair();
        final PrivateKey privateKey = keyPair.getPrivate();
        final String pemPrivateKey = SignatureUtil.privateKeyToPEM(privateKey);
        final PrivateKey parsedKey = SignatureUtil.privateKeyFromPEM(pemPrivateKey);

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
