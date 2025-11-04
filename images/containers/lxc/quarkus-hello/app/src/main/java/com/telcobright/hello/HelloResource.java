package com.telcobright.hello;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.jboss.logging.Logger;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Path("/hello")
public class HelloResource {

    private static final Logger LOG = Logger.getLogger(HelloResource.class);
    private static int requestCounter = 0;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> hello(@QueryParam("name") String name) {
        requestCounter++;

        String recipient = (name != null && !name.isEmpty()) ? name : "World";
        String message = "Hello, " + recipient + "!";

        // Log at different levels for testing
        LOG.info("Request #" + requestCounter + " - Saying hello to: " + recipient);
        LOG.debug("Debug: Processing hello request with name parameter: " + name);

        if (requestCounter % 10 == 0) {
            LOG.warn("Warning: Request counter reached: " + requestCounter);
        }

        if (requestCounter % 50 == 0) {
            LOG.error("Error simulation: High request count milestone: " + requestCounter);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", message);
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("requestNumber", requestCounter);
        response.put("service", "quarkus-hello");

        LOG.info("Response sent successfully for request #" + requestCounter);

        return response;
    }

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> status() {
        LOG.info("Status endpoint called");

        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "quarkus-hello");
        status.put("totalRequests", requestCounter);
        status.put("timestamp", LocalDateTime.now().toString());

        return status;
    }
}
