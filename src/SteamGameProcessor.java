import MainFunctions.Config;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SteamGameProcessor {

    private static final String EXISTING_GAMES_FILE = "Data/JSON/Games/existing_games.json";
    private static final String NEW_GAMES_FILE = "Data/JSON/Games/new_games.json";
    private static final String INVALID_GAMES_FILE = "Data/JSON/Games/invalid_games.json";
    private static final String API_KEY = Config.STEAM_KEY; // Замените на ваш API-ключ Steam

    // Создание HttpClient с установленной спецификацией куки
    public static CloseableHttpClient createHttpClient() {
        RequestConfig globalConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD) // Или CookieSpecs.IGNORE_COOKIES
                .build();

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(globalConfig)
                .build();
    }

    public static void main(String[] args) {
        try {
            Set<Integer> existingGameIds = loadExistingGameIds();
            List<Integer> allGameIds = getAllGameIds();
            List<Integer> newGameIds = new ArrayList<>();

            for (Integer gameId : allGameIds) {
                if (!existingGameIds.contains(gameId)) {
                    newGameIds.add(gameId);
                }
            }

            System.out.println("Найдено новых игр: " + newGameIds.size());

            List<Map<String, Object>> newGamesInfo = new ArrayList<>();
            CloseableHttpClient httpClient = createHttpClient();

            int processedGames = 0;
            for (Integer appid : newGameIds) {
                System.out.println("Processing game: " + appid);
                Map<String, Object> gameInfo = processGame(appid, httpClient);
                if (gameInfo != null) {
                    newGamesInfo.add(gameInfo);
                }

                processedGames++;

                // Задержка 1-2 секунды
                Thread.sleep(1000 + new Random().nextInt(1000));

                // Сохранение данных каждые 100 игр
                if (processedGames % 10 == 0) {
                    saveGamesInfo(newGamesInfo, NEW_GAMES_FILE);
                    System.out.println("Сохранено игр: " + processedGames);
                    newGamesInfo.clear(); // Очистить список после сохранения
                }
            }

            // Сохранение оставшихся данных, если есть
            if (!newGamesInfo.isEmpty()) {
                saveGamesInfo(newGamesInfo, NEW_GAMES_FILE);
                System.out.println("Сохранено игр: " + processedGames);
            }

            System.out.println("Обработка завершена. Новые игры сохранены в " + NEW_GAMES_FILE);

            httpClient.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Чтение существующих ID игр из JSON-файлов (existing_games.json и invalid_games.json)
    private static Set<Integer> loadExistingGameIds() {
        Set<Integer> existingGameIds = new HashSet<>();

        // Загрузка ID из existing_games.json
        existingGameIds.addAll(loadGameIdsFromFile(EXISTING_GAMES_FILE));

        // Загрузка ID из invalid_games.json
        existingGameIds.addAll(loadGameIdsFromFile(INVALID_GAMES_FILE));

        return existingGameIds;
    }

    // Метод для загрузки ID игр из указанного файла
    private static Set<Integer> loadGameIdsFromFile(String fileName) {
        Set<Integer> gameIds = new HashSet<>();
        File file = new File(fileName);
        if (file.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                List<Map<String, Object>> games = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> game : games) {
                    gameIds.add((Integer) game.get("ID"));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return gameIds;
    }

    // Получение всех ID игр из Steam API
    private static List<Integer> getAllGameIds() {
        List<Integer> allGameIds = new ArrayList<>();
        String url = "http://api.steampowered.com/ISteamApps/GetAppList/v2";
        try {
            CloseableHttpClient client = createHttpClient();
            HttpGet request = new HttpGet(url);
            HttpResponse response = client.execute(request);
            String jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> result = mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});
            Map<String, Object> applist = (Map<String, Object>) result.get("applist");
            List<Map<String, Object>> apps = (List<Map<String, Object>>) applist.get("apps");

            for (Map<String, Object> app : apps) {
                allGameIds.add((Integer) app.get("appid"));
            }

            client.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allGameIds;
    }

    // Обработка информации об одной игре
    public static Map<String, Object> processGame(int appid, CloseableHttpClient httpClient) {
        try {
            JsonNode steamDetails = fetchSteamGameDetails(appid, httpClient);
            if (steamDetails == null) {
                saveInvalidGame(appid, null);
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
            Map<String, Object> steamSpyData = fetchSteamSpyData(appid, httpClient);
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

            return gameInfo;

        } catch (Exception e) {
            System.out.println("Error processing game ID " + appid + ": " + e.getMessage());
            saveInvalidGame(appid, null);
            return null;
        }
    }


    // Helper methods
    private static String extractFirstItemOrDefault(JsonNode arrayNode, String defaultValue) {
        return arrayNode.isArray() && arrayNode.size() > 0 ? arrayNode.get(0).asText() : defaultValue;
    }

    private static List<String> extractListFromNode(JsonNode arrayNode, String field) {
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





    // Получение деталей об игре из Steam API
    // Измените метод fetchSteamGameDetails
    private static JsonNode fetchSteamGameDetails(int appid, CloseableHttpClient httpClient) {
        String url = String.format("http://store.steampowered.com/api/appdetails?appids=%d&cc=US&key=%s", appid, API_KEY);
        try {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            String jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonString);
            JsonNode appNode = rootNode.get(String.valueOf(appid));

            if (appNode == null || !appNode.path("success").asBoolean()) {
                System.out.println("Failed or missing data for appid " + appid);
                return null;
            }

            JsonNode dataNode = appNode.path("data");
            if (!dataNode.isObject()) {
                System.out.println("Unexpected 'data' format for appid " + appid);
                return null;
            }

            return dataNode;

        } catch (Exception e) {
            System.out.println("Error fetching details for appid " + appid + ": " + e.getMessage());
            return null;
        }
    }




    // Получение данных из SteamSpy
    private static Map<String, Object> fetchSteamSpyData(int appid, CloseableHttpClient httpClient) {
        String url = String.format("https://steamspy.com/api.php?request=appdetails&appid=%d", appid);
        String jsonString = "";
        try {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});

            return data;

        } catch (Exception e) {
            System.out.println("Error fetching SteamSpy data for appid " + appid + ": " + e.getMessage());
            System.out.println("Response JSON: " + jsonString);
            return new HashMap<>();
        }
    }


    // Получение топовых тегов из данных SteamSpy
// Получение топовых тегов из данных SteamSpy
    private static List<String> getTopTags(Map<String, Object> steamSpyData) {
        Object tagsObject = steamSpyData.get("tags");

        // Проверка, является ли tags словарём (Map) или списком (ArrayList)
        if (tagsObject instanceof Map) {
            Map<String, Integer> tags = (Map<String, Integer>) tagsObject;
            return tags.entrySet().stream()
                    .sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        } else {
            // Если tags не является словарём, возвращаем "No tags found"
            return Collections.singletonList("No tags found");
        }
    }


    // Получение цены игры
    private static String getPrice(Map<String, Object> steamDetails) {
        String price = "N/A";
        if ((Boolean) steamDetails.getOrDefault("is_free", false)) {
            price = "Free";
        } else if (steamDetails.containsKey("price_overview")) {
            Map<String, Object> priceOverview = (Map<String, Object>) steamDetails.get("price_overview");
            price = (String) priceOverview.getOrDefault("final_formatted", "Price not available");
        } else if ((Boolean) ((Map<String, Object>) steamDetails.getOrDefault("release_date", new HashMap<>())).getOrDefault("coming_soon", false)) {
            price = "Coming Soon";
        }
        return price;
    }

    // Парсинг поддерживаемых языков
    private static Map<String, List<String>> parseSupportedLanguages(String languagesHtml) {
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

    // Сохранение информации об играх в файл
    private static void saveGamesInfo(List<Map<String, Object>> gamesInfo, String fileName) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            File file = new File(fileName);
            List<Map<String, Object>> existingGames = new ArrayList<>();
            if (file.exists()) {
                // Загрузка существующих данных
                existingGames = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
            }
            existingGames.addAll(gamesInfo);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, existingGames);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Сохранение информации о недействительной игре в файл invalid_games.json
    public static void saveInvalidGame(int appid, String gameName) {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(INVALID_GAMES_FILE);
        List<Map<String, Object>> invalidGames = new ArrayList<>();

        // Загрузка существующих данных, если файл существует
        if (file.exists()) {
            try {
                invalidGames = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Создание записи об игре
        Map<String, Object> invalidGameInfo = new HashMap<>();
        invalidGameInfo.put("ID", appid);
        if (gameName != null && !gameName.isEmpty()) {
            invalidGameInfo.put("Name", gameName);
        } else {
            invalidGameInfo.put("Name", "Invalid Game");
        }

        // Проверка, существует ли игра уже в списке
        boolean exists = false;
        for (Map<String, Object> game : invalidGames) {
            if (((Integer) game.get("ID")).equals(appid)) {
                exists = true;
                break;
            }
        }

        // Если игра не существует, добавляем её
        if (!exists) {
            invalidGames.add(invalidGameInfo);
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, invalidGames);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Сохранение или обновление информации об игре в JSON-файл
    public static void saveOrUpdateGameInfo(Map<String, Object> gameInfo, String fileName) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(fileName);
        List<Map<String, Object>> gamesList = new ArrayList<>();

        // Загрузка существующих данных, если файл существует
        if (file.exists()) {
            gamesList = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
        }

        // Проверка, существует ли игра уже в списке
        boolean updated = false;
        for (int i = 0; i < gamesList.size(); i++) {
            Map<String, Object> existingGame = gamesList.get(i);
            if (((Integer) existingGame.get("ID")).equals(gameInfo.get("ID"))) {
                // Замена существующей информации об игре
                gamesList.set(i, gameInfo);
                updated = true;
                break;
            }
        }

        // Если игра не существует, добавляем её
        if (!updated) {
            gamesList.add(gameInfo);
        }

        // Сохранение обновленного списка обратно в файл
        mapper.writerWithDefaultPrettyPrinter().writeValue(file, gamesList);
    }
}
