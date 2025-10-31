package com.telcobright.orchestrix.automation.cluster.kafkazookeeper;

/**
 * Configuration for a remote server
 */
public class ServerConfig {
    private final String host;
    private final int sshPort;
    private final String sshUser;
    private final String sshPassword;

    public ServerConfig(String host, int sshPort, String sshUser, String sshPassword) {
        this.host = host;
        this.sshPort = sshPort;
        this.sshUser = sshUser;
        this.sshPassword = sshPassword;
    }

    public String getHost() {
        return host;
    }

    public int getSshPort() {
        return sshPort;
    }

    public String getSshUser() {
        return sshUser;
    }

    public String getSshPassword() {
        return sshPassword;
    }

    @Override
    public String toString() {
        return String.format("ServerConfig{host='%s', port=%d, user='%s'}", host, sshPort, sshUser);
    }
}
