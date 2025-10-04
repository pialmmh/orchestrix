package com.telcobright.orchestrix.automation.api.device;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public interface SshCapable {
    
    CompletableFuture<Boolean> connectSsh(String hostname, int port, String username, String password) throws IOException;
    
    CompletableFuture<String> sendSshCommand(String command) throws IOException;
    
    CompletableFuture<String> receiveSshResponse() throws IOException;
    
    boolean isSshConnected();
    
    void disconnectSsh() throws IOException;
    
    default CompletableFuture<String> executeSshCommand(String command) {
        try {
            return sendSshCommand(command)
                .thenCompose(sent -> {
                    try {
                        return receiveSshResponse();
                    } catch (IOException e) {
                        return CompletableFuture.failedFuture(e);
                    }
                });
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}