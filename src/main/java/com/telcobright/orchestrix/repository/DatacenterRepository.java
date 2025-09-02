package com.telcobright.orchestrix.repository;

import com.telcobright.orchestrix.entity.Datacenter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DatacenterRepository extends JpaRepository<Datacenter, Integer> {
    List<Datacenter> findByStatus(String status);
    
    List<Datacenter> findByType(String type);
    
    List<Datacenter> findByProvider(String provider);
    
    @Query("SELECT d FROM Datacenter d WHERE d.country.code = :countryCode")
    List<Datacenter> findByCountryCode(@Param("countryCode") String countryCode);
    
    @Query("SELECT d FROM Datacenter d WHERE d.city.id = :cityId")
    List<Datacenter> findByCityId(@Param("cityId") Integer cityId);
    
    List<Datacenter> findByNameContainingIgnoreCase(String name);
}