package org.entando.kubernetes.model.bundle.descriptor;

public interface Descriptor {

    default String getDescriptorClassName() {
        return this.getClass().getSimpleName().replace(".class", "");
    }

    ComponentKey getComponentKey();
}
