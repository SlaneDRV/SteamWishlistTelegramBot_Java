package MainFunctions.DataManageFunctions;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;
import java.util.stream.Collectors;

public class FindGame {

    public static List<Map.Entry<String, JSONObject>> findGamesByTag(String searchTag, Map<String, Object> database) {
        System.out.println("Starting the search for games by tag...");

        if (database == null) {
            System.out.println("Database is null.");
            return Collections.emptyList();
        }

        Map<String, JSONObject> results = new HashMap<>();
        String lowerCaseSearchTag = searchTag.toLowerCase();

        for (Map.Entry<String, Object> entry : database.entrySet()) {
            try {
                String gameId = entry.getKey();
                Object gameDataObj = entry.getValue();

                if (!(gameDataObj instanceof Map)) {
                    System.out.println("Game data is not a valid Map for game ID: " + gameId);
                    continue;
                }

                Map<String, Object> gameDataMap = (Map<String, Object>) gameDataObj;
                JSONObject gameData = new JSONObject(gameDataMap);

                if (gameData.isEmpty()) {
                    System.out.println("Game data is empty for game ID: " + gameId);
                    continue;
                }

                if (gameData.has("TopTags")) {
                    JSONArray topTagsArray = gameData.optJSONArray("TopTags");

                    if (topTagsArray != null) {
                        for (int i = 0; i < topTagsArray.length(); i++) {
                            String tag = topTagsArray.optString(i).toLowerCase();
                            double similarity = calculateLevenshteinSimilarity(lowerCaseSearchTag, tag);

                            if (similarity > 0.6) {
                                results.put(gameId, gameData);
                                break;
                            }
                        }
                    } else {
                        System.out.println("TopTags is not a valid List for game ID: " + gameId);
                    }
                } else {
                    System.out.println("No TopTags found for game ID: " + gameId);
                }

            } catch (Exception e) {
                System.out.println("Error processing game ID: " + entry.getKey() + " - " + e.getMessage());
                e.printStackTrace();
            }
        }

        List<Map.Entry<String, JSONObject>> sortedResults = new ArrayList<>(results.entrySet());
        sortedResults.sort((entry1, entry2) -> {
            int totalReviews1 = entry1.getValue().optInt("PositiveReviews", 0) + entry1.getValue().optInt("NegativeReviews", 0);
            int totalReviews2 = entry2.getValue().optInt("PositiveReviews", 0) + entry2.getValue().optInt("NegativeReviews", 0);
            return Integer.compare(totalReviews2, totalReviews1);
        });

        System.out.println("Search games by tag is done. Games found: " + sortedResults.size());

        return sortedResults.size() > 20 ? sortedResults.subList(0, 20) : sortedResults;
    }


    private static double calculateLevenshteinSimilarity(String s1, String s2) {
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / Math.max(s1.length(), s2.length());
    }

    private static int levenshteinDistance(String s1, String s2) {
        int[][] dp = new int[s1.length() + 1][s2.length() + 1];

        for (int i = 0; i <= s1.length(); i++) {
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    dp[i][j] = j;
                } else if (j == 0) {
                    dp[i][j] = i;
                } else {
                    dp[i][j] = Math.min(Math.min(
                                    dp[i - 1][j] + 1,
                                    dp[i][j - 1] + 1),
                            dp[i - 1][j - 1] + (s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1));
                }
            }
        }
        return dp[s1.length()][s2.length()];
    }

    public static List<Map.Entry<String, JSONObject>> findGamesByName(String gameName, Map<String, Object> database) {
        System.out.println("Search games by name has been started.");
        Map<String, JSONObject> results = new HashMap<>();
        String searchQuery = gameName.toLowerCase().replace(" ", "");

        for (Map.Entry<String, Object> entry : database.entrySet()) {
            String gameId = entry.getKey();
            Object gameDataObj = entry.getValue();

            if (!(gameDataObj instanceof Map)) {
                System.out.println("Invalid game data for game ID: " + gameId);
                continue;
            }

            Map<String, Object> gameDataMap = (Map<String, Object>) gameDataObj;
            JSONObject gameData = new JSONObject(gameDataMap);

            String name = gameData.optString("Name").toLowerCase().replace(" ", "");

            double ratio = calculateLevenshteinSimilarity(searchQuery, name);

            if (ratio > 0.6 || name.contains(searchQuery)) {
                int totalReviews = gameData.optInt("PositiveReviews", 0) + gameData.optInt("NegativeReviews", 0);
                results.put(gameId, gameData);
            }
        }

        List<Map.Entry<String, JSONObject>> sortedResults = new ArrayList<>(results.entrySet());
        sortedResults.sort((x, y) -> {
            int totalReviews1 = x.getValue().optInt("PositiveReviews", 0) + x.getValue().optInt("NegativeReviews", 0);
            int totalReviews2 = y.getValue().optInt("PositiveReviews", 0) + y.getValue().optInt("NegativeReviews", 0);
            return Integer.compare(totalReviews2, totalReviews1);
        });

        System.out.println("Search games by name is done.");
        return sortedResults.stream().limit(10).collect(Collectors.toList());
    }
}
