package org.entando.kubernetes.service.digitalexchange.entandocore.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.service.digitalexchange.job.model.ContentModelDescriptor;

@Data
@NoArgsConstructor
public class EntandoCoreContentModel {

    private String id;
    private String contentType;
    private String descr;
    private String contentShape;

    public EntandoCoreContentModel(final ContentModelDescriptor descriptor) {
        this.id = descriptor.getId();
        this.contentType = descriptor.getContentType();
        this.descr = descriptor.getDescription();
        this.contentShape = descriptor.getContentShape();
    }

}
