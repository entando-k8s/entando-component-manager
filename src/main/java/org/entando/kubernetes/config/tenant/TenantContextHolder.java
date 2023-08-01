package org.entando.kubernetes.config.tenant;

import org.springframework.stereotype.Component;

@Component
public class TenantContextHolder {
    private static final InheritableThreadLocal<TenantContext> holder = new InheritableThreadLocal<>();

    public void set(TenantContext context) {
        holder.set(context);
    }

    public TenantContext get() {
        return holder.get();
    }

    public void remove() {
        holder.remove();
    }
}
