package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.dto.CloudDTO;
import com.telcobright.orchestrix.entity.Cloud;
import com.telcobright.orchestrix.entity.Partner;
import com.telcobright.orchestrix.repository.CloudRepository;
import com.telcobright.orchestrix.repository.PartnerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/clouds")
@CrossOrigin(origins = "*")
public class CloudController {

    @Autowired
    private CloudRepository cloudRepository;
    
    @Autowired
    private PartnerRepository partnerRepository;

    @GetMapping
    public List<Cloud> getAllClouds() {
        return cloudRepository.findAllWithDatacenters();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cloud> getCloudById(@PathVariable Long id) {
        Cloud cloud = cloudRepository.findByIdWithDatacenters(id);
        if (cloud != null) {
            return ResponseEntity.ok(cloud);
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping
    public ResponseEntity<?> createCloud(@RequestBody CloudDTO cloudDTO) {
        // Partner is mandatory
        if (cloudDTO.getPartnerId() == null || cloudDTO.getPartnerId() <= 0) {
            return ResponseEntity.badRequest().body("Partner selection is mandatory for creating a cloud");
        }
        
        Partner partner = partnerRepository.findById(cloudDTO.getPartnerId()).orElse(null);
        if (partner == null) {
            return ResponseEntity.badRequest().body("Invalid partner ID");
        }
        
        Cloud cloud = new Cloud();
        cloud.setName(cloudDTO.getName());
        cloud.setDescription(cloudDTO.getDescription());
        cloud.setDeploymentRegion(cloudDTO.getDeploymentRegion());
        cloud.setStatus(cloudDTO.getStatus());
        cloud.setPartner(partner);
        
        return ResponseEntity.ok(cloudRepository.save(cloud));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCloud(@PathVariable Long id, @RequestBody CloudDTO cloudDTO) {
        Optional<Cloud> optionalCloud = cloudRepository.findById(id);
        if (optionalCloud.isPresent()) {
            Cloud cloud = optionalCloud.get();
            cloud.setName(cloudDTO.getName());
            cloud.setDescription(cloudDTO.getDescription());
            cloud.setDeploymentRegion(cloudDTO.getDeploymentRegion());
            cloud.setStatus(cloudDTO.getStatus());
            
            // Partner is mandatory - cannot be null or removed
            if (cloudDTO.getPartnerId() == null || cloudDTO.getPartnerId() <= 0) {
                return ResponseEntity.badRequest().body("Partner selection is mandatory for a cloud");
            }
            
            Partner partner = partnerRepository.findById(cloudDTO.getPartnerId()).orElse(null);
            if (partner == null) {
                return ResponseEntity.badRequest().body("Invalid partner ID");
            }
            cloud.setPartner(partner);
            
            return ResponseEntity.ok(cloudRepository.save(cloud));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCloud(@PathVariable Long id) {
        if (cloudRepository.existsById(id)) {
            cloudRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/client/{clientName}")
    public List<Cloud> getCloudsByClient(@PathVariable String clientName) {
        return cloudRepository.findByClientName(clientName);
    }
}