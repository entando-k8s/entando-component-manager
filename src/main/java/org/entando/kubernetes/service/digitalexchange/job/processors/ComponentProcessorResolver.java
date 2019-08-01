package org.entando.kubernetes.service.digitalexchange.job.processors;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.ResolvableType;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ComponentProcessorResolver implements ApplicationContextAware {

    private Map<Class<?>, ComponentProcessor<?>> processorMap = new HashMap<>();

    @SuppressWarnings("unchecked")
    public <T> ComponentProcessor<T> resolve(final Class<T> clazz) {
        return (ComponentProcessor<T>) processorMap.get(clazz);
    }

    @Override
    public void setApplicationContext(final ApplicationContext context) throws BeansException {
        final Map<String, ComponentProcessor> beans = context.getBeansOfType(ComponentProcessor.class);
        beans.values().forEach(processor -> {
            final ResolvableType resolvableType = ResolvableType.forClass(processor.getClass()).as(ComponentProcessor.class);
            processorMap.put(resolvableType.getGeneric(0).getRawClass(), processor);
        });
    }
}
