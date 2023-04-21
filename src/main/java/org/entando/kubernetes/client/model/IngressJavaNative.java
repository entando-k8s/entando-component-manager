package org.entando.kubernetes.client.model;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.extensions.IngressSpec;
import io.fabric8.kubernetes.api.model.extensions.IngressStatus;
import lombok.Data;

@Data
public class IngressJavaNative {

    ObjectMeta metadata;
    IngressSpec spec;
    IngressStatus status;

}
