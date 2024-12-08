package MainFunctions.DataManageFunctions;

import MainFunctions.DataManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class DatabaseFunctions {

    public static CompletableFuture<Void> preloadDatabase() {
        return CompletableFuture.runAsync(() -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();

                List<Map<String, Object>> gameList = objectMapper.readValue(
                        new File(DataManager.getGamesPath()),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                DataManager.DATABASE = new HashMap<>();
                for (Map<String, Object> game : gameList) {
                    String gameId = String.valueOf(game.get("ID"));
                    DataManager.DATABASE.put(gameId, game);
                }

                System.out.println("Database preloaded successfully.");
            } catch (IOException e) {
                System.out.println("JSON database file not found or error decoding JSON data: " + e.getMessage());
            }
        });
    }

    public static Map<String, Object> readDatabase() {
        if (DataManager.DATABASE == null || DataManager.DATABASE.isEmpty()) {
            System.out.println("Database is not loaded.");
            return null;
        } else {
            System.out.println("Connected to JSON database successfully.");
            return DataManager.DATABASE;
        }
    }
}
