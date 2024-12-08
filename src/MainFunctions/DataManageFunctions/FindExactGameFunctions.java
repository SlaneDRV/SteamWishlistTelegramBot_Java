package MainFunctions.DataManageFunctions;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FindExactGameFunctions {

    public static List<JSONObject> findGameByExactName(String gameName, Map<String, Object> database) {
        System.out.println("Search game by exact name has been started.");
        List<JSONObject> results = new ArrayList<>();
        String searchQuery = gameName.toLowerCase().trim();

        for (Map.Entry<String, Object> entry : database.entrySet()) {
            String gameId = entry.getKey();
            Object gameDataObj = entry.getValue();

            if (!(gameDataObj instanceof Map)) {
                System.out.println("Game data is not a valid Map for game ID: " + gameId);
                continue;
            }

            JSONObject gameData = new JSONObject((Map<String, Object>) gameDataObj);
            String name = gameData.optString("Name").toLowerCase().trim();
            if (name.equals(searchQuery)) {
                int totalReviews = gameData.optInt("PositiveReviews", 0) + gameData.optInt("NegativeReviews", 0);
                gameData.put("TotalReviews", totalReviews);
                results.add(gameData);
            }
        }

        results.sort((x, y) -> Integer.compare(y.optInt("TotalReviews", 0), x.optInt("TotalReviews", 0)));
        System.out.println("Search game by exact name is done.");
        return results.stream().limit(1).collect(Collectors.toList());
    }

    public static List<JSONObject> findGameByExactId(String gameId, Map<String, Object> database) {
        System.out.println("Search game by exact id has been started.");
        List<JSONObject> results = new ArrayList<>();
        String searchQuery = String.valueOf(gameId).trim();

        for (Map.Entry<String, Object> entry : database.entrySet()) {
            Object gameDataObj = entry.getValue();

            if (!(gameDataObj instanceof Map)) {
                System.out.println("Game data is not a valid Map for game ID: " + entry.getKey());
                continue;
            }

            String dbGameIdStr = String.valueOf(((Map<String, Object>) gameDataObj).get("ID")).trim();
            if (dbGameIdStr.equals(searchQuery)) {
                results.add(new JSONObject((Map<String, Object>) gameDataObj));
            }
        }

        System.out.println("Search game by exact id is done.");
        return results.isEmpty() ? new ArrayList<>() : results.subList(0, 1);
    }
}
