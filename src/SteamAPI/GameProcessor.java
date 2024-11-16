package SteamAPI;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GameProcessor {

    private SteamApiClient steamApiClient;
    private SteamSpyClient steamSpyClient;
    private GameDataManager dataManager;

    public GameProcessor() {
        steamApiClient = new SteamApiClient();
        steamSpyClient = new SteamSpyClient(steamApiClient.httpClient);
        dataManager = new GameDataManager();
    }

    public Map<String, Object> processGame(int appid) {
        try {
            JsonNode steamDetails = steamApiClient.fetchSteamGameDetails(appid, "US");
            if (steamDetails == null) {
                dataManager.saveInvalidGame(appid, null);
                return null;
            }

            Map<String, Object> gameInfo = new LinkedHashMap<>();

            gameInfo.put("ID", appid);
            gameInfo.put("Name", steamDetails.path("name").asText("Unknown"));
            gameInfo.put("ImageURL", steamDetails.path("header_image").asText("No image available"));

            // Price
            JsonNode priceNode = steamDetails.path("price_overview");
            String price = priceNode.has("final_formatted") ? priceNode.get("final_formatted").asText("Price not available") : "N/A";
            gameInfo.put("Price", price);

            // Developers and Publishers
            gameInfo.put("Developer", extractFirstItemOrDefault(steamDetails.path("developers"), "Unknown"));
            gameInfo.put("Publisher", extractFirstItemOrDefault(steamDetails.path("publishers"), "Unknown"));

            // SteamSpy data
            Map<String, Object> steamSpyData = steamSpyClient.fetchSteamSpyData(appid);
            gameInfo.put("PositiveReviews", steamSpyData.getOrDefault("positive", 0));
            gameInfo.put("NegativeReviews", steamSpyData.getOrDefault("negative", 0));
            gameInfo.put("DayPeak", steamSpyData.getOrDefault("ccu", 0));
            gameInfo.put("TopTags", getTopTags(steamSpyData));

            // Languages
            Map<String, List<String>> languages = parseSupportedLanguages(steamDetails.path("supported_languages").asText("Not available"));
            gameInfo.put("LanguagesSub", languages.get("Subtitles"));
            gameInfo.put("LanguagesAudio", languages.get("Full Audio"));

            // Additional fields
            gameInfo.put("ShortDesc", steamDetails.path("short_description").asText("No description available"));
            gameInfo.put("ReleaseDate", steamDetails.path("release_date").path("date").asText("Unknown"));
            gameInfo.put("Genres", extractListFromNode(steamDetails.path("genres"), "description"));
            gameInfo.put("Categories", extractListFromNode(steamDetails.path("categories"), "description"));

            // Platforms
            JsonNode platformsNode = steamDetails.path("platforms");
            String platformsStr = platformsNode.isObject() ?
                    StreamSupport.stream(Spliterators.spliteratorUnknownSize(platformsNode.fields(), 0), false)
                            .filter(entry -> entry.getValue().asBoolean(false))
                            .map(Map.Entry::getKey)
                            .collect(Collectors.joining(", "))
                    : "Unknown";
            gameInfo.put("Platforms", platformsStr);
            gameInfo.put("CollectedDate", LocalDate.now().toString());

            return gameInfo;

        } catch (Exception e) {
            System.out.println("Error processing game ID " + appid + ": " + e.getMessage());
            dataManager.saveInvalidGame(appid, null);
            return null;
        }
    }

    public String getGamePriceByRegion(int appid, String region) {
        JsonNode steamDetails = steamApiClient.fetchSteamGameDetails(appid, region);
        if (steamDetails == null) {
            return "Price not available in region " + region;
        }

        JsonNode priceNode = steamDetails.path("price_overview");
        return priceNode.has("final_formatted")
                ? priceNode.get("final_formatted").asText("Price not available")
                : "Price not available";
    }


    // Helper methods
    private String extractFirstItemOrDefault(JsonNode arrayNode, String defaultValue) {
        return arrayNode.isArray() && arrayNode.size() > 0 ? arrayNode.get(0).asText() : defaultValue;
    }

    private List<String> extractListFromNode(JsonNode arrayNode, String field) {
        List<String> result = new ArrayList<>();
        if (arrayNode.isArray()) {
            arrayNode.forEach(item -> {
                if (item.has(field)) {
                    result.add(item.get(field).asText());
                }
            });
        }
        return result.isEmpty() ? Collections.singletonList("Unknown") : result;
    }

    private List<String> getTopTags(Map<String, Object> steamSpyData) {
        Object tagsObject = steamSpyData.get("tags");

        if (tagsObject instanceof Map) {
            Map<String, Integer> tags = (Map<String, Integer>) tagsObject;
            return tags.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } else {
            return Collections.singletonList("No tags found");
        }
    }

    private Map<String, List<String>> parseSupportedLanguages(String languagesHtml) {
        languagesHtml = languagesHtml.replaceAll("<br><strong>\\*</strong>languages with full audio support", "");
        languagesHtml = languagesHtml.replaceAll("<[^>]*>", "");
        String[] allLanguagesArray = languagesHtml.split(",");
        List<String> allLanguages = Arrays.stream(allLanguagesArray)
                .map(String::trim)
                .filter(lang -> !lang.isEmpty())
                .collect(Collectors.toList());
        List<String> fullAudio = allLanguages.stream()
                .filter(lang -> lang.contains("*"))
                .map(lang -> lang.replace("*", "").trim())
                .collect(Collectors.toList());
        List<String> subtitles = allLanguages.stream()
                .map(lang -> lang.replace("*", "").trim())
                .collect(Collectors.toList());
        Map<String, List<String>> result = new HashMap<>();
        result.put("Full Audio", fullAudio.isEmpty() ? Collections.singletonList("Not available") : fullAudio);
        result.put("Subtitles", subtitles.isEmpty() ? Collections.singletonList("Not available") : subtitles);
        return result;
    }

    public void close() throws Exception {
        steamApiClient.close();
    }
}
