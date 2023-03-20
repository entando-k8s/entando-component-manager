package org.entando.kubernetes.client.hub.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Data
@NoArgsConstructor
public class BundleDto {

  @Schema(example = "bundle identifier")
  private String bundleId;

  @Schema(example = "bundle-sample")
  private String name;

  @Schema(example = "This is a example bundle")
  @Setter(AccessLevel.PUBLIC)
  protected String description;

  @Schema(example = "data:image/png;base64,base64code")
  @Setter(AccessLevel.PUBLIC)
  protected String descriptionImage;

  @Schema(example = "V5")
  private String descriptorVersion;

  @Schema(example = "docker://registry.hub.docker.com/organization/bundle-sample")
  private String gitRepoAddress;
  @Schema(example = "https://github.com/organization/bundle-sample")
  private String gitSrcRepoAddress;

  private List<String> dependencies;
  private List<String> bundleGroups; //Used for bundle group versions, need to make it bundleGroupVersions


  @Deprecated
  public BundleDto(String id, String name, String description, String gitRepoAddress, String gitSrcRepoAddress, List<String> dependencies, List<String> bundleGroupVersions, String descriptorVersion) {
    this.bundleId = id;
    this.name = name;
    this.description = description;
    this.gitRepoAddress = gitRepoAddress;
    this.gitSrcRepoAddress = gitSrcRepoAddress;
    this.dependencies = dependencies;
    this.bundleGroups = bundleGroupVersions;
    this.descriptorVersion = descriptorVersion;
  }

//  @Deprecated
//  public BundleDto(com.entando.hub.catalog.persistence.entity.Bundle entity) {
//    this.bundleId = entity.getId().toString();
//    this.name = entity.getName();
//    this.description = entity.getDescription();
//    this.gitRepoAddress = entity.getGitRepoAddress();
//    this.gitSrcRepoAddress = entity.getGitSrcRepoAddress();
//    this.dependencies = Arrays.asList(entity.getDependencies().split(","));
//    this.bundleGroups = entity.getBundleGroupVersions().stream().map(bundleGroupVersion -> bundleGroupVersion.getId().toString()).collect(Collectors.toList());
//    this.descriptorVersion = entity.getDescriptorVersion().toString();
//  }

/*
  public com.entando.hub.catalog.persistence.entity.Bundle createEntity(Optional<String> id) {
    com.entando.hub.catalog.persistence.entity.Bundle ret = new com.entando.hub.catalog.persistence.entity.Bundle();
    ret.setDescription(this.getDescription());
    ret.setName(this.getName());
    ret.setGitRepoAddress(this.getGitRepoAddress());
    ret.setGitSrcRepoAddress(this.getGitSrcRepoAddress());
    ret.setDependencies(String.join(",", this.getDependencies()));

    //for now, if the repo address does not start with docker, we assume it's a V1 bundle.
    boolean isDocker = (this.getGitRepoAddress() != null) && (this.getGitRepoAddress().startsWith("docker:"));
    ret.setDescriptorVersion(isDocker ? DescriptorVersion.V5 : DescriptorVersion.V1);

    //TODO bundlegroups contains bundle group version id! fix it!
    Set<BundleGroupVersionDto> bundleGroupVersions = this.bundleGroups.stream().map((bundleGroupVersionId) -> {
      BundleGroupVersionDto bundleGroupVersion = new BundleGroupVersionDto();
      bundleGroupVersion.setId(Long.valueOf(bundleGroupVersionId));
      return bundleGroupVersion;
    }).collect(Collectors.toSet());
    ret.setBundleGroupVersions(bundleGroupVersions);
    id.map(Long::valueOf).ifPresent(ret::setId);
    return ret;
  }
*/

}
