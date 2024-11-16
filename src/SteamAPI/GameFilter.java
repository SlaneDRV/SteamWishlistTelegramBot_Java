package SteamAPI;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GameFilter {

    private static final String GAMES_FILE = "Data/JSON/Games/existing_games_actual.json"; // Путь к файлу с данными об играх

    public static void main(String[] args) {
        List<Map<String, Object>> filteredGames = getGamesWithReviewsOverThreshold(1000);
        printGames(filteredGames);
    }

    /**
     * Метод для загрузки игр с отзывами более указанного порога
     */
    public static List<Map<String, Object>> getGamesWithReviewsOverThreshold(int threshold) {
        List<Map<String, Object>> gamesList = loadGamesFromFile(GAMES_FILE);

        return gamesList.stream()
                .filter(game -> (Integer) game.getOrDefault("PositiveReviews", 0) > threshold)
                .sorted((game1, game2) -> Integer.compare(
                        (Integer) game2.getOrDefault("PositiveReviews", 0),
                        (Integer) game1.getOrDefault("PositiveReviews", 0)
                ))
                .collect(Collectors.toList());
    }

    /**
     * Метод для загрузки игр из JSON-файла
     */
    private static List<Map<String, Object>> loadGamesFromFile(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, Object>> gamesList = new ArrayList<>();

        try {
            File file = new File(filePath);
            if (file.exists()) {
                gamesList = mapper.readValue(file, new TypeReference<List<Map<String, Object>>>() {});
            } else {
                System.out.println("Файл не найден: " + filePath);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return gamesList;
    }

    /**
     * Метод для печати названий игр с количеством отзывов
     */
    private static void printGames(List<Map<String, Object>> games) {
        System.out.println("Игры с положительными отзывами > 1000:");
        for (Map<String, Object> game : games) {
            String name = (String) game.get("Name");
            int positiveReviews = (Integer) game.get("PositiveReviews");
            System.out.println(name + " - Положительные отзывы: " + positiveReviews);
        }
    }
}
