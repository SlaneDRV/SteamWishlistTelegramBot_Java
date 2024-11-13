package MainFunctions.DataManageFunctions;

import MainFunctions.DataManager;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class Database {

    public static CompletableFuture<Void> preloadDatabase() {
        return CompletableFuture.runAsync(() -> {
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                DataManager.DATABASE = objectMapper.readValue(new File(DataManager.getGamesPath()), HashMap.class);
                System.out.println("Database preloaded successfully.");
            } catch (IOException e) {
                System.out.println("JSON database file not found or error decoding JSON data: " + e.getMessage());
            }
        });
    }

    public static Map<String, Object> readDatabase() {
        if (DataManager.DATABASE == null) {
            System.out.println("Database is not loaded.");
            return null;
        } else {
            System.out.println("Connected to JSON database successfully.");
            return DataManager.DATABASE;
        }
    }

}
