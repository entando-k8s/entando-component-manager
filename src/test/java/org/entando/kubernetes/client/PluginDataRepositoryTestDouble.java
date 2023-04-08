package org.entando.kubernetes.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import org.apache.commons.lang.NotImplementedException;
import org.entando.kubernetes.model.job.PluginDataEntity;
import org.entando.kubernetes.repository.PluginDataRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

@Profile("mockjpa")
public class PluginDataRepositoryTestDouble implements PluginDataRepository {

    Map<UUID, PluginDataEntity> database;

    public PluginDataRepositoryTestDouble() {
        this.database = new ConcurrentHashMap<>();
    }

    @Override
    public long deleteByPluginCode(String pluginCode) {
        return database.entrySet().stream()
                .map(e -> {
                    if (e.getValue().getPluginCode().equals(pluginCode)) {
                        database.remove(e.getKey());
                        return 1;
                    }
                    return 0;
                })
                .reduce(0, Integer::sum);
    }

    @Override
    public Optional<PluginDataEntity> findByBundleIdAndPluginName(String bundleId, String pluginName) {
        return database.values().stream()
                .filter(p -> p.getBundleId().equals(bundleId) && p.getPluginName().equals(pluginName))
                .findFirst();
    }

    @Override
    public List<PluginDataEntity> findAllByBundleId(String bundleId) {
        throw new NotImplementedException();
    }

    @Override
    public List<PluginDataEntity> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public List<PluginDataEntity> findAll(Sort sort) {
        throw new NotImplementedException();
    }

    @Override
    public Page<PluginDataEntity> findAll(Pageable pageable) {
        throw new NotImplementedException();
    }

    @Override
    public <S extends PluginDataEntity> List<S> findAll(Example<S> example) {
        throw new NotImplementedException();
    }

    @Override
    public <S extends PluginDataEntity> List<S> findAll(Example<S> example, Sort sort) {
        throw new NotImplementedException();
    }

    @Override
    public <S extends PluginDataEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new NotImplementedException();
    }

    @Override
    public <S extends PluginDataEntity, R> R findBy(Example<S> example,
            Function<FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public List<PluginDataEntity> findAllById(Iterable<UUID> uuids) {
        throw new NotImplementedException();
    }

    @Override
    public long count() {
        return database.size();
    }

    @Override
    public <S extends PluginDataEntity> long count(Example<S> example) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteById(UUID uuid) {
        throw new NotImplementedException();
    }

    @Override
    public void delete(PluginDataEntity entity) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteAllById(Iterable<? extends UUID> uuids) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteAll(Iterable<? extends PluginDataEntity> entities) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteAll() {
        throw new NotImplementedException();
    }

    @Override
    public <S extends PluginDataEntity> S save(S entity) {
        if (entity.getId() == null) {
            entity.generateId();
        }
        database.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public <S extends PluginDataEntity> List<S> saveAll(Iterable<S> entities) {
        throw new NotImplementedException();
    }

    @Override
    public Optional<PluginDataEntity> findById(UUID uuid) {
        throw new NotImplementedException();
    }

    @Override
    public boolean existsById(UUID uuid) {
        throw new NotImplementedException();
    }

    @Override
    public void flush() {
        throw new NotImplementedException();
    }

    @Override
    public <S extends PluginDataEntity> S saveAndFlush(S entity) {
        throw new NotImplementedException();
    }

    @Override
    public <S extends PluginDataEntity> List<S> saveAllAndFlush(Iterable<S> entities) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteAllInBatch(Iterable<PluginDataEntity> entities) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteAllInBatch() {
        throw new NotImplementedException();
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<UUID> uuids) {
        throw new NotImplementedException();
    }

    @Override
    public PluginDataEntity getOne(UUID uuid) {
        throw new NotImplementedException();
    }

    @Override
    public PluginDataEntity getById(UUID uuid) {
        throw new NotImplementedException();
    }

    @Override
    public PluginDataEntity getReferenceById(UUID uuid) {
        return null;
    }

    @Override
    public <S extends PluginDataEntity> Optional<S> findOne(Example<S> example) {
        throw new NotImplementedException();
    }

    @Override
    public <S extends PluginDataEntity> boolean exists(Example<S> example) {
        throw new NotImplementedException();
    }
}
