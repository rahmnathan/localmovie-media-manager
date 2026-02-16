package com.github.rahmnathan.localmovie.media.recommendation.ollama;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api")
public interface OllamaApi {

    @POST
    @Path("/generate")
    @Retry(name = "ollama")
    @CircuitBreaker(name = "ollama-client")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    OllamaResponse generate(OllamaRequest request);

    @GET
    @Path("/tags")
    @Produces(MediaType.APPLICATION_JSON)
    OllamaTagsResponse getTags();
}
