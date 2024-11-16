package SteamAPI;

import java.io.IOException;
import java.util.Map;

public class GameInfoUpdater {

    private static final String EXISTING_GAMES_FILE = "Data/JSON/Games/existing_games_actual.json";
    private static final String INVALID_GAMES_FILE = "Data/JSON/Games/invalid_games.json";

    public static void main(String[] args) {
        try {
            int appid = 1086940;
            GameProcessor gameProcessor = new GameProcessor();
            GameDataManager dataManager = new GameDataManager();

            Map<String, Object> gameInfo = gameProcessor.processGame(1086940);

            if (gameInfo != null) {
                System.out.println("Информация об игре:");
                gameInfo.forEach((key, value) -> System.out.println(key + ": " + value));

                dataManager.saveOrUpdateGameInfo(gameInfo, EXISTING_GAMES_FILE);
                System.out.println("Игра успешно сохранена в " + EXISTING_GAMES_FILE);

            } else {
                System.out.println("Не удалось получить полную информацию для игры с ID " + appid + ". Игра добавлена в список недействительных игр.");

                gameInfo = Map.of("ID", appid, "Name", "Invalid Game");

                dataManager.saveOrUpdateGameInfo(gameInfo, INVALID_GAMES_FILE);
                System.out.println("Игра успешно сохранена в " + INVALID_GAMES_FILE);
            }

            gameProcessor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
