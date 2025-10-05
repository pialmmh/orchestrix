package com.telcobright.orchestrix.automation.infrastructure.consul.entity;

/**
 * Operating system type for Consul nodes
 */
public enum OsType {
    DEBIAN,   // Debian/Ubuntu - uses systemd, apt
    ALPINE,   // Alpine Linux - uses OpenRC, apk
    UNKNOWN
}
