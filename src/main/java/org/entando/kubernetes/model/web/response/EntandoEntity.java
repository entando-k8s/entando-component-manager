package org.entando.kubernetes.model.web.response;

/**
 * Don't use this class.
 * @deprecated use {@link SimpleRestResponse} instead.
 */
@Deprecated
public class EntandoEntity<T> extends SimpleRestResponse<T> {

    public EntandoEntity(final T payload) {
        super(payload);
    }

    public EntandoEntity() {
        super();
    }

}
