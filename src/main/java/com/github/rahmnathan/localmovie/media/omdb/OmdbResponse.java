package com.github.rahmnathan.localmovie.media.omdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OmdbResponse {

    @JsonProperty("Title")
    private String title;

    @JsonProperty("Year")
    private String year;

    @JsonProperty("imdbRating")
    private String imdbRating;

    @JsonProperty("Metascore")
    private String metascore;

    @JsonProperty("Genre")
    private String genre;

    @JsonProperty("Plot")
    private String plot;

    @JsonProperty("Actors")
    private String actors;

    @JsonProperty("Poster")
    private String poster;

    @JsonProperty("imdbID")
    private String imdbId;

    @JsonProperty("Response")
    private String response;

    public boolean isSuccess() {
        return "True".equalsIgnoreCase(response);
    }

    public boolean hasPoster() {
        return poster != null && !poster.equalsIgnoreCase("N/A") && !poster.isBlank();
    }
}
