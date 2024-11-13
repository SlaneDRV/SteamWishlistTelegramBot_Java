import MainFunctions.Config;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.util.Map;

public class GameInfoUpdater {

    private static final String EXISTING_GAMES_FILE = "existing_games.json";
    private static final String INVALID_GAMES_FILE = "invalid_games.json";
    private static final String API_KEY = Config.STEAM_KEY; // Замените на ваш API-ключ Steam

    public static void main(String[] args) {
        int appid = 2005791; // Замените на нужный ID игры

        try {
            Map<String, Object> gameInfo = updateGameInfo(appid);
            if (gameInfo != null) {
                System.out.println("Собранная информация об игре:");
            } else {
                System.out.println("Информация об игре не получена.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Map<String, Object> updateGameInfo(int appid) throws IOException {
        CloseableHttpClient httpClient = SteamGameProcessor.createHttpClient();
        Map<String, Object> gameInfo = SteamGameProcessor.processGame(appid, httpClient);

        if (gameInfo != null) {
            // Печатаем всю собранную информацию об игре
            System.out.println("Информация об игре:");
            gameInfo.forEach((key, value) -> System.out.println(key + ": " + value));

            // Сохраняем или обновляем информацию об игре в EXISTING_GAMES_FILE
            SteamGameProcessor.saveOrUpdateGameInfo(gameInfo, EXISTING_GAMES_FILE);
            System.out.println("Игра успешно сохранена в " + EXISTING_GAMES_FILE);

        } else {
            // Если информация недоступна или игра невалидная, добавляем её в список недействительных игр
            System.out.println("Не удалось получить полную информацию для игры с ID " + appid + ". Игра добавлена в список недействительных игр.");

            // Создание базовой информации для недействительной игры
            gameInfo = Map.of("ID", appid, "Name", "Invalid Game");

            // Сохранение недействительной игры в INVALID_GAMES_FILE
            SteamGameProcessor.saveOrUpdateGameInfo(gameInfo, INVALID_GAMES_FILE);
            System.out.println("Игра успешно сохранена в " + INVALID_GAMES_FILE);
        }

        httpClient.close();
        return gameInfo;
    }
}
