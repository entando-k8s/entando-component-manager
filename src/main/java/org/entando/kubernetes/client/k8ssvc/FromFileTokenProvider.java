package org.entando.kubernetes.client.k8ssvc;

import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.time.Instant;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken.TokenType;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;

public final class FromFileTokenProvider {

    private String value;

    private FromFileTokenProvider(String v) {
        value = v;
    }

    public static FromFileTokenProvider getInstance(Path tokenFileUri) {
        return new FromFileTokenProvider(readValue(tokenFileUri));
    }


    public OAuth2AccessToken getAccessToken() {
        JWT jwt = readToken();
        return new OAuth2AccessToken(TokenType.BEARER, value, getIat(jwt), getExp(jwt));
    }

    public OAuth2RefreshToken getRefreshAccessToken() {
        JWT jwt = readToken();
        return new OAuth2RefreshToken(value, getIat(jwt), getExp(jwt));
    }

    /**
     * read the token from the set file.
     *
     * @return the OAuth2AccessToken built using the token read from the set file
     */
    private static String readValue(Path tokenFileUri) {
        try {
            return Files.readString(tokenFileUri).trim();
        } catch (IOException e) {
            throw new EntandoComponentManagerException(String
                    .format("Issues retrieving service account token from %s", tokenFileUri), e);
        }
    }

    private JWT readToken() {
        try {
            return JWTParser.parse(value);
        } catch (ParseException e) {
            throw new EntandoComponentManagerException(String
                    .format("Issues retrieving jwt token from %s", value), e);
        }
    }

    private Instant getIat(JWT jwt) {
        try {
            return jwt.getJWTClaimsSet().getIssueTime().toInstant();
        } catch (ParseException e) {
            throw new EntandoComponentManagerException(String
                    .format("Issues retrieving iat from jwt token"), e);
        }
    }

    private Instant getExp(JWT jwt) {
        try {
            return jwt.getJWTClaimsSet().getExpirationTime().toInstant();
        } catch (ParseException e) {
            throw new EntandoComponentManagerException(String
                    .format("Issues retrieving iat from jwt token"), e);
        }
    }

}
