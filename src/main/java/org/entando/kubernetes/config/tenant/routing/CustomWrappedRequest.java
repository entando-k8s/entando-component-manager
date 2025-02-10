package org.entando.kubernetes.config.tenant.routing;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.util.*;

public class CustomWrappedRequest extends HttpServletRequestWrapper
{

    private final String virtualContextPath;

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

    @Override
    public String getRequestURI() {
        return stripVirtualContextIfRequired(super.getRequestURI());
    }

    @Override
    public StringBuffer getRequestURL() {
        StringBuffer res = new StringBuffer();
        return res.append(stripVirtualContextIfRequired(super.getRequestURL().toString()));
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
}
