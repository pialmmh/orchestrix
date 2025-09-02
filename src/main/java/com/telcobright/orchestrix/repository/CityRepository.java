package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Integer> {
    List<City> findByCountryId(Integer countryId);
    
    List<City> findByStateId(Integer stateId);
    
    @Query("SELECT c FROM City c WHERE c.country.code = :countryCode")
    List<City> findByCountryCode(@Param("countryCode") String countryCode);
    
    @Query("SELECT c FROM City c WHERE c.state.id = :stateId AND c.country.id = :countryId")
    List<City> findByCountryIdAndStateId(@Param("countryId") Integer countryId, @Param("stateId") Integer stateId);
    
    List<City> findByNameContainingIgnoreCase(String name);
    
    List<City> findByIsCapitalTrue();
}