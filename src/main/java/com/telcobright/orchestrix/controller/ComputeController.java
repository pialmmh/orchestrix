package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.Compute;
import com.telcobright.orchestrix.repository.ComputeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/computes")
@CrossOrigin(origins = "*")
public class ComputeController {

    @Autowired
    private ComputeRepository computeRepository;

    @GetMapping
    public List<Compute> getAllComputes() {
        return computeRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Compute> getComputeById(@PathVariable Long id) {
        Compute compute = computeRepository.findByIdWithContainers(id);
        if (compute != null) {
            return ResponseEntity.ok(compute);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/cloud/{cloudId}")
    public List<Compute> getComputesByCloud(@PathVariable Long cloudId) {
        return computeRepository.findByCloudId(cloudId);
    }

    @GetMapping("/datacenter/{datacenterId}")
    public List<Compute> getComputesByDatacenter(@PathVariable Long datacenterId) {
        return computeRepository.findByDatacenterId(datacenterId);
    }

    @PostMapping
    public Compute createCompute(@RequestBody Compute compute) {
        return computeRepository.save(compute);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Compute> updateCompute(@PathVariable Long id, @RequestBody Compute computeDetails) {
        Optional<Compute> optionalCompute = computeRepository.findById(id);
        if (optionalCompute.isPresent()) {
            Compute compute = optionalCompute.get();
            compute.setName(computeDetails.getName());
            compute.setDescription(computeDetails.getDescription());
            compute.setNodeType(computeDetails.getNodeType());
            compute.setHostname(computeDetails.getHostname());
            compute.setIpAddress(computeDetails.getIpAddress());
            compute.setOsVersion(computeDetails.getOsVersion());
            compute.setCpuCores(computeDetails.getCpuCores());
            compute.setMemoryGb(computeDetails.getMemoryGb());
            compute.setDiskGb(computeDetails.getDiskGb());
            compute.setStatus(computeDetails.getStatus());
            return ResponseEntity.ok(computeRepository.save(compute));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompute(@PathVariable Long id) {
        if (computeRepository.existsById(id)) {
            computeRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}