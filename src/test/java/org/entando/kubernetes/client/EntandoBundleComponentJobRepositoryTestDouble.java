package org.entando.kubernetes.client;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.persistence.EntityNotFoundException;
import org.entando.kubernetes.model.job.EntandoBundleComponentJobEntity;
import org.entando.kubernetes.model.job.EntandoBundleJobEntity;
import org.entando.kubernetes.model.job.JobStatus;
import org.entando.kubernetes.repository.EntandoBundleComponentJobRepository;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class EntandoBundleComponentJobRepositoryTestDouble implements EntandoBundleComponentJobRepository {

    Map<UUID, EntandoBundleComponentJobEntity> database;

    public EntandoBundleComponentJobRepositoryTestDouble() {
        this.database = new ConcurrentHashMap<>();
    }


    @Override
    public List<EntandoBundleComponentJobEntity> findAllByParentJob(EntandoBundleJobEntity job) {
        return this.database.values().stream().filter(j -> j.getParentJob().equals(job)).collect(Collectors.toList());
    }

    @Override
    public List<EntandoBundleComponentJobEntity> findAllByParentJobId(UUID id) {
        return this.database.values().stream().filter(j -> j.getParentJob().getId().equals(id))
                .collect(Collectors.toList());
    }

    @Override
    public void updateJobStatus(UUID id, JobStatus status) {
        this.database.computeIfPresent(id, (k, v) -> {
            v.setStatus(status);
            return v;
        });
    }

    @Override
    public void updateJobStatus(UUID id, JobStatus status, String errorMessage) {
        this.database.computeIfPresent(id, (k, v) -> {
            v.setStatus(status);
            v.setErrorMessage(errorMessage);
            return v;
        });
    }

    @Override
    public void updateStartedAt(UUID id, LocalDateTime datetime) {
        this.database.computeIfPresent(id, (k, v) -> {
            v.setStartedAt(datetime);
            return v;
        });
    }

    @Override
    public void updateFinishedAt(UUID id, LocalDateTime datetime) {
        this.database.computeIfPresent(id, (k, v) -> {
            v.setFinishedAt(datetime);
            return v;
        });
    }

    @Override
    public List<EntandoBundleComponentJobEntity> findAll() {
        return new ArrayList<>(database.values());
    }

    @Override
    public List<EntandoBundleComponentJobEntity> findAll(Sort sort) {
        return findAll();
    }

    @Override
    public Page<EntandoBundleComponentJobEntity> findAll(Pageable pageable) {
        List<EntandoBundleComponentJobEntity> j = this.database.values().stream()
                .skip(pageable.getOffset()).limit(pageable.getPageSize())
                .collect(Collectors.toList());
        return new PageImpl<>(j, pageable, database.size());
    }

    @Override
    public <S extends EntandoBundleComponentJobEntity> List<S> findAll(Example<S> example) {
        return null;
    }

    @Override
    public <S extends EntandoBundleComponentJobEntity> List<S> findAll(Example<S> example, Sort sort) {
        return null;
    }

    @Override
    public <S extends EntandoBundleComponentJobEntity> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public List<EntandoBundleComponentJobEntity> findAllById(Iterable<UUID> uuids) {
        List<UUID> ids = StreamSupport.stream(uuids.spliterator(), false).collect(Collectors.toList());
        return database.values().stream().filter(j -> ids.contains(j.getId())).collect(Collectors.toList());
    }

    @Override
    public long count() {
        return database.size();
    }

    @Override
    public <S extends EntandoBundleComponentJobEntity> long count(Example<S> example) {
        return 0;
    }

    @Override
    public void deleteById(UUID uuid) {
        this.database.remove(uuid);
    }

    @Override
    public void delete(EntandoBundleComponentJobEntity entity) {
        this.deleteById(entity.getId());
    }

    @Override
    public void deleteAll(Iterable<? extends EntandoBundleComponentJobEntity> entities) {
        for (EntandoBundleComponentJobEntity e : entities) {
            this.delete(e);
        }
    }

    @Override
    public void deleteAll() {
        this.database = new ConcurrentHashMap<>();
    }

    @Override
    public <S extends EntandoBundleComponentJobEntity> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        UUID id = entity.getId();
        this.database.put(id, entity);
        return entity;
    }

    @Override
    public <S extends EntandoBundleComponentJobEntity> List<S> saveAll(Iterable<S> entities) {
        List<S> saved = new ArrayList<>();
        for (S e : entities) {
            saved.add(this.save(e));

        }
        return saved;
    }

    @Override
    public Optional<EntandoBundleComponentJobEntity> findById(UUID uuid) {
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
    public <S extends EntandoBundleComponentJobEntity> S saveAndFlush(S entity) {
        return this.save(entity);
    }

    @Override
    public void deleteInBatch(Iterable<EntandoBundleComponentJobEntity> entities) {
        for (EntandoBundleComponentJobEntity e : entities) {
            this.delete(e);
        }
    }

    @Override
    public void deleteAllInBatch() {
        this.deleteAll();
    }

    @Override
    public EntandoBundleComponentJobEntity getOne(UUID uuid) {
        if (!database.containsKey(uuid)) {
            throw new EntityNotFoundException("Entity with uuid " + uuid.toString() + " not found");
        }
        return database.get(uuid);
    }

    @Override
    public <S extends EntandoBundleComponentJobEntity> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends EntandoBundleComponentJobEntity> boolean exists(Example<S> example) {
        return false;
    }
}
