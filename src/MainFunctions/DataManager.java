package MainFunctions;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class DataManager {

    private static final String TGBOT_DIR = "TgBot";
    private static final String STEAMAPI_DIR = "SteamAPI";
    private static final String JSON_DIR = "JSON";
    private static final String DETAILED_GAMES_FILE = "detailed_games_transformed.json";

    private static Map<String, Object> DATABASE;

    public static String getDetailedGamesPath() {
        return String.join(File.separator, TGBOT_DIR, STEAMAPI_DIR, JSON_DIR, DETAILED_GAMES_FILE);
    }

    public static CompletableFuture<Void> preloadDatabase() {
        return CompletableFuture.runAsync(() -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                DATABASE = objectMapper.readValue(new File(getDetailedGamesPath()), HashMap.class);
                System.out.println("Database preloaded successfully.");
            } catch (IOException e) {
                System.out.println("JSON database file not found or error decoding JSON data: " + e.getMessage());
            }
        });
    }

    public static Map<String, Object> readDatabase() {
        if (DATABASE == null) {
            System.out.println("Database is not loaded.");
            return null;
        } else {
            System.out.println("Connected to JSON database successfully.");
            return DATABASE;
        }
    }


    public static List<JSONObject> sortWishlistByDate(List<JSONObject> wishlist, Map<String, Object> database) {
        List<Map.Entry<JSONObject, LocalDate>> sortedWishlist = new ArrayList<>();

        DateTimeFormatter standardFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");

        for (JSONObject game : wishlist) {
            String gameId = game.optString("ID");
            if (!gameId.isEmpty()) {
                List<JSONObject> dbGame = findGameByExactId(gameId, database);
                if (!dbGame.isEmpty()) {
                    JSONObject gameData = dbGame.get(0);
                    String releaseDate = gameData.optString("ReleaseDate");

                    LocalDate parsedDate = parseReleaseDate(releaseDate, standardFormatter);
                    if (parsedDate != null) {
                        sortedWishlist.add(new AbstractMap.SimpleEntry<>(game, parsedDate));
                    }
                }
            }
        }

        sortedWishlist.sort((x, y) -> y.getValue().compareTo(x.getValue()));
        return sortedWishlist.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private static LocalDate parseReleaseDate(String releaseDate, DateTimeFormatter standardFormatter) {
        try {
            if (releaseDate.equalsIgnoreCase("To be announced")) {
                return LocalDate.MAX;
            } else if (releaseDate.matches("\\d{4}")) {
                return LocalDate.parse(releaseDate + "-01-01");
            } else {
                return LocalDate.parse(releaseDate, standardFormatter);
            }
        } catch (DateTimeParseException e) {

            return null;
        }
    }


    public static List<JSONObject> sortWishlistByReviews(List<JSONObject> wishlist, Map<String, Object> database) {
        List<Map.Entry<JSONObject, Integer>> sortedWishlist = new ArrayList<>();

        for (JSONObject game : wishlist) {
            String gameId = game.optString("ID");
            if (!gameId.isEmpty()) {
                List<JSONObject> dbGame = findGameByExactId(gameId, database);
                if (!dbGame.isEmpty()) {
                    JSONObject gameData = dbGame.get(0);
                    int positiveReviews = gameData.optInt("PositiveReviews", 0);
                    int negativeReviews = gameData.optInt("NegativeReviews", 0);
                    int totalReviews = positiveReviews + negativeReviews;
                    sortedWishlist.add(new AbstractMap.SimpleEntry<>(game, totalReviews));
                }
            }
        }

        sortedWishlist.sort((x, y) -> Integer.compare(y.getValue(), x.getValue()));
        return sortedWishlist.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public static List<JSONObject> sortWishlistByAlphabet(List<JSONObject> wishlist) {

        wishlist.sort(Comparator.comparing(game -> game.optString("Name", "").toLowerCase()));
        return wishlist;
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

    public static List<Map.Entry<String, JSONObject>> findGamesByTag(String searchTag, Map<String, Object> database) {
        System.out.println("Starting the search for games by tag...");

        if (database == null) {
            System.out.println("Database is null.");
            return Collections.emptyList();
        }

        Map<String, JSONObject> results = new HashMap<>();

        System.out.println("Total number of games in the database: " + database.size());
        System.out.println("Is database empty? " + database.isEmpty());

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
                        Set<String> tagsSet = new HashSet<>();

                        for (int i = 0; i < topTagsArray.length(); i++) {
                            tagsSet.add(topTagsArray.optString(i).toLowerCase());
                        }

                        if (tagsSet.contains(searchTag.toLowerCase())) {
                            results.put(gameId, gameData);
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

            if (ratio > 0.7 || name.contains(searchQuery)) {
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

    private static final String WISHLIST_DIR = "Wishlists";

    private static String getWishlistPath(long userId) {
        return WISHLIST_DIR + File.separator + userId + "_wishlist.json";
    }

    public static List<JSONObject> readWishlist(long userId) {
        String filename = getWishlistPath(userId);
        System.out.println("Reading wishlist from: " + filename);

        if (!Files.exists(Paths.get(filename))) {
            System.out.println("Wishlist file does not exist: " + filename);
            return new ArrayList<>();
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));

            JSONArray jsonArray = new JSONArray(content);
            List<JSONObject> wishlist = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                wishlist.add(jsonArray.getJSONObject(i));
            }
            return wishlist;

        } catch (IOException e) {
            System.out.println("Error reading wishlist file: " + e.getMessage());
            return new ArrayList<>();
        } catch (JSONException e) {
            System.out.println("Invalid JSON format in wishlist file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void saveWishlist(long userId, List<JSONObject> wishlist) {
        String filename = getWishlistPath(userId);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            System.out.println("Save wishlist for user: " + userId);
            writer.write(wishlist.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<JSONObject> addGameToWishlist(long userId, JSONObject game) {
        List<JSONObject> wishlist = readWishlist(userId);

        if (!checkWishlist(userId, game.optString("Name"))) {
            wishlist.add(game);
            saveWishlist(userId, wishlist);
            System.out.println("Added game to wishlist of user: " + userId);
        }

        return wishlist;
    }

    public static boolean checkWishlist(long userId, String gameName) {
        List<JSONObject> wishlist = readWishlist(userId);

        for (JSONObject game : wishlist) {
            if (game.optString("Name").equals(gameName)) {
                return true;
            }
        }
        return false;
    }

    public static List<JSONObject> removeGameFromWishlist(long userId, String gameName) {
        System.out.println("Remove game from wishlist of user: " + userId);
        List<JSONObject> wishlist = readWishlist(userId);
        List<JSONObject> newWishlist = new ArrayList<>();
        for (JSONObject game : wishlist) {
            if (!game.optString("Name").equals(gameName)) {
                newWishlist.add(game);
            }
        }
        saveWishlist(userId, newWishlist);
        return newWishlist;
    }

    public static String generateWishlistFileTxt(long userId) {
        List<JSONObject> wishlist = readWishlist(userId);
        String filename = String.format("wishlist_%d.txt", userId);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (JSONObject game : wishlist) {
                String price = game.optDouble("Price", 0.0) != 0.0 ? String.valueOf(game.optDouble("Price")) : "Free";
                writer.write(String.format("%s: %s - %s%n", game.optString("ID"), game.optString("Name"), price));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filename;
    }

    public static List<JSONObject> filterWishlistFields(List<JSONObject> wishlist) {
        List<JSONObject> filteredWishlist = new ArrayList<>();

        for (JSONObject game : wishlist) {
            JSONObject filteredGame = new JSONObject();
            filteredGame.put("ID", game.optString("ID"));
            filteredGame.put("Name", game.optString("Name"));
            filteredGame.put("Price", game.optDouble("Price", 0.0) != 0.0 ? String.valueOf(game.optDouble("Price")) : "Free");
            filteredWishlist.add(filteredGame);
        }

        return filteredWishlist;
    }

    public static String generateWishlistFileJson(long userId) {
        List<JSONObject> wishlist = readWishlist(userId);
        List<JSONObject> filteredWishlist = filterWishlistFields(wishlist);
        String filename = String.format("wishlist_%d.json", userId);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(filteredWishlist.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filename;
    }

    public static String generateWishlistFileYaml(long userId) {
        List<JSONObject> wishlist = readWishlist(userId);
        List<Map<String, Object>> filteredWishlist = filterWishlistFields(wishlist).stream()
                .map(JSONObject::toMap)
                .collect(Collectors.toList());

        String filename = String.format("wishlist_%d.yaml", userId);

        Yaml yaml = new Yaml();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            yaml.dump(filteredWishlist, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filename;
    }





    public static void mergeWishlists(long userId, List<JSONObject> importedData) {

        List<JSONObject> currentWishlist = readWishlist(userId);

        for (JSONObject importedGame : importedData) {
            String importedGameId = importedGame.optString("ID");
            String importedGameName = importedGame.optString("Name");

            boolean existsInWishlist = currentWishlist.stream().anyMatch(existingGame ->
                    importedGameId.equals(existingGame.optString("ID")) &&
                            importedGameName.equals(existingGame.optString("Name"))
            );

            if (!existsInWishlist) {
                currentWishlist.add(importedGame);
            }
        }

        saveWishlist(userId, currentWishlist);
    }

    public static List<JSONObject> readTxtFile(byte[] fileContent) {
        List<JSONObject> importedData = new ArrayList<>();
        String content = new String(fileContent);
        String[] lines = content.strip().split("\n");

        for (String line : lines) {

            int lastDashIndex = line.lastIndexOf(" - ");
            if (lastDashIndex != -1) {
                String idAndName = line.substring(0, lastDashIndex);
                String price = line.substring(lastDashIndex + 3);

                int firstColonIndex = idAndName.indexOf(':');
                if (firstColonIndex != -1) {
                    String gameId = idAndName.substring(0, firstColonIndex).trim();
                    String gameName = idAndName.substring(firstColonIndex + 1).trim();
                    JSONObject gameInfo = new JSONObject();
                    gameInfo.put("ID", Integer.parseInt(gameId));
                    gameInfo.put("Name", gameName);
                    gameInfo.put("Price", price.strip());
                    importedData.add(gameInfo);
                } else {
                    System.out.println("Error parsing line (ID and Name): " + line);
                }
            } else {
                System.out.println("Error parsing line (Price): " + line);
            }
        }
        System.out.println(" Successfuly readig of TXT FILE !!!");
        return importedData;
    }

    public static List<JSONObject> readYamlFile(byte[] fileContent) {
        Yaml yaml = new Yaml();
        List<JSONObject> importedData = new ArrayList<>();

        List<Map<String, Object>> yamlData = yaml.load(new ByteArrayInputStream(fileContent));
        if (yamlData != null) {
            for (Map<String, Object> gameData : yamlData) {

                importedData.add(new JSONObject(gameData));
            }
        }
        System.out.println(" Successfuly readig of YAML FILE !!!");
        return importedData;
    }

    public static List<JSONObject> readJsonFile(byte[] downloadedFile) {
        List<JSONObject> gamesList = new ArrayList<>();

        try {

            String jsonContent = new String(downloadedFile);
            JSONArray jsonArray = new JSONArray(jsonContent);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject game = jsonArray.getJSONObject(i);
                gamesList.add(game);
            }

        } catch (Exception e) {
            System.out.println("Error reading JSON file: " + e.getMessage());
            e.printStackTrace();
        }

        return gamesList;
    }
}
