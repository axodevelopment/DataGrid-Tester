package org.acme.infinispan.client;

import io.quarkus.infinispan.client.Remote;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.infinispan.client.hotrod.RemoteCache;

import java.util.concurrent.CompletionStage;

@Path("/greeting")
public class InfinispanGreetingResource {

    @Inject
    @Remote("respCache") 
    RemoteCache<String, Greeting> cache; 

    @POST
    @Path("/{id}")
    public CompletionStage<String> postGreeting(String id, Greeting greeting) {
        return cache.putAsync(id, greeting) 
              .thenApply(g -> "Greeting done!")
              .exceptionally(ex -> ex.getMessage());
    }

    @GET
    @Path("/{id}")
    public CompletionStage<Greeting> getGreeting(String id) {
        return cache.getAsync(id); 
    }
}
/*
package org.acme.infinispan.client;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import java.util.concurrent.CompletionStage;

@Path("/greeting")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InfinispanGreetingResource {

    @Inject
    RemoteCacheManager cacheManager;

    private RemoteCache<String, Greeting> getFlapCache() {
        RemoteCache<String, Greeting> cache = cacheManager.getCache("testCache");
        if (cache == null) {
            throw new IllegalStateException("Cache 'testCache' not found on server!");
        }
        return cache;
    }

    @POST
    @Path("/{id}")
    public CompletionStage<String> postGreeting(@PathParam("id") String id, Greeting greeting) {
        return getFlapCache()
                .putAsync(id, greeting)
                .thenApply(g -> "Greeting done!")
                .exceptionally(ex -> "Error: " + ex.getMessage());
    }

    @GET
    @Path("/{id}")
    public CompletionStage<Greeting> getGreeting(@PathParam("id") String id) {
        return getFlapCache()
                .getAsync(id)
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null; // or maybe return a default Greeting if you want
                });
    }
}
    */
