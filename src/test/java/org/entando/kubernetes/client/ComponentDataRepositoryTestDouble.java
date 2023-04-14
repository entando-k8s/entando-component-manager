package org.entando.kubernetes.client;

import jakarta.persistence.EntityNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.entando.kubernetes.model.bundle.ComponentType;
import org.entando.kubernetes.model.job.ComponentDataEntity;
import org.entando.kubernetes.repository.ComponentDataRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery.FetchableFluentQuery;

@Profile("mockjpa")
public class ComponentDataRepositoryTestDouble implements ComponentDataRepository {

    Map<UUID, ComponentDataEntity> database;

    public ComponentDataRepositoryTestDouble() {
        this.database = new ConcurrentHashMap<>();
    }


    @Override
    public Optional<ComponentDataEntity> findByComponentTypeAndComponentCode(ComponentType componentType,
            String componentCode) {
        return database.values().stream()
                .filter(d -> Objects.equals(componentType, d.getComponentType()) && Objects.equals(componentCode,
                        d.getComponentCode())).findFirst();
    }

    @Override
    public List<ComponentDataEntity> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public List<ComponentDataEntity> findAll(Sort sort) {
        return findAll();
    }

    @Override
    public Page<ComponentDataEntity> findAll(Pageable pageable) {
        List<ComponentDataEntity> j = this.database.values().stream()
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .collect(Collectors.toList());
        return new PageImpl<>(j, pageable, database.size());
    }

    @Override
    public <S extends ComponentDataEntity> List<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends ComponentDataEntity> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends ComponentDataEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends ComponentDataEntity> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends ComponentDataEntity, R> R findBy(Example<S> example,
            Function<FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public List<ComponentDataEntity> findAllById(Iterable<UUID> uuids) {
        List<UUID> ids = StreamSupport.stream(uuids.spliterator(), false).collect(Collectors.toList());
        return database.values().stream().filter(j -> ids.contains(j.getId())).collect(Collectors.toList());
    }

    @Override
    public long count() {
        return database.size();
    }

    @Override
    public <S extends ComponentDataEntity> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends ComponentDataEntity> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public void deleteById(UUID uuid) {
        this.database.remove(uuid);
    }

    @Override
    public void delete(ComponentDataEntity entity) {
        this.deleteById(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends UUID> iterable) {

    }

    @Override
    public void deleteAll(Iterable<? extends ComponentDataEntity> entities) {
        for (ComponentDataEntity e : entities) {
            this.delete(e);
        }
    }

    @Override
    public void deleteAll() {
        this.database = new ConcurrentHashMap<>();
    }

    @Override
    public <S extends ComponentDataEntity> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        UUID id = entity.getId();
        this.database.put(id, entity);
        return entity;
    }

    @Override
    public <S extends ComponentDataEntity> List<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S e : entities) {
            saved.add(this.save(e));

        }
        return saved;
    }

    @Override
    public Optional<ComponentDataEntity> findById(UUID uuid) {
        return Optional.ofNullable(database.get(uuid));
    }

    @Override
    public boolean existsById(UUID uuid) {
        return database.containsKey(uuid);
    }

    @Override
    public void flush() {

    }

    @Override
    public <S extends ComponentDataEntity> S saveAndFlush(S entity) {
        return this.save(entity);
    }

    @Override
    public <S extends ComponentDataEntity> List<S> saveAllAndFlush(Iterable<S> iterable) {
        return null;
    }

    @Override
    public void deleteInBatch(Iterable<ComponentDataEntity> entities) {
        for (ComponentDataEntity e : entities) {
            this.delete(e);
        }
    }

    @Override
    public void deleteAllInBatch(Iterable<ComponentDataEntity> iterable) {

    }

    @Override
    public void deleteAllInBatch() {
        this.deleteAll();
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<UUID> iterable) {

    }

    @Override
    public ComponentDataEntity getOne(UUID uuid) {
        if (!database.containsKey(uuid)) {
            throw new EntityNotFoundException("Entity with uuid " + uuid.toString() + " not found");
        }
        return database.get(uuid);
    }

    @Override
    public ComponentDataEntity getById(UUID uuid) {
        return null;
    }

    @Override
    public ComponentDataEntity getReferenceById(UUID uuid) {
        return null;
    }

}
