package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.Country;
import com.telcobright.orchestrix.entity.State;
import com.telcobright.orchestrix.entity.City;
import com.telcobright.orchestrix.entity.Datacenter;
import com.telcobright.orchestrix.entity.Partner;
import com.telcobright.orchestrix.repository.CountryRepository;
import com.telcobright.orchestrix.repository.StateRepository;
import com.telcobright.orchestrix.repository.CityRepository;
import com.telcobright.orchestrix.repository.DatacenterRepository;
import com.telcobright.orchestrix.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/locations")
@CrossOrigin(origins = "*")
public class LocationController {
    
    @Autowired
    private CountryRepository countryRepository;
    
    @Autowired
    private StateRepository stateRepository;
    
    @Autowired
    private CityRepository cityRepository;
    
    @Autowired
    private DatacenterRepository datacenterRepository;
    
    @Autowired
    private PartnerRepository partnerRepository;
    
    // Country endpoints
    @GetMapping("/countries")
    public ResponseEntity<?> getAllCountries() {
        List<Country> countries = countryRepository.findAll();
        Map<String, Object> response = new HashMap<>();
        
        List<Map<String, Object>> countryList = countries.stream().map(country -> {
            Map<String, Object> countryMap = new HashMap<>();
            countryMap.put("id", country.getId());
            countryMap.put("code", country.getCode());
            countryMap.put("code3", country.getCode3());
            countryMap.put("name", country.getName());
            countryMap.put("region", country.getRegion());
            countryMap.put("hasStates", country.getHasStates());
            return countryMap;
        }).collect(Collectors.toList());
        
        response.put("countries", countryList);
        response.put("total", countryList.size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/countries/{countryCode}")
    public ResponseEntity<?> getCountryByCode(@PathVariable String countryCode) {
        return countryRepository.findByCode(countryCode)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }
    
    // State endpoints
    @GetMapping("/states")
    public ResponseEntity<?> getAllStates(@RequestParam(required = false) String countryCode) {
        List<State> states;
        if (countryCode != null) {
            states = stateRepository.findByCountryCode(countryCode);
        } else {
            states = stateRepository.findAll();
        }
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> stateList = states.stream().map(state -> {
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put("id", state.getId());
            stateMap.put("code", state.getCode());
            stateMap.put("name", state.getName());
            stateMap.put("countryId", state.getCountry().getId());
            stateMap.put("countryCode", state.getCountry().getCode());
            stateMap.put("countryName", state.getCountry().getName());
            return stateMap;
        }).collect(Collectors.toList());
        
        response.put("states", stateList);
        response.put("total", stateList.size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/countries/{countryCode}/states")
    public ResponseEntity<?> getStatesByCountry(@PathVariable String countryCode) {
        List<State> states = stateRepository.findByCountryCode(countryCode);
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> stateList = states.stream().map(state -> {
            Map<String, Object> stateMap = new HashMap<>();
            stateMap.put("id", state.getId());
            stateMap.put("code", state.getCode());
            stateMap.put("name", state.getName());
            return stateMap;
        }).collect(Collectors.toList());
        
        response.put("states", stateList);
        response.put("total", stateList.size());
        return ResponseEntity.ok(response);
    }
    
    // City endpoints
    @GetMapping("/cities")
    public ResponseEntity<?> getAllCities(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) Integer stateId) {
        
        List<City> cities;
        if (countryCode != null) {
            cities = cityRepository.findByCountryCode(countryCode);
        } else if (stateId != null) {
            cities = cityRepository.findByStateId(stateId);
        } else {
            cities = cityRepository.findAll();
        }
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> cityList = cities.stream().map(city -> {
            Map<String, Object> cityMap = new HashMap<>();
            cityMap.put("id", city.getId());
            cityMap.put("name", city.getName());
            cityMap.put("countryId", city.getCountry().getId());
            cityMap.put("countryCode", city.getCountry().getCode());
            cityMap.put("countryName", city.getCountry().getName());
            if (city.getState() != null) {
                cityMap.put("stateId", city.getState().getId());
                cityMap.put("stateCode", city.getState().getCode());
                cityMap.put("stateName", city.getState().getName());
            }
            cityMap.put("isCapital", city.getIsCapital());
            cityMap.put("latitude", city.getLatitude());
            cityMap.put("longitude", city.getLongitude());
            return cityMap;
        }).collect(Collectors.toList());
        
        response.put("cities", cityList);
        response.put("total", cityList.size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/countries/{countryCode}/cities")
    public ResponseEntity<?> getCitiesByCountry(@PathVariable String countryCode) {
        List<City> cities = cityRepository.findByCountryCode(countryCode);
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> cityList = cities.stream().map(city -> {
            Map<String, Object> cityMap = new HashMap<>();
            cityMap.put("id", city.getId());
            cityMap.put("name", city.getName());
            if (city.getState() != null) {
                cityMap.put("stateId", city.getState().getId());
                cityMap.put("stateName", city.getState().getName());
            }
            cityMap.put("isCapital", city.getIsCapital());
            return cityMap;
        }).collect(Collectors.toList());
        
        response.put("cities", cityList);
        response.put("total", cityList.size());
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/states/{stateId}/cities")
    public ResponseEntity<?> getCitiesByState(@PathVariable Integer stateId) {
        List<City> cities = cityRepository.findByStateId(stateId);
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> cityList = cities.stream().map(city -> {
            Map<String, Object> cityMap = new HashMap<>();
            cityMap.put("id", city.getId());
            cityMap.put("name", city.getName());
            cityMap.put("isCapital", city.getIsCapital());
            return cityMap;
        }).collect(Collectors.toList());
        
        response.put("cities", cityList);
        response.put("total", cityList.size());
        return ResponseEntity.ok(response);
    }
    
    // Datacenter endpoints
    @GetMapping("/datacenters")
    public ResponseEntity<?> getAllDatacenters() {
        List<Datacenter> datacenters = datacenterRepository.findAll();
        
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> datacenterList = datacenters.stream().map(dc -> {
            Map<String, Object> dcMap = new HashMap<>();
            dcMap.put("id", dc.getId());
            dcMap.put("name", dc.getName());
            
            if (dc.getCountry() != null) {
                dcMap.put("countryId", dc.getCountry().getId());
                dcMap.put("countryCode", dc.getCountry().getCode());
                dcMap.put("countryName", dc.getCountry().getName());
            }
            
            if (dc.getState() != null) {
                dcMap.put("stateId", dc.getState().getId());
                dcMap.put("stateCode", dc.getState().getCode());
                dcMap.put("stateName", dc.getState().getName());
            }
            
            if (dc.getCity() != null) {
                dcMap.put("cityId", dc.getCity().getId());
                dcMap.put("cityName", dc.getCity().getName());
            }
            
            dcMap.put("locationOther", dc.getLocationOther());
            dcMap.put("type", dc.getType());
            dcMap.put("status", dc.getStatus());
            dcMap.put("provider", dc.getProvider());
            
            if (dc.getPartner() != null) {
                dcMap.put("partnerId", dc.getPartner().getId());
                dcMap.put("partnerName", dc.getPartner().getName());
                dcMap.put("partnerDisplayName", dc.getPartner().getDisplayName());
            }
            
            dcMap.put("latitude", dc.getLatitude());
            dcMap.put("longitude", dc.getLongitude());
            dcMap.put("servers", dc.getServers());
            dcMap.put("storageTb", dc.getStorageTb());
            dcMap.put("utilization", dc.getUtilization());
            
            return dcMap;
        }).collect(Collectors.toList());
        
        response.put("datacenters", datacenterList);
        response.put("total", datacenterList.size());
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/datacenters")
    public ResponseEntity<?> createDatacenter(@RequestBody Map<String, Object> datacenterData) {
        Datacenter datacenter = new Datacenter();
        datacenter.setName((String) datacenterData.get("name"));
        
        // Set country if provided
        if (datacenterData.get("countryId") != null) {
            Integer countryId = Integer.valueOf(datacenterData.get("countryId").toString());
            countryRepository.findById(countryId).ifPresent(datacenter::setCountry);
        }
        
        // Set state if provided
        if (datacenterData.get("stateId") != null) {
            Integer stateId = Integer.valueOf(datacenterData.get("stateId").toString());
            stateRepository.findById(stateId).ifPresent(datacenter::setState);
        }
        
        // Set city if provided
        if (datacenterData.get("cityId") != null) {
            Integer cityId = Integer.valueOf(datacenterData.get("cityId").toString());
            cityRepository.findById(cityId).ifPresent(datacenter::setCity);
        }
        
        // Set other location if provided
        if (datacenterData.get("locationOther") != null) {
            datacenter.setLocationOther((String) datacenterData.get("locationOther"));
        }
        
        // Set partner if provided
        if (datacenterData.get("partnerId") != null) {
            Integer partnerId = Integer.valueOf(datacenterData.get("partnerId").toString());
            partnerRepository.findById(partnerId).ifPresent(datacenter::setPartner);
        }
        
        datacenter.setType((String) datacenterData.getOrDefault("type", "Primary"));
        datacenter.setStatus((String) datacenterData.getOrDefault("status", "ACTIVE"));
        datacenter.setProvider((String) datacenterData.getOrDefault("provider", "AWS"));
        
        if (datacenterData.get("servers") != null) {
            datacenter.setServers(Integer.valueOf(datacenterData.get("servers").toString()));
        }
        
        if (datacenterData.get("storageTb") != null) {
            datacenter.setStorageTb(Integer.valueOf(datacenterData.get("storageTb").toString()));
        }
        
        if (datacenterData.get("utilization") != null) {
            datacenter.setUtilization(Integer.valueOf(datacenterData.get("utilization").toString()));
        }
        
        Datacenter saved = datacenterRepository.save(datacenter);
        return ResponseEntity.ok(saved);
    }
    
    @PutMapping("/datacenters/{id}")
    public ResponseEntity<?> updateDatacenter(@PathVariable Integer id, @RequestBody Map<String, Object> datacenterData) {
        return datacenterRepository.findById(id)
            .map(datacenter -> {
                if (datacenterData.get("name") != null) {
                    datacenter.setName((String) datacenterData.get("name"));
                }
                
                // Update country if provided
                if (datacenterData.containsKey("countryId")) {
                    if (datacenterData.get("countryId") != null) {
                        Integer countryId = Integer.valueOf(datacenterData.get("countryId").toString());
                        countryRepository.findById(countryId).ifPresent(datacenter::setCountry);
                    } else {
                        datacenter.setCountry(null);
                    }
                }
                
                // Update state if provided
                if (datacenterData.containsKey("stateId")) {
                    if (datacenterData.get("stateId") != null) {
                        Integer stateId = Integer.valueOf(datacenterData.get("stateId").toString());
                        stateRepository.findById(stateId).ifPresent(datacenter::setState);
                    } else {
                        datacenter.setState(null);
                    }
                }
                
                // Update city if provided
                if (datacenterData.containsKey("cityId")) {
                    if (datacenterData.get("cityId") != null) {
                        Integer cityId = Integer.valueOf(datacenterData.get("cityId").toString());
                        cityRepository.findById(cityId).ifPresent(datacenter::setCity);
                    } else {
                        datacenter.setCity(null);
                    }
                }
                
                // Update other location if provided
                if (datacenterData.containsKey("locationOther")) {
                    datacenter.setLocationOther((String) datacenterData.get("locationOther"));
                }
                
                // Update partner if provided
                if (datacenterData.containsKey("partnerId")) {
                    if (datacenterData.get("partnerId") != null) {
                        Integer partnerId = Integer.valueOf(datacenterData.get("partnerId").toString());
                        partnerRepository.findById(partnerId).ifPresent(datacenter::setPartner);
                    } else {
                        datacenter.setPartner(null);
                    }
                }
                
                if (datacenterData.get("type") != null) {
                    datacenter.setType((String) datacenterData.get("type"));
                }
                
                if (datacenterData.get("status") != null) {
                    datacenter.setStatus((String) datacenterData.get("status"));
                }
                
                if (datacenterData.get("provider") != null) {
                    datacenter.setProvider((String) datacenterData.get("provider"));
                }
                
                if (datacenterData.get("servers") != null) {
                    datacenter.setServers(Integer.valueOf(datacenterData.get("servers").toString()));
                }
                
                if (datacenterData.get("storageTb") != null) {
                    datacenter.setStorageTb(Integer.valueOf(datacenterData.get("storageTb").toString()));
                }
                
                if (datacenterData.get("utilization") != null) {
                    datacenter.setUtilization(Integer.valueOf(datacenterData.get("utilization").toString()));
                }
                
                Datacenter updated = datacenterRepository.save(datacenter);
                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }
    
    @DeleteMapping("/datacenters/{id}")
    public ResponseEntity<?> deleteDatacenter(@PathVariable Integer id) {
        return datacenterRepository.findById(id)
            .map(datacenter -> {
                datacenterRepository.delete(datacenter);
                Map<String, String> response = new HashMap<>();
                response.put("message", "Datacenter deleted successfully");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }
}