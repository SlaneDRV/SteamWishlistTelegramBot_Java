package SteamAPI;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class SteamGamesProcessor {

    private GameDataManager dataManager;
    private GameProcessor gameProcessor;

    public SteamGamesProcessor() {
        dataManager = new GameDataManager();
        gameProcessor = new GameProcessor();
    }

    public static void main(String[] args) {
        SteamGamesProcessor processor = new SteamGamesProcessor();
        processor.run();
    }

    public void run() {
        try {
            Set<Integer> existingGameIds = dataManager.loadExistingGameIds();
            List<Integer> allGameIds = getAllGameIds();
            List<Integer> newGameIds = new ArrayList<>();

            for (Integer gameId : allGameIds) {
                if (!existingGameIds.contains(gameId)) {
                    newGameIds.add(gameId);
                }
            }

            System.out.println("Найдено новых игр: " + newGameIds.size());

            List<Map<String, Object>> newGamesInfo = new ArrayList<>();

            int processedGames = 0;
            for (Integer appid : newGameIds) {
                System.out.println("Processing game: " + appid);
                Map<String, Object> gameInfo = gameProcessor.processGame(appid);
                if (gameInfo != null) {
                    newGamesInfo.add(gameInfo);
                }

                processedGames++;

                // Задержка 1-2 секунды
                Thread.sleep(1000 + new Random().nextInt(1000));

                // Сохранение данных каждые 100 игр
                if (processedGames % 10 == 0) {
                    dataManager.saveGamesInfo(newGamesInfo, "Data/JSON/Games/existing_games_actual.json");
                    System.out.println("Сохранено игр: " + processedGames);
                    newGamesInfo.clear();
                }
            }

            // Сохранение оставшихся данных
            if (!newGamesInfo.isEmpty()) {
                dataManager.saveGamesInfo(newGamesInfo, "Data/JSON/Games/existing_games_actual.json");
                System.out.println("Сохранено игр: " + processedGames);
            }

            System.out.println("Обработка завершена. Новые игры сохранены.");

            gameProcessor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Получение всех ID игр из Steam API
    private List<Integer> getAllGameIds() {
        List<Integer> allGameIds = new ArrayList<>();
        String url = "http://api.steampowered.com/ISteamApps/GetAppList/v2";
        try {
            CloseableHttpClient client = new SteamApiClient().createHttpClient();
            org.apache.http.client.methods.HttpGet request = new HttpGet(url);
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

}
