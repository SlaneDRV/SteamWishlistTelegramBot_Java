package IntegrationTests;

import MainFunctions.DataManageFunctions.Database;
import MainFunctions.DataManageFunctions.FindExactGame;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Интеграционный тест для класса FindExactGame.
 * Проверяет взаимодействие методов поиска с базой данных.
 */
class FindExactGameIntegrationTest {

    private Database database; // Экземпляр базы данных
    private Map<String, Object> dbData; // Представление базы данных в виде Map

    /**
     * Метод, выполняющийся перед каждым тестом.
     * Настраивает тестовую базу данных с тестовыми данными.
     */
    @BeforeEach
    void setUp() {
        // Инициализируем базу данных
        database = new Database();
        dbData = new HashMap<>();

        // Создаем тестовые данные для первой игры
        Map<String, Object> game1 = new HashMap<>();
        game1.put("ID", 12345);
        game1.put("Name", "Dota 2");
        game1.put("LanguagesSub", List.of("English", "Spanish")); // Субтитры
        game1.put("LanguagesAudio", List.of("English", "French")); // Озвучка
        game1.put("PositiveReviews", 150); // Положительные отзывы
        game1.put("NegativeReviews", 10); // Отрицательные отзывы

        // Добавляем первую игру в базу данных
        dbData.put(String.valueOf(game1.get("ID")), game1);

        // Создаем тестовые данные для второй игры (без языковых данных)
        Map<String, Object> game2 = new HashMap<>();
        game2.put("ID", 67890);
        game2.put("Name", "American");
        // Нет языковых данных
        game2.put("PositiveReviews", 50);
        game2.put("NegativeReviews", 5);

        // Добавляем вторую игру в базу данных
        dbData.put(String.valueOf(game2.get("ID")), game2);

        // Создаем тестовые данные для третьей игры с невалидными данными
        Map<String, Object> game3 = new HashMap<>();
        game3.put("ID", 99999);
        game3.put("Name", "InvalidGame");
        game3.put("LanguagesSub", "Not a list"); // Неправильный тип данных
        game3.put("LanguagesAudio", null); // Отсутствуют данные
        game3.put("PositiveReviews", "Many"); // Неправильный тип данных
        // Отрицательные отзывы отсутствуют

        // Добавляем третью игру в базу данных
        dbData.put(String.valueOf(game3.get("ID")), game3);

        // Записываем все тестовые данные в базу данных
        database.writeDatabase(dbData);
    }

    /**
     * Тестирование метода findGameByExactName с существующей игрой.
     * Проверяет, что метод возвращает корректные данные для игры "Dota 2".
     */
    @Test
    void testFindGameByExactName_Found() {
        // Задаем имя игры для поиска
        String gameName = "Dota 2";

        // Вызываем метод поиска по имени
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database.readDatabase());

        // Проверяем, что результат не пустой и содержит одну игру
        assertFalse(results.isEmpty(), "Результаты поиска не должны быть пустыми.");
        assertEquals(1, results.size(), "Должна быть найдена только одна игра.");

        // Получаем найденную игру
        JSONObject foundGame = results.get(0);

        // Проверяем, что название игры соответствует искомому
        assertEquals(gameName, foundGame.optString("Name"), "Название игры должно совпадать с искомым.");

        // Проверяем расчет общего количества отзывов
        int expectedTotalReviews = 150 + 10; // Положительные + отрицательные
        assertEquals(expectedTotalReviews, foundGame.optInt("TotalReviews"), "Общее количество отзывов должно быть корректным.");

        // Проверяем языковые данные
        JSONArray subtitles = foundGame.optJSONArray("LanguagesSub");
        JSONArray audio = foundGame.optJSONArray("LanguagesAudio");

        assertNotNull(subtitles, "Должны быть доступны субтитры.");
        assertNotNull(audio, "Должны быть доступные озвучки.");

        assertEquals(2, subtitles.length(), "Должно быть два субтитра.");
        assertEquals("English", subtitles.getString(0));
        assertEquals("Spanish", subtitles.getString(1));

        assertEquals(2, audio.length(), "Должно быть две озвучки.");
        assertEquals("English", audio.getString(0));
        assertEquals("French", audio.getString(1));
    }

    /**
     * Тестирование метода findGameByExactName с игрой, которой нет в базе данных.
     * Ожидается, что результат будет пустым списком.
     */
    @Test
    void testFindGameByExactName_NotFound() {
        // Задаем имя игры, которой нет в базе данных
        String gameName = "Unknown Game";

        // Вызываем метод поиска по имени
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database.readDatabase());

        // Проверяем, что результат пустой
        assertTrue(results.isEmpty(), "Результаты поиска должны быть пустыми для несуществующей игры.");
    }

    /**
     * Тестирование метода findGameByExactName с игрой, имеющей невалидные данные.
     * Проверяет, что такие игры игнорируются и не возвращаются в результатах.
     */
    @Test
    void testFindGameByExactName_InvalidData() {
        // Задаем имя игры с невалидными данными
        String gameName = "InvalidGame";

        // Вызываем метод поиска по имени
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database.readDatabase());

        // Проверяем, что игра с невалидными данными не возвращается
        assertTrue(results.isEmpty(), "Игры с невалидными данными не должны возвращаться в результатах.");
    }

    /**
     * Тестирование метода findGameByExactId с существующей игрой.
     * Проверяет, что метод возвращает корректные данные для игры с ID 12345.
     */
    @Test
    void testFindGameByExactId_Found() {
        // Задаем ID игры для поиска
        String gameId = "12345";

        // Вызываем метод поиска по ID
        List<JSONObject> results = FindExactGame.findGameByExactId(gameId, database.readDatabase());

        // Проверяем, что результат не пустой и содержит одну игру
        assertFalse(results.isEmpty(), "Результаты поиска не должны быть пустыми.");
        assertEquals(1, results.size(), "Должна быть найдена только одна игра.");

        // Получаем найденную игру
        JSONObject foundGame = results.get(0);

        // Проверяем, что ID игры соответствует искомому
        assertEquals(Integer.parseInt(gameId), foundGame.optInt("ID"), "ID игры должно совпадать с искомым.");

        // Проверяем, что название игры соответствует
        assertEquals("Dota 2", foundGame.optString("Name"), "Название игры должно совпадать с ожидаемым.");

        // Проверяем языковые данные
        JSONArray subtitles = foundGame.optJSONArray("LanguagesSub");
        JSONArray audio = foundGame.optJSONArray("LanguagesAudio");

        assertNotNull(subtitles, "Должны быть доступны субтитры.");
        assertNotNull(audio, "Должны быть доступные озвучки.");

        assertEquals(2, subtitles.length(), "Должно быть два субтитра.");
        assertEquals("English", subtitles.getString(0));
        assertEquals("Spanish", subtitles.getString(1));

        assertEquals(2, audio.length(), "Должно быть две озвучки.");
        assertEquals("English", audio.getString(0));
        assertEquals("French", audio.getString(1));
    }

    /**
     * Тестирование метода findGameByExactId с игрой, которой нет в базе данных.
     * Ожидается, что результат будет пустым списком.
     */
    @Test
    void testFindGameByExactId_NotFound() {
        // Задаем ID игры, которой нет в базе данных
        String gameId = "99999"; // Предполагается, что эта игра либо отсутствует, либо имеет невалидные данные

        // Вызываем метод поиска по ID
        List<JSONObject> results = FindExactGame.findGameByExactId(gameId, database.readDatabase());

        // Проверяем, что результат пустой
        assertTrue(results.isEmpty(), "Результаты поиска должны быть пустыми для несуществующей игры.");
    }

    /**
     * Тестирование метода findGameByExactId с игрой, имеющей невалидные данные.
     * Проверяет, что такие игры игнорируются и не возвращаются в результатах.
     */
    @Test
    void testFindGameByExactId_InvalidData() {
        // Задаем ID игры с невалидными данными
        String gameId = "99999"; // В нашем случае игра с ID 99999 имеет невалидные данные

        // Вызываем метод поиска по ID
        List<JSONObject> results = FindExactGame.findGameByExactId(gameId, database.readDatabase());

        // Проверяем, что игра с невалидными данными не возвращается
        assertTrue(results.isEmpty(), "Игры с невалидными данными не должны возвращаться в результатах.");
    }

    /**
     * Дополнительный тест: поиск по имени с разным регистром и пробелами.
     * Проверяет, что поиск нечувствителен к регистру и пробелам.
     */
    @Test
    void testFindGameByExactName_CaseInsensitiveAndTrim() {
        // Задаем имя игры с разными регистрами и пробелами
        String gameName = "  dota 2  ";

        // Вызываем метод поиска по имени
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database.readDatabase());

        // Проверяем, что результат не пустой и содержит одну игру
        assertFalse(results.isEmpty(), "Результаты поиска не должны быть пустыми.");
        assertEquals(1, results.size(), "Должна быть найдена только одна игра.");

        // Получаем найденную игру
        JSONObject foundGame = results.get(0);

        // Проверяем, что название игры соответствует искомому
        assertEquals("Dota 2", foundGame.optString("Name"), "Название игры должно совпадать с искомым.");
    }
}
