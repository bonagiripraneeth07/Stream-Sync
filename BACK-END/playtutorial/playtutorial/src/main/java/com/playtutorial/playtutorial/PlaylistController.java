package com.playtutorial.playtutorial;

import org.python.util.PythonInterpreter;
import org.python.core.*;
import com.squareup.okhttp.HttpUrl;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.MultiValueMap;
import org.springframework.util.LinkedMultiValueMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

@RestController
@RequestMapping("/api/playlists")
@CrossOrigin(origins = "http://127.0.0.1:5500")
public class PlaylistController {
    private final RestTemplate restTemplate = new RestTemplate();
    private final YoutubeService youtubeService = new YoutubeService();

    private final String SPOTIFY_CLIENT_ID = "YOUR SPOTIFY CLIENT ID";
    private final String SPOTIFY_CLIENT_SECRET = "YOUR SPOTIFY CLIENT SECRET";

    @GetMapping
    public ResponseEntity<?> fetchPlaylist(@RequestParam String url) {
        String platform = detectPlatform(url);
        String playlistId = extractPlaylistId(url, platform);
        System.out.println("Received URL: from 41 " + url);
        System.out.println("platform from 42 :" + platform);
        if (platform == null && playlistId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid or unsupported playlist URL"));
        }
        try {
            System.out.println(platform);
            System.out.println("Received URL: " + url);
            List<String> songs = null;
            String playlistName = "Unknown Playlist";
            if (platform.equals("spotify")) {
                songs = extractSongsFromSpotify(playlistId, getSpotifyAccessToken());
                playlistName = "Spotify Playlist";
                System.out.println("from spotify");
                System.out.println("Converting songs to YouTube...");
                Map<String, Object> youtubePlaylist = youtubeService.getYoutubePlaylist(playlistName, songs);
                System.out.println("YouTube playlist generated successfully!");
                return ResponseEntity.ok(youtubePlaylist);

            } else if (platform.equals("jiosaavn")) {
                playlistName = "JioSaavn Playlist";
                System.out.println("from jiosaavan");
                songs = getJioSaavnPlaylist(url);
                System.out.println("Converting songs to YouTube...");
                Map<String, Object> youtubePlaylist = youtubeService.getYoutubePlaylist(playlistName, songs);
                System.out.println("YouTube playlist generated successfully!");
                return ResponseEntity.ok(youtubePlaylist);

            } else if (platform.equals("gaana")) {
                System.out.println("from gaana");
                playlistName = "Gaana Playlist";
                songs = fetchGaanaPlaylist(url);
                System.out.println("Converting songs to YouTube...");
                Map<String, Object> youtubePlaylist = youtubeService.getYoutubePlaylist(playlistName, songs);
                System.out.println("YouTube playlist generated successfully!");

                return ResponseEntity.ok(youtubePlaylist);

            }

            System.out.println("Converting songs to YouTube...");
            Map<String, Object> youtubePlaylist = youtubeService.getYoutubePlaylist(playlistName, songs);
            System.out.println("YouTube playlist generated successfully!");

            return ResponseEntity.ok(youtubePlaylist);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }

    }

    private String detectPlatform(String url) {
        System.out.println("Checking platform for URL: " + url);
        if (url.contains("spotify.com")) {
            System.out.println("Detected Spotify");
            return "spotify";
        }
        if (url.contains("jiosaavn.com")) {
            System.out.println("Detected JioSaavn");
            return "jiosaavn";
        }
        if (url.contains("gaana.com")) {
            System.out.println("Detected Gaana");
            return "gaana";
        }
        System.out.println("Unknown platform detected");
        return "unknown";
    }

    private String extractPlaylistId(String url, String platform) {
        if ("spotify".equals(platform)) {
            Matcher matcher = Pattern.compile("playlist/([a-zA-Z0-9]+)").matcher(url);
            return matcher.find() ? matcher.group(1) : null;
        } else if ("jiosaavn".equals(platform)) {
            Matcher matcher = Pattern.compile("album/([a-zA-Z0-9]+)").matcher(url);
            return matcher.find() ? matcher.group(1) : null;
        } else if ("gaana".equals(platform)) {
            Matcher matcher = Pattern.compile("playlist/([a-zA-Z0-9]+)").matcher(url);
            return matcher.find() ? matcher.group(1) : null;
        }
        return null;
    }

    // Spotify Access Token
    private String getSpotifyAccessToken() {
        String url = "https://accounts.spotify.com/api/token";
        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(SPOTIFY_CLIENT_ID, SPOTIFY_CLIENT_SECRET);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "client_credentials");

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        return response.getBody().get("access_token").toString();
    }

    private List<String> extractSongsFromSpotify(String playlistId, String accessToken) {
        String url = "https://api.spotify.com/v1/playlists/" + playlistId + "/tracks";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("Content-Type", "application/json");

        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        String body = response.getBody();

        JSONObject root = new JSONObject(response.getBody());
        JSONArray items = root.getJSONArray("items");

        List<String> songNames = new ArrayList<>();
        for (int i = 0; i < items.length(); i++) {
            JSONObject trackObject = items.getJSONObject(i).getJSONObject("track");
            String songName = trackObject.getString("name");
            songNames.add(songName);
            System.out.println(songName);
        }

        return songNames;
    }

    // fetching jiosavavn
    private final OkHttpClient client = new OkHttpClient();

    public List<String> getJioSaavnPlaylist(String playlistUrl) throws IOException {

        HttpUrl.Builder urlBuilder = HttpUrl.parse("https://saavn.dev/api/playlists")
                .newBuilder()
                .addQueryParameter("link", playlistUrl)
                .addQueryParameter("limit", "50");

        String apiUrl = urlBuilder.build().toString();
        System.out.println("Requesting JioSaavn API: " + apiUrl);

        Request request = new Request.Builder()
                .url(apiUrl)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                System.err.println("Error: " + response.code() + " - " + response.message());
                return new ArrayList<>();
            }

            String responseBody = response.body().string();
            JSONObject jsonResponse = new JSONObject(responseBody);

            if (jsonResponse.has("data") && jsonResponse.getJSONObject("data").has("songs")) {
                JSONObject dataObject = jsonResponse.getJSONObject("data");
                JSONArray songsArray = dataObject.getJSONArray("songs");

                List<String> songNames = new ArrayList<>();
                for (int i = 0; i < songsArray.length(); i++) {
                    JSONObject song = songsArray.getJSONObject(i);
                    songNames.add(song.getString("name"));
                }

                System.out.println("Extracted Songs: " + songNames);
                return songNames;
            } else {
                System.err.println("Error: 'data' or 'songs' not found in JSON response.");
                return new ArrayList<>();
            }

        }
    }

}
