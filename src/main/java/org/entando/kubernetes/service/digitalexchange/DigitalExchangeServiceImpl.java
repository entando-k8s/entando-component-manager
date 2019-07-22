package org.entando.kubernetes.service.digitalexchange;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.entando.kubernetes.model.digitalexchange.DigitalExchangeEntity;
import org.entando.kubernetes.repository.DigitalExchangeRepository;
import org.entando.kubernetes.service.digitalexchange.client.DigitalExchangesClient;
import org.entando.kubernetes.service.digitalexchange.client.SimpleDigitalExchangeCall;
import org.entando.kubernetes.service.digitalexchange.model.DigitalExchange;
import org.entando.web.exception.NotFoundException;
import org.entando.web.response.EntandoEntity;
import org.entando.web.response.RestError;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DigitalExchangeServiceImpl implements DigitalExchangesService {

    private final @NonNull DigitalExchangeRepository repository;
    private final @NonNull DigitalExchangesClient client;

    @Override
    public List<DigitalExchange> getDigitalExchanges() {
        return repository.findAll().stream()
                .map(DigitalExchangeEntity::convert)
                .collect(Collectors.toList());
    }

    @Override
    public DigitalExchange findById(final String id) {
        return repository.findById(UUID.fromString(id))
                .map(DigitalExchangeEntity::convert)
                .orElseThrow(() -> new NotFoundException("org.entando.digitalExchange.notFound"));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
    public DigitalExchange create(final DigitalExchange digitalExchange) {
        // TODO validate
        return repository.save(new DigitalExchangeEntity(digitalExchange)).convert();
    }

    @Override
    public DigitalExchange update(final DigitalExchange digitalExchange) {
        final DigitalExchangeEntity digitalExchangeEntity = repository.findById(UUID.fromString(digitalExchange.getId()))
                .orElseThrow(() -> new NotFoundException("org.entando.digitalExchange.notFound"));

        // TODO validate
        digitalExchangeEntity.apply(digitalExchange);
        repository.save(digitalExchangeEntity);
        return digitalExchangeEntity.convert();
    }

    @Override
    public void delete(final String digitalExchangeId) {
        final DigitalExchangeEntity digitalExchangeEntity = repository.findById(UUID.fromString(digitalExchangeId))
                .orElseThrow(() -> new NotFoundException("org.entando.digitalExchange.notFound"));
        repository.delete(digitalExchangeEntity);
    }

    @Override
    public List<RestError> test(final String digitalExchangeId) {
        return test(findById(digitalExchangeId));
    }

    private List<RestError> test(final DigitalExchange digitalExchange) {
        final SimpleDigitalExchangeCall<Map<String, List<RestError>>> call = new SimpleDigitalExchangeCall<>(
                HttpMethod.GET, new ParameterizedTypeReference<EntandoEntity<Map<String, List<RestError>>>>() {
        }, "digitalExchange", "exchanges", "test");
        return client.getSingleResponse(digitalExchange, call).getErrors();
    }

    @Override
    public Map<String, List<RestError>> testAll() {
        final Map<String, List<RestError>> result = new HashMap<>();
        getDigitalExchanges().forEach(de -> result.put(de.getId(), test(de)));
        return result;
    }
}
