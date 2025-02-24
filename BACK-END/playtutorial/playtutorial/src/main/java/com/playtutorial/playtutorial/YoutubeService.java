package com.playtutorial.playtutorial;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YoutubeService {
    private static final String YOUTUBE_API_KEY = "your Youtube Api key ";
    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private final RestTemplate restTemplate = new RestTemplate();

    public Map<String, Object> getYoutubePlaylist(String playlistName, List<String> songs) {
        List<Map<String, String>> youtubeSongs = new ArrayList<>();

        for (String song : songs) {
            String youtubeUrl = searchYoutube(song);
            if (youtubeUrl != null) {
                youtubeSongs.add(Map.of("song", song, "youtube_link", youtubeUrl));
            }
        }

        return Map.of(
                "playlist_name", playlistName,
                "platform", "YouTube",
                "songs", youtubeSongs);
    }

    private String searchYoutube(String songName) {
        String queryUrl = YOUTUBE_SEARCH_URL + "?part=snippet&q=" + songName + "&key=" + YOUTUBE_API_KEY
                + "&maxResults=1&type=video";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(queryUrl, String.class);
            JSONObject jsonResponse = new JSONObject(response.getBody());
            JSONArray items = jsonResponse.getJSONArray("items");
            System.out.println("from 40 yotube response :" + jsonResponse);
            if (items.length() > 0) {
                String videoId = items.getJSONObject(0).getJSONObject("id").getString("videoId");
                return "https://www.youtube.com/watch?v=" + videoId;
            }
        } catch (Exception e) {
            System.err.println("Error fetching YouTube link for: " + songName);
            e.printStackTrace();
        }

        return null;
    }
}
