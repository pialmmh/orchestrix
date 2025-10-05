package com.telcobright.orchestrix.automation.infrastructure.consul.entity;

/**
 * Consul node role
 */
public enum ConsulNodeRole {
    /**
     * Consul server - participates in consensus
     */
    SERVER,

    /**
     * Consul client - forwards requests to servers
     */
    CLIENT
}
