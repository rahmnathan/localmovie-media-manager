package com.github.rahmnathan.localmovie.media.omdb;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.persistence.entity.Media;
import com.github.rahmnathan.localmovie.persistence.entity.MediaImage;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.Closeable;

@Component
public class MediaProvider implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(MediaProvider.class);
    private static final String OMDB_URL = "https://www.omdbapi.com";

    private final Client client;
    private final OmdbApi omdbApi;
    private final ServiceConfig serviceConfig;

    public MediaProvider(ServiceConfig serviceConfig) {
        this.serviceConfig = serviceConfig;
        this.client = ClientBuilder.newBuilder().build();

        ResteasyWebTarget target = (ResteasyWebTarget) client.target(OMDB_URL);
        this.omdbApi = target.proxy(OmdbApi.class);
    }

    public Media getMovie(String title, String year) throws MediaProviderException {
        logger.debug("Request for movie: {}", title);

        OmdbResponse response = omdbApi.getMedia(title, year, "movie", serviceConfig.getOmdb().getApiKey());

        Media media = buildMedia(response, title, null, MediaType.MOVIE);
        logger.debug("Movie response: {}", media);
        return media;
    }

    public Media getSeries(String title, String year) throws MediaProviderException {
        logger.debug("Received request for series: {}", title);

        OmdbResponse response = omdbApi.getMedia(title, year, "series", serviceConfig.getOmdb().getApiKey());

        Media media = buildMedia(response, title, null, MediaType.SERIES);
        logger.debug("Series response: {}", media);
        return media;
    }

    public Media getEpisode(String seriesTitle, Integer seasonNumber, Integer episodeNumber) throws MediaProviderException {
        logger.debug("Received request for episode. Series: {} season number: {} episode number: {}", seriesTitle, seasonNumber, episodeNumber);

        OmdbResponse response = omdbApi.getEpisode(seriesTitle, seasonNumber, episodeNumber, "episode", serviceConfig.getOmdb().getApiKey());

        Media media = buildMedia(response, seriesTitle, episodeNumber, MediaType.EPISODE);
        logger.debug("Episode response: {}", media);
        return media;
    }

    private Media buildMedia(OmdbResponse response, String requestedTitle, Integer number, MediaType mediaType) throws MediaProviderException {
        if (response == null || !response.isSuccess()) {
            throw new MediaNotFoundException("Media not found.");
        }

        Media.MediaBuilder mediaBuilder = Media.builder()
                .mediaType(mediaType)
                .imdbRating(response.getImdbRating())
                .metaRating(response.getMetascore())
                .releaseYear(response.getYear())
                .genre(response.getGenre())
                .plot(response.getPlot())
                .actors(response.getActors())
                .imdbId(response.getImdbId());

        if (mediaType == MediaType.EPISODE || mediaType == MediaType.SERIES) {
            mediaBuilder.number(number);
            if (response.getTitle() != null && !response.getTitle().isBlank()) {
                mediaBuilder.title(response.getTitle());
            } else if (MediaType.EPISODE == mediaType) {
                mediaBuilder.title("Episode " + number);
            } else {
                mediaBuilder.title("Season " + number);
            }
        } else {
            mediaBuilder.title(requestedTitle);
        }

        Media result = mediaBuilder.build();

        if (response.hasPoster()) {
            byte[] posterData = fetchPoster(response.getPoster());
            if (posterData != null) {
                MediaImage mediaImage = new MediaImage(posterData, result);
                result.setImage(mediaImage);
            }
        }

        return result;
    }

    private byte[] fetchPoster(String posterUrl) {
        try {
            Response response = client.target(posterUrl)
                    .request()
                    .get();

            if (response.getStatus() == 200) {
                return response.readEntity(byte[].class);
            } else {
                logger.warn("Failed to fetch poster from {}. Status: {}", posterUrl, response.getStatus());
                return null;
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch poster from {}: {}", posterUrl, e.getMessage());
            return null;
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
        }
    }
}
