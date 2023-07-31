package org.entando.kubernetes.config.tenant;

import com.google.common.net.HttpHeaders;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
public final class UrlUtils {

    public static final String ENTANDO_APP_USE_TLS = "ENTANDO_APP_USE_TLS";
    public static final String ENTANDO_APP_ENGINE_EXTERNAL_PORT = "ENTANDO_APP_ENGINE_EXTERNAL_PORT";
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";

    private UrlUtils(){}

    public static String fetchScheme(HttpServletRequest request){
        return getProtoFromEnv().filter(UrlUtils::isHttps)
                .orElseGet(() -> getProtoFromXHeader(request).filter(UrlUtils::isHttps).orElse(request.getScheme()));
    }

    private static Optional<String> getProtoFromEnv(){
        if( BooleanUtils.toBoolean(System.getenv(ENTANDO_APP_USE_TLS)) ) {
            return Optional.ofNullable(HTTPS_SCHEME);
        } else {
            return Optional.ofNullable(HTTP_SCHEME);
        }
    }

    private static Optional<String> getProtoFromXHeader(HttpServletRequest request){
        return Optional.ofNullable(request.getHeader(HttpHeaders.X_FORWARDED_PROTO)).filter(StringUtils::isNotBlank);
    }

    private static boolean isHttps(String proto){
        return HTTPS_SCHEME.equals(proto);
    }

    public static String fetchServer(HttpServletRequest request){
        return getHostFromXHeader(request)
                .orElseGet(() -> getHostFromHeader(request).orElse(request.getServerName()));
    }

    private static Optional<String> getHostFromXHeader(HttpServletRequest request){
        return Optional.ofNullable(request.getHeader(HttpHeaders.X_FORWARDED_HOST)).filter(StringUtils::isNotBlank);
    }

    private static Optional<String> getHostFromHeader(HttpServletRequest request){
        return Optional.ofNullable(request.getHeader(HttpHeaders.HOST))
                .filter(StringUtils::isNotBlank)
                .filter(s -> s.startsWith(request.getServerName()))
                .map(s -> s.split(":")[0]);
    }

    public static Optional<Integer> fetchPort(HttpServletRequest request) {
        Integer port = getPortFromEnv().orElseGet(() -> getPortFromXHeader(request)
                .orElseGet(() -> getPortFromHeader(request)
                        .orElseGet(() -> getPortServlet(request))));
        return Optional.ofNullable(port);
    }

    private static Optional<Integer> getPortFromEnv(){
        String port = System.getenv(ENTANDO_APP_ENGINE_EXTERNAL_PORT);
        if( NumberUtils.isParsable(port)) {
            return Optional.of(NumberUtils.toInt(port));
        } else {
            return Optional.empty();
        }
    }

    private static Optional<Integer> getPortFromXHeader(HttpServletRequest request){
        return Optional.ofNullable(request.getHeader(HttpHeaders.X_FORWARDED_PORT)).filter(StringUtils::isNotBlank).map(Integer::valueOf);
    }

    private static Optional<Integer> getPortFromHeader(HttpServletRequest request){
        return Optional.ofNullable(request.getHeader(HttpHeaders.HOST))
                .filter(StringUtils::isNotBlank)
                .filter(s -> s.startsWith(request.getServerName()))
                .filter(UrlUtils::hostHeaderContainsPort)
                .map(host -> host.split(":")[1])
                .map(Integer::valueOf);
    }

    private static boolean hostHeaderContainsPort(String header) {
        return header.contains(":") && header.split(":").length > 1;
    }


    private static Integer getPortServlet(HttpServletRequest request) {
        return request.getServerPort() == 0 ? null : request.getServerPort();
    }

    public static Optional<String> fetchServerNameFromUri(String uri){
        return Optional.ofNullable(uri)
                .map(u -> getUri(u))
                .map(URI::getHost)
                .filter(StringUtils::isNotBlank);
    }

    public static Optional<String> fetchPathFromUri(String uri){
        return Optional.ofNullable(uri)
                .map(u -> getUri(u))
                .map(URI::getPath)
                .filter(StringUtils::isNotBlank);
    }

    public static Optional<String> removeContextRootFromPath(String path, HttpServletRequest request){
        return Optional.ofNullable(path).filter(StringUtils::isNotBlank).map(s -> {
            if(s.startsWith(request.getContextPath())){
                return s.replaceFirst(request.getContextPath(), "");
            }
            return s;
        }).filter(StringUtils::isNotBlank);
    }


    private static URI getUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException ex) {
            log.debug("error with url:'{}'", url, ex);
            return null;
        }
    }

    public static URL composeBaseUrl(HttpServletRequest request){
        String reqScheme = UrlUtils.fetchScheme(request);
        String serverName = UrlUtils.fetchServer(request);
        Optional<Integer> port = UrlUtils.fetchPort(request);

        return generateUrl(reqScheme, serverName, port, request);
    }

    private static URL generateUrl(String reqScheme, String serverName, Optional<Integer> port, HttpServletRequest request) {
        try {
            URL baseUrl = null;
            if ( port.isEmpty() || isStandardHttp(reqScheme, port.get()) || isStandardHttps(reqScheme, port.get()) || forcedHttps(request, port.get()) ) {
                baseUrl = new URL(reqScheme, serverName, "");
            } else {
                baseUrl = new URL(reqScheme, serverName, port.get(), "");
            }
            return baseUrl;

        } catch(Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static boolean isStandardHttp(String reqScheme, int port) {
        return HTTP_SCHEME.equals(reqScheme) && 80 == port;
    }

    private static boolean isStandardHttps(String reqScheme, int port) {
        return HTTPS_SCHEME.equals(reqScheme) && 443 == port;
    }

    private static boolean forcedHttps(HttpServletRequest request, int port) {
        return (HTTPS_SCHEME.equals(getProtoFromXHeader(request).orElse("")) && isStandardPort(port) )||
                (HTTPS_SCHEME.equals(getProtoFromEnv().orElse("")) && isStandardPort(port) ) ;
    }

    private static boolean isStandardPort(int port) {
        return 443 == port || 80 == port;
    }

    public static class EntUrlBuilder {
        private String url;
        private List<String> paths = new ArrayList<>();

        private EntUrlBuilder(){}

        public static EntUrlBuilder builder() {
            return new EntUrlBuilder();
        }

        public EntUrlBuilder url(String u){
            this.url = u;
            return this;
        }

        public EntUrlBuilder url(URI u){
            this.url = u.toString();
            return this;
        }

        public EntUrlBuilder path(String p){
            paths.add(p);
            return this;
        }

        public EntUrlBuilder paths(String ... u){
            paths.addAll(Arrays.asList(u));
            return this;
        }

        public URI build() {
            UriBuilder builder = UriComponentsBuilder.fromUriString(url);
            for (String path : paths) {
                if (StringUtils.isNotBlank(path)) {
                    path = path.trim();
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    builder.path(path);
                }
            }
            return builder.build();
        }
    }

}