package com.github.rahmnathan.localmovie.media.omdb;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@Path("/")
public interface OmdbApi {

    @GET
    @Retry(name = "omdb")
    @CircuitBreaker(name = "omdb-client")
    @Produces(MediaType.APPLICATION_JSON)
    OmdbResponse getMedia(@QueryParam("t") String title,
                          @QueryParam("y") String year,
                          @QueryParam("type") String type,
                          @QueryParam("apikey") String apiKey);

    @GET
    @Retry(name = "omdb")
    @CircuitBreaker(name = "omdb-client")
    @Produces(MediaType.APPLICATION_JSON)
    OmdbResponse getEpisode(@QueryParam("t") String seriesTitle,
                            @QueryParam("Season") Integer seasonNumber,
                            @QueryParam("Episode") Integer episodeNumber,
                            @QueryParam("type") String type,
                            @QueryParam("apikey") String apiKey);
}
