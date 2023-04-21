package org.entando.kubernetes.client.model;

import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.Data;
import org.entando.kubernetes.model.common.EntandoCustomResourceStatus;
import org.entando.kubernetes.model.link.EntandoAppPluginLinkSpec;

@Data
public class EntandoAppPluginLinkJavaNative {

    ObjectMeta metadata;
    EntandoAppPluginLinkSpec spec;
    EntandoCustomResourceStatus status;

}
