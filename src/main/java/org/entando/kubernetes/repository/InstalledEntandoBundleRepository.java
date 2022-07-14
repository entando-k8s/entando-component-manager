package org.entando.kubernetes.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.commons.lang3.StringUtils;
import org.entando.kubernetes.model.job.EntandoBundleEntity;
import org.entando.kubernetes.service.digitalexchange.BundleUtilities;
import org.entando.kubernetes.validator.ValidationFunctions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface InstalledEntandoBundleRepository extends JpaRepository<EntandoBundleEntity, UUID> {

    @Transactional
    void deleteByBundleCode(String bundleCode);

    @Transactional
    List<EntandoBundleEntity> findAllByRepoUrlIn(List<String> repoUrls);

    @Transactional
    Optional<EntandoBundleEntity> findFirstByRepoUrl(String repoUrl);

    @Transactional
    List<EntandoBundleEntity> findAllByName(String name);

    @Transactional
    boolean existsByBundleCode(String bundleCode);

    @Transactional
    Optional<EntandoBundleEntity> findByBundleCode(String bundleCode);

    // TECH-DEBT: to search with the bundleId in the table installed_entando_bundles that does not contain it
    // and doesn't have a FK to PluginDataEntity I calculate the bundleId on the fly from the repoUrl
    default Optional<EntandoBundleEntity> findByBundleId(String bundleId) {
        return findAll().stream()
                .filter(b -> b.isInstalled() && StringUtils.equals(bundleId,
                        BundleUtilities.removeProtocolAndGetBundleId(
                                ValidationFunctions.composeCommonUrlOrThrow(b.getRepoUrl(), "", ""))))
                .findFirst();
    }

}
