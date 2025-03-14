package org.entando.kubernetes.config.tenant.routing;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.MappingMatch;
import java.util.*;

public class CustomWrappedRequest extends HttpServletRequestWrapper {

    public static final String VIRTUAL_CONTEXT = "virtual-context";
    private final String virtualContextPath;
    private Map<String, String> headersOverrides = new HashMap<>();

    /**
     * Create a new request wrapper that will merge additional parameters into
     * the request object without prematurely reading parameters from the
     * original request.
     */
    public CustomWrappedRequest(
            final HttpServletRequest request,
            String virtualContextPath,
            final Map<String, String[]> additionalParams
    ) {
        super(request);
        this.virtualContextPath = ((virtualContextPath.startsWith("/")) ? "" : "/") + virtualContextPath;
    }

    @Override
    public String getContextPath() {
        return (virtualContextPath != null)
                ? virtualContextPath
                : getOriginalContextPath();
    }

    @Override
    public String getServletPath() {
        return stripVirtualContextIfRequired(this.getOriginalServletPath());
    }

    public boolean hasVirtualContext() {
        return virtualContextPath != null;
    }

    public String getOriginalContextPath() {
        return super.getContextPath();
    }

    public String getOriginalServletPath() {
        return super.getServletPath();
    }

    private String stripVirtualContextIfRequired(String path) {
        return (virtualContextPath != null)
                ? path.replaceFirst("^" + virtualContextPath + "/", "/")
                : path;
    }

    public static CustomWrappedRequest getCustomizedRequest(ServletRequest request) {
        if (request instanceof CustomWrappedRequest) {
            return (CustomWrappedRequest) request;
        } else if (request instanceof HttpServletRequestWrapper) {
            ServletRequest child = ((HttpServletRequestWrapper) request).getRequest();
            return (request == child) ? null : getCustomizedRequest(child);
        } else {
            return null;
        }
    }

    /**
     * As for some reason "alwaysUseFullPath" seems to be set to try, spring is matching the routes against the
     * full request path (instead of the servletPath) and the below change in getMappingMatch is the workaround
     * I've found by checking the code.
     */
    @Override
    public HttpServletMapping getHttpServletMapping() {
        HttpServletMapping res = super.getHttpServletMapping();
        return new HttpServletMapping() {
            public String getMatchValue() {
                return res.getMatchValue();
            }

            public String getPattern() {
                return res.getPattern();
            }

            public String getServletName() {
                return res.getServletName();
            }

            public MappingMatch getMappingMatch() {
                return MappingMatch.PATH;
            }
        };
    }
}
