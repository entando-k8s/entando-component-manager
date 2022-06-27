package org.entando.kubernetes.model.bundle.descriptor;

public interface Descriptor {

    default String getDescriptorClassName() {
        return this.getClass().getSimpleName().replace(".class", "");
    }

    ComponentKey getComponentKey();

    /**
     * Tells if a component is auxiliary
     * An auxiliary component is not meant to be used autonomously but instead together with another controller.
     * It's not installed by normal means and will not appear among the standard components.
     */
    default boolean isAuxiliary() {
        return false;
    }
}
