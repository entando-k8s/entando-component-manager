package org.entando.kubernetes.repository;

import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InstalledEntandoBundleRepository extends JpaRepository<EntandoBundleEntity, String> {

    @Transactional
    void deleteById(String id);

    List<EntandoBundleEntity> findAllByRepoUrlIn(List<String> repoUrls);

    default List<EntandoBundleEntity> findAllByRepoUrlInWithURL(List<URL> repoUrls) {
        final List<String> repoUrlStrings = repoUrls.stream()
                .map(URL::toString)
                .collect(Collectors.toList());
        return findAllByRepoUrlIn(repoUrlStrings);
    }
}
