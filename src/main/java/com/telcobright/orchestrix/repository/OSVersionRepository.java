package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.OSVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OSVersionRepository extends JpaRepository<OSVersion, Long> {
    List<OSVersion> findByOsTypeOrderByDistributionAscVersionAsc(String osType);
    List<OSVersion> findAllByOrderByOsTypeAscDistributionAscVersionAsc();
}