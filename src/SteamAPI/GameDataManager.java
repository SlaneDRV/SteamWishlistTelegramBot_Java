package SteamAPI;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GameDataManager {

    private static final String EXISTING_GAMES_FILE = "Data/JSON/Games/existing_games_actual.json";
    private static final String INVALID_GAMES_FILE = "Data/JSON/Games/invalid_games.json";

    private static ObjectMapper mapper;

    public GameDataManager() {
        mapper = new ObjectMapper();
    }

    // Загрузка существующих ID игр
    public Set<Integer> loadExistingGameIds() {
        Set<Integer> existingGameIds = new HashSet<>();

        existingGameIds.addAll(loadGameIdsFromFile(EXISTING_GAMES_FILE));
        existingGameIds.addAll(loadGameIdsFromFile(INVALID_GAMES_FILE));

        return existingGameIds;
    }

    private Set<Integer> loadGameIdsFromFile(String fileName) {
        Set<Integer> gameIds = new HashSet<>();
        File file = new File(fileName);
        if (file.exists()) {
            try {
                List<Map<String, Object>> games = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
                for (Map<String, Object> game : games) {
                    Object id = game.get("ID");
                    if (id instanceof String) {
                        gameIds.add(Integer.parseInt((String) id));
                    } else if (id instanceof Integer) {
                        gameIds.add((Integer) id);
                    } else {
                        throw new IllegalArgumentException("Unexpected ID type: " + id.getClass().getName());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return gameIds;
    }

    // Сохранение информации об играх
    public void saveGamesInfo(List<Map<String, Object>> gamesInfo, String fileName) {
        try {
            File file = new File(fileName);
            List<Map<String, Object>> existingGames = new ArrayList<>();
            if (file.exists()) {
                existingGames = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
            }
            existingGames.addAll(gamesInfo);
            mapper.writerWithDefaultPrettyPrinter().writeValue(file, existingGames);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Сохранение недействительных игр
    public void saveInvalidGame(int appid, String gameName) {
        File file = new File(INVALID_GAMES_FILE);
        List<Map<String, Object>> invalidGames = new ArrayList<>();

        if (file.exists()) {
            try {
                invalidGames = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Map<String, Object> invalidGameInfo = new HashMap<>();
        invalidGameInfo.put("ID", appid);
        invalidGameInfo.put("Name", gameName != null && !gameName.isEmpty() ? gameName : "Invalid Game");

        boolean exists = invalidGames.stream().anyMatch(game -> game.get("ID").equals(appid));

        if (!exists) {
            invalidGames.add(invalidGameInfo);
            try {
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, invalidGames);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // Сохранение или обновление информации об игре
    public static void saveOrUpdateGameInfo(Map<String, Object> gameInfo, String fileName) {
        try {
            File file = new File(fileName);
            List<Map<String, Object>> gamesList = new ArrayList<>();

            if (file.exists()) {
                gamesList = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
            }

            boolean updated = false;
            for (int i = 0; i < gamesList.size(); i++) {
                Map<String, Object> existingGame = gamesList.get(i);
                if (existingGame.get("ID").equals(gameInfo.get("ID"))) {
                    gamesList.set(i, gameInfo);
                    updated = true;
                    break;
                }
            }

            if (!updated) {
                gamesList.add(gameInfo);
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(file, gamesList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
