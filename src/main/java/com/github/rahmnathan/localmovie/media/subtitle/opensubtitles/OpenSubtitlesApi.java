package com.github.rahmnathan.localmovie.media.subtitle.opensubtitles;

import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model.*;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1")
public interface OpenSubtitlesApi {

    @POST
    @Path("/login")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    OpenSubtitlesLoginResponse login(
            @HeaderParam("Api-Key") String apiKey,
            OpenSubtitlesLoginRequest request);

    @GET
    @Path("/subtitles")
    @Produces(MediaType.APPLICATION_JSON)
    OpenSubtitlesSearchResponse search(
            @HeaderParam("Api-Key") String apiKey,
            @HeaderParam("Authorization") String bearerToken,
            @QueryParam("imdb_id") String imdbId,
            @QueryParam("languages") String languages);

    @POST
    @Path("/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.WILDCARD)  // OpenSubtitles requires Accept: */* for download endpoint
    OpenSubtitlesDownloadResponse download(
            @HeaderParam("Api-Key") String apiKey,
            @HeaderParam("Authorization") String bearerToken,
            OpenSubtitlesDownloadRequest request);
}
