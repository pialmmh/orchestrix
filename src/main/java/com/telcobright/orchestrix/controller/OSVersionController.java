package com.telcobright.orchestrix.controller;

import com.telcobright.orchestrix.entity.OSVersion;
import com.telcobright.orchestrix.repository.OSVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/os-versions")
@CrossOrigin(origins = "*")
public class OSVersionController {

    @Autowired
    private OSVersionRepository osVersionRepository;

    @GetMapping
    public ResponseEntity<List<OSVersion>> getAllOSVersions() {
        List<OSVersion> osVersions = osVersionRepository.findAllByOrderByOsTypeAscDistributionAscVersionAsc();
        return ResponseEntity.ok(osVersions);
    }

    @GetMapping("/by-type/{osType}")
    public ResponseEntity<List<OSVersion>> getOSVersionsByType(@PathVariable String osType) {
        List<OSVersion> osVersions = osVersionRepository.findByOsTypeOrderByDistributionAscVersionAsc(osType);
        return ResponseEntity.ok(osVersions);
    }
}