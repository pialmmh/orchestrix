package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.Country;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface CountryRepository extends JpaRepository<Country, Integer> {
    Optional<Country> findByCode(String code);
    Optional<Country> findByCode3(String code3);
    Optional<Country> findByName(String name);
    List<Country> findByRegion(String region);
    List<Country> findByHasStatesTrue();
}