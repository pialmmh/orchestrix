package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface StateRepository extends JpaRepository<State, Integer> {
    List<State> findByCountryId(Integer countryId);
    
    @Query("SELECT s FROM State s WHERE s.country.code = :countryCode")
    List<State> findByCountryCode(@Param("countryCode") String countryCode);
    
    @Query("SELECT s FROM State s WHERE s.country.id = :countryId AND s.code = :stateCode")
    Optional<State> findByCountryIdAndCode(@Param("countryId") Integer countryId, @Param("stateCode") String stateCode);
    
    List<State> findByNameContainingIgnoreCase(String name);
}