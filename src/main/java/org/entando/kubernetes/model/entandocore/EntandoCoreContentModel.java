package org.entando.kubernetes.model.entandocore;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.entando.kubernetes.model.bundle.descriptor.ContentTemplateDescriptor;

@Data
@NoArgsConstructor
public class EntandoCoreContentModel {

    private String id;
    private String contentType;
    private String descr;
    private String contentShape;

    public EntandoCoreContentModel(final ContentTemplateDescriptor descriptor) {
        this.id = descriptor.getId();
        this.contentType = descriptor.getContentType();
        this.descr = descriptor.getDescription();
        this.contentShape = descriptor.getContentShape();
    }

}
