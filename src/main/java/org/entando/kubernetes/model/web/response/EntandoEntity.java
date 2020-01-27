package org.entando.kubernetes.model.web.response;

/**
 * @deprecated use {@link SimpleRestResponse} instead
 * @param <T>
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
