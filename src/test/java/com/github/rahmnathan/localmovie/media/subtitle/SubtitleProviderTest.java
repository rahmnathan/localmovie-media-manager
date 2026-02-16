package com.github.rahmnathan.localmovie.media.subtitle;

import com.github.rahmnathan.localmovie.config.ServiceConfig;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.OpenSubtitlesApi;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.OpenSubtitlesAuthManager;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model.OpenSubtitlesDownloadResponse;
import com.github.rahmnathan.localmovie.media.subtitle.opensubtitles.model.OpenSubtitlesSearchResponse;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SubtitleProviderTest {

    @Mock
    private ServiceConfig serviceConfig;
    @Mock
    private ServiceConfig.OpenSubtitlesConfig openSubtitlesConfig;
    @Mock
    private Client client;
    @Mock
    private OpenSubtitlesApi openSubtitlesApi;
    @Mock
    private OpenSubtitlesAuthManager authManager;
    @Mock
    private WebTarget webTarget;
    @Mock
    private Invocation.Builder invocationBuilder;
    @Mock
    private Response response;

    private SubtitleProvider subtitleProvider;

    @BeforeEach
    void setUp() {
        when(serviceConfig.getOpensubtitles()).thenReturn(openSubtitlesConfig);
        subtitleProvider = new SubtitleProvider(serviceConfig, client, openSubtitlesApi, authManager);
    }

    @Test
    void fetchSubtitle_whenDisabled_returnsEmpty() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(false);

        Optional<SubtitleResult> result = subtitleProvider.fetchSubtitle("tt1234567");

        assertTrue(result.isEmpty());
        verifyNoInteractions(openSubtitlesApi);
    }

    @Test
    void fetchSubtitle_whenQuotaExhausted_throwsException() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(authManager.hasDownloadsRemaining()).thenReturn(false);

        assertThrows(DownloadQuotaExceededException.class,
                () -> subtitleProvider.fetchSubtitle("tt1234567"));
    }

    @Test
    void fetchSubtitle_whenAuthFails_returnsEmpty() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(authManager.hasDownloadsRemaining()).thenReturn(true);
        when(authManager.getToken()).thenReturn(null);

        Optional<SubtitleResult> result = subtitleProvider.fetchSubtitle("tt1234567");

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchSubtitle_whenNoSearchResults_returnsEmpty() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(openSubtitlesConfig.getApiKey()).thenReturn("test-api-key");
        when(authManager.hasDownloadsRemaining()).thenReturn(true);
        when(authManager.getToken()).thenReturn("test-token");

        OpenSubtitlesSearchResponse emptyResponse = new OpenSubtitlesSearchResponse();
        emptyResponse.setData(List.of());
        when(openSubtitlesApi.search(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(emptyResponse);

        Optional<SubtitleResult> result = subtitleProvider.fetchSubtitle("tt1234567");

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchSubtitle_success() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(openSubtitlesConfig.getApiKey()).thenReturn("test-api-key");
        when(authManager.hasDownloadsRemaining()).thenReturn(true);
        when(authManager.getToken()).thenReturn("test-token");

        // Set up search response
        OpenSubtitlesSearchResponse searchResponse = createSearchResponse();
        when(openSubtitlesApi.search(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(searchResponse);

        // Set up download response
        OpenSubtitlesDownloadResponse downloadResponse = new OpenSubtitlesDownloadResponse();
        downloadResponse.setLink("https://example.com/subtitle.vtt");
        downloadResponse.setRemaining(99);
        when(openSubtitlesApi.download(anyString(), anyString(), any()))
                .thenReturn(downloadResponse);

        // Set up VTT content fetch
        when(client.target("https://example.com/subtitle.vtt")).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(invocationBuilder);
        when(invocationBuilder.get()).thenReturn(response);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(String.class)).thenReturn("WEBVTT\n\n00:00:01.000 --> 00:00:05.000\nHello");

        Optional<SubtitleResult> result = subtitleProvider.fetchSubtitle("tt1234567");

        assertTrue(result.isPresent());
        assertEquals("vtt", result.get().getFormat());
        assertEquals("en", result.get().getLanguageCode());
        assertEquals("sub123", result.get().getOpensubtitlesId());
        assertTrue(result.get().getContent().contains("WEBVTT"));

        verify(authManager).updateRemainingDownloads(99);
    }

    @Test
    void fetchSubtitle_normalizesImdbIdWithPrefix() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(openSubtitlesConfig.getApiKey()).thenReturn("test-api-key");
        when(authManager.hasDownloadsRemaining()).thenReturn(true);
        when(authManager.getToken()).thenReturn("test-token");

        OpenSubtitlesSearchResponse emptyResponse = new OpenSubtitlesSearchResponse();
        when(openSubtitlesApi.search(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(emptyResponse);

        subtitleProvider.fetchSubtitle("tt1234567");

        // Should strip "tt" prefix
        verify(openSubtitlesApi).search(anyString(), anyString(), eq("1234567"), anyString());
    }

    @Test
    void fetchSubtitle_selectsHighestDownloadCount() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(openSubtitlesConfig.getApiKey()).thenReturn("test-api-key");
        when(authManager.hasDownloadsRemaining()).thenReturn(true);
        when(authManager.getToken()).thenReturn("test-token");

        // Create search response with multiple results
        OpenSubtitlesSearchResponse searchResponse = createSearchResponseWithMultiple();
        when(openSubtitlesApi.search(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(searchResponse);

        // Set up download response
        OpenSubtitlesDownloadResponse downloadResponse = new OpenSubtitlesDownloadResponse();
        downloadResponse.setLink("https://example.com/subtitle.vtt");
        downloadResponse.setRemaining(99);
        when(openSubtitlesApi.download(anyString(), anyString(), any()))
                .thenReturn(downloadResponse);

        // Set up VTT content fetch
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(invocationBuilder);
        when(invocationBuilder.get()).thenReturn(response);
        when(response.getStatus()).thenReturn(200);
        when(response.readEntity(String.class)).thenReturn("WEBVTT");

        subtitleProvider.fetchSubtitle("tt1234567");

        // Should select the subtitle with highest download count (999 in this case)
        verify(openSubtitlesApi).download(anyString(), anyString(), argThat(req ->
                req.getFileId() == 999));
    }

    @Test
    void fetchSubtitle_whenDownloadFails_returnsEmpty() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(openSubtitlesConfig.getApiKey()).thenReturn("test-api-key");
        when(authManager.hasDownloadsRemaining()).thenReturn(true);
        when(authManager.getToken()).thenReturn("test-token");

        OpenSubtitlesSearchResponse searchResponse = createSearchResponse();
        when(openSubtitlesApi.search(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(searchResponse);

        // Download response with no link
        OpenSubtitlesDownloadResponse downloadResponse = new OpenSubtitlesDownloadResponse();
        downloadResponse.setLink(null);
        when(openSubtitlesApi.download(anyString(), anyString(), any()))
                .thenReturn(downloadResponse);

        Optional<SubtitleResult> result = subtitleProvider.fetchSubtitle("tt1234567");

        assertTrue(result.isEmpty());
    }

    @Test
    void fetchSubtitle_whenContentFetchFails_returnsEmpty() throws DownloadQuotaExceededException {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(openSubtitlesConfig.getApiKey()).thenReturn("test-api-key");
        when(authManager.hasDownloadsRemaining()).thenReturn(true);
        when(authManager.getToken()).thenReturn("test-token");

        OpenSubtitlesSearchResponse searchResponse = createSearchResponse();
        when(openSubtitlesApi.search(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(searchResponse);

        OpenSubtitlesDownloadResponse downloadResponse = new OpenSubtitlesDownloadResponse();
        downloadResponse.setLink("https://example.com/subtitle.vtt");
        downloadResponse.setRemaining(99);
        when(openSubtitlesApi.download(anyString(), anyString(), any()))
                .thenReturn(downloadResponse);

        // Content fetch returns 404
        when(client.target(anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(invocationBuilder);
        when(invocationBuilder.get()).thenReturn(response);
        when(response.getStatus()).thenReturn(404);

        Optional<SubtitleResult> result = subtitleProvider.fetchSubtitle("tt1234567");

        assertTrue(result.isEmpty());
    }

    @Test
    void hasDownloadsRemaining_delegatesToAuthManager() {
        when(authManager.hasDownloadsRemaining()).thenReturn(true);
        assertTrue(subtitleProvider.hasDownloadsRemaining());

        when(authManager.hasDownloadsRemaining()).thenReturn(false);
        assertFalse(subtitleProvider.hasDownloadsRemaining());
    }

    @Test
    void getRemainingDownloads_delegatesToAuthManager() {
        when(authManager.getRemainingDownloads()).thenReturn(42);
        assertEquals(42, subtitleProvider.getRemainingDownloads());
    }

    @Test
    void refreshQuota_delegatesToAuthManager() {
        subtitleProvider.refreshQuota();
        verify(authManager).refreshQuota();
    }

    @Test
    void fetchSubtitle_when406Error_setsRemainingToZeroAndThrowsQuotaExceeded() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(openSubtitlesConfig.getApiKey()).thenReturn("test-api-key");
        when(authManager.hasDownloadsRemaining()).thenReturn(true);
        when(authManager.getToken()).thenReturn("test-token");

        OpenSubtitlesSearchResponse searchResponse = createSearchResponse();
        when(openSubtitlesApi.search(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(searchResponse);

        // Simulate 406 Not Acceptable response
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(406);
        Response.StatusType statusType = mock(Response.StatusType.class);
        when(statusType.getStatusCode()).thenReturn(406);
        when(statusType.getReasonPhrase()).thenReturn("Not Acceptable");
        when(mockResponse.getStatusInfo()).thenReturn(statusType);
        WebApplicationException webException = new WebApplicationException(mockResponse);
        when(openSubtitlesApi.download(anyString(), anyString(), any()))
                .thenThrow(webException);

        assertThrows(DownloadQuotaExceededException.class,
                () -> subtitleProvider.fetchSubtitle("tt1234567"));

        // Verify that remaining downloads was set to 0
        verify(authManager).updateRemainingDownloads(0);
    }

    @Test
    void fetchSubtitle_whenNon406Error_propagatesException() {
        when(openSubtitlesConfig.isEnabled()).thenReturn(true);
        when(openSubtitlesConfig.getApiKey()).thenReturn("test-api-key");
        when(authManager.hasDownloadsRemaining()).thenReturn(true);
        when(authManager.getToken()).thenReturn("test-token");

        OpenSubtitlesSearchResponse searchResponse = createSearchResponse();
        when(openSubtitlesApi.search(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(searchResponse);

        // Simulate 500 Internal Server Error
        Response mockResponse = mock(Response.class);
        when(mockResponse.getStatus()).thenReturn(500);
        Response.StatusType statusType = mock(Response.StatusType.class);
        when(statusType.getStatusCode()).thenReturn(500);
        when(statusType.getReasonPhrase()).thenReturn("Internal Server Error");
        when(mockResponse.getStatusInfo()).thenReturn(statusType);
        WebApplicationException webException = new WebApplicationException(mockResponse);
        when(openSubtitlesApi.download(anyString(), anyString(), any()))
                .thenThrow(webException);

        assertThrows(WebApplicationException.class,
                () -> subtitleProvider.fetchSubtitle("tt1234567"));

        // Verify that remaining downloads was NOT set to 0 for non-406 errors
        verify(authManager, never()).updateRemainingDownloads(anyInt());
    }

    private OpenSubtitlesSearchResponse createSearchResponse() {
        OpenSubtitlesSearchResponse.SubtitleFile file = new OpenSubtitlesSearchResponse.SubtitleFile();
        file.setFileId(123);
        file.setFileName("subtitle.srt");

        OpenSubtitlesSearchResponse.SubtitleAttributes attrs = new OpenSubtitlesSearchResponse.SubtitleAttributes();
        attrs.setSubtitleId("sub123");
        attrs.setDownloadCount(100);
        attrs.setFiles(List.of(file));

        OpenSubtitlesSearchResponse.SubtitleData data = new OpenSubtitlesSearchResponse.SubtitleData();
        data.setAttributes(attrs);

        OpenSubtitlesSearchResponse response = new OpenSubtitlesSearchResponse();
        response.setData(List.of(data));
        return response;
    }

    private OpenSubtitlesSearchResponse createSearchResponseWithMultiple() {
        // Low download count
        OpenSubtitlesSearchResponse.SubtitleFile file1 = new OpenSubtitlesSearchResponse.SubtitleFile();
        file1.setFileId(111);
        OpenSubtitlesSearchResponse.SubtitleAttributes attrs1 = new OpenSubtitlesSearchResponse.SubtitleAttributes();
        attrs1.setSubtitleId("sub1");
        attrs1.setDownloadCount(10);
        attrs1.setFiles(List.of(file1));
        OpenSubtitlesSearchResponse.SubtitleData data1 = new OpenSubtitlesSearchResponse.SubtitleData();
        data1.setAttributes(attrs1);

        // High download count - should be selected
        OpenSubtitlesSearchResponse.SubtitleFile file2 = new OpenSubtitlesSearchResponse.SubtitleFile();
        file2.setFileId(999);
        OpenSubtitlesSearchResponse.SubtitleAttributes attrs2 = new OpenSubtitlesSearchResponse.SubtitleAttributes();
        attrs2.setSubtitleId("sub2");
        attrs2.setDownloadCount(1000);
        attrs2.setFiles(List.of(file2));
        OpenSubtitlesSearchResponse.SubtitleData data2 = new OpenSubtitlesSearchResponse.SubtitleData();
        data2.setAttributes(attrs2);

        // Medium download count
        OpenSubtitlesSearchResponse.SubtitleFile file3 = new OpenSubtitlesSearchResponse.SubtitleFile();
        file3.setFileId(222);
        OpenSubtitlesSearchResponse.SubtitleAttributes attrs3 = new OpenSubtitlesSearchResponse.SubtitleAttributes();
        attrs3.setSubtitleId("sub3");
        attrs3.setDownloadCount(50);
        attrs3.setFiles(List.of(file3));
        OpenSubtitlesSearchResponse.SubtitleData data3 = new OpenSubtitlesSearchResponse.SubtitleData();
        data3.setAttributes(attrs3);

        OpenSubtitlesSearchResponse response = new OpenSubtitlesSearchResponse();
        response.setData(List.of(data1, data2, data3));
        return response;
    }
}
