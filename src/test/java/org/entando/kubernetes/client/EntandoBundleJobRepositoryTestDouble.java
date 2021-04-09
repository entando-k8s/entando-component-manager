package org.entando.kubernetes.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.EntityNotFoundException;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.repository.EntandoBundleJobRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@Profile("mockjpa")
public class EntandoBundleJobRepositoryTestDouble implements EntandoBundleJobRepository {

    Map<UUID, EntandoBundleJobEntity> database;

    public EntandoBundleJobRepositoryTestDouble() {
        this.database = new ConcurrentHashMap<>();
    }


    @Override
    public List<EntandoBundleJobEntity> findAllByOrderByStartedAtDesc() {
        return this.database.values()
                .stream()
                .sorted(Comparator.comparing(EntandoBundleJobEntity::getStartedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<EntandoBundleJobEntity> findAllByComponentIdOrderByStartedAtDesc(String componentId) {
        return this.database.values()
                .stream()
                .filter(j -> j.getComponentId().equals(componentId))
                .sorted(Comparator.comparing(EntandoBundleJobEntity::getStartedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public Optional<EntandoBundleJobEntity> findFirstByComponentIdAndStatusInOrderByStartedAtDesc(String componentId,
            Set<JobStatus> status) {
        return this.database.values()
                .stream()
                .filter(j -> j.getComponentId().equals(componentId) && status.contains(j.getStatus()))
                .max(Comparator.comparing(EntandoBundleJobEntity::getStartedAt));
    }

    @Override
    public Optional<EntandoBundleJobEntity> findFirstByComponentIdAndStatusOrderByStartedAtDesc(String componentId,
            JobStatus status) {
        return this.database.values()
                .stream()
                .filter(j -> j.getComponentId().equals(componentId) && j.getStatus().equals(status))
                .max(Comparator.comparing(EntandoBundleJobEntity::getStartedAt));
    }

    @Override
    public Optional<EntandoBundleJobEntity> findFirstByComponentIdOrderByStartedAtDesc(String componentId) {
        return this.database.values()
                .stream()
                .filter(j -> j.getComponentId().equals(componentId))
                .max(Comparator.comparing(EntandoBundleJobEntity::getStartedAt));
    }

    @Override
    public void updateJobStatus(UUID id, JobStatus status) {
        this.database.computeIfPresent(id, (k, v) -> {
            v.setStatus(status);
            return v;
        });
    }

    @Override
    public Optional<List<EntandoBundleJobEntity>> findEntandoBundleJobEntityByIdIn(
            Set<UUID> componentIdList) {
        return Optional.of(new ArrayList<>(this.database.values()));
    }

    @Override
    public List<EntandoBundleJobEntity> findAll() {
        return new ArrayList<>(this.database.values());
    }

    @Override
    public List<EntandoBundleJobEntity> findAll(Sort sort) {
        return new ArrayList<>(this.database.values());
    }

    @Override
    public Page<EntandoBundleJobEntity> findAll(Pageable pageable) {
        List<EntandoBundleJobEntity> jobs = this.database.values()
                .stream()
                .skip(pageable.getOffset())
                .limit(pageable.getPageSize())
                .collect(Collectors.toList());
        return new PageImpl<>(jobs, pageable, this.database.size());
    }

    @Override
    public <S extends EntandoBundleJobEntity> List<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends EntandoBundleJobEntity> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends EntandoBundleJobEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public List<EntandoBundleJobEntity> findAllById(Iterable<UUID> uuids) {
        List<UUID> ids = StreamSupport.stream(uuids.spliterator(), false).collect(Collectors.toList());
        return this.database.values().stream().filter(j -> ids.contains(j.getId())).collect(Collectors.toList());
    }

    @Override
    public long count() {
        return this.database.size();
    }

    @Override
    public <S extends EntandoBundleJobEntity> long count(Example<S> example) {
        return 0;
    }

    @Override
    public void deleteById(UUID uuid) {
        this.database.remove(uuid);
    }

    @Override
    public void delete(EntandoBundleJobEntity entity) {
        if (entity.getId() != null) {
            this.deleteById(entity.getId());
        }
    }

    @Override
    public void deleteAll(Iterable<? extends EntandoBundleJobEntity> entities) {
        for (EntandoBundleJobEntity e : entities) {
            this.delete(e);
        }
    }

    @Override
    public void deleteAll() {
        this.database = new ConcurrentHashMap<>();
    }

    @Override
    public <S extends EntandoBundleJobEntity> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        UUID id = entity.getId();
        EntandoBundleJobEntity oldEntity = this.database.remove(id);
        EntandoBundleJobEntity newEntity = entity.clone();
        this.database.put(id, newEntity);
        return (S) newEntity;
    }

    @Override
    public <S extends EntandoBundleJobEntity> List<S> saveAll(Iterable<S> entities) {
        List<S> savedEntities = new ArrayList<>();
        for (S e : entities) {
            savedEntities.add(this.save(e));
        }
        return savedEntities;
    }

    @Override
    public Optional<EntandoBundleJobEntity> findById(UUID uuid) {
        return Optional.ofNullable(this.database.get(uuid));
    }

    @Override
    public boolean existsById(UUID uuid) {
        return this.database.containsKey(uuid);
    }

    @Override
    public void flush() {

    }

    @Override
    public <S extends EntandoBundleJobEntity> S saveAndFlush(S entity) {
        return this.save(entity);
    }

    @Override
    public void deleteInBatch(Iterable<EntandoBundleJobEntity> entities) {
        for (EntandoBundleJobEntity e : entities) {
            this.delete(e);
        }
    }

    @Override
    public void deleteAllInBatch() {
        this.database = new ConcurrentHashMap<>();
    }

    @Override
    public EntandoBundleJobEntity getOne(UUID uuid) {
        if (this.database.containsKey(uuid)) {
            return this.database.get(uuid);
        }
        throw new EntityNotFoundException("Entity with uuid " + uuid.toString() + " not found in the database");
    }

    @Override
    public <S extends EntandoBundleJobEntity> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends EntandoBundleJobEntity> boolean exists(Example<S> example) {
        return false;
    }

}
