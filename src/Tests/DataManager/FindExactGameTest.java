package Tests.DataManager;

import MainFunctions.DataManageFunctions.FindExactGame;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class FindExactGameTest {

    private Map<String, Object> database;

    @BeforeEach
    void setUp() {
        // Подготавливаем базу данных
        database = Map.of(
                "2621440", Map.of(
                        "ID", 2621440,
                        "Name", "Infinity Castle Dungeon",
                        "PositiveReviews", 0,
                        "NegativeReviews", 0
                ),
                "1168650", Map.of(
                        "ID", 1168650,
                        "Name", "Angry Bunny",
                        "PositiveReviews", 28,
                        "NegativeReviews", 26
                ),
                "1235460", Map.of(
                        "ID", 1235460,
                        "Name", "Angry Bunny 2: Lost hole",
                        "PositiveReviews", 29,
                        "NegativeReviews", 16
                )
        );
    }

    @Test
    void testFindGameByExactName_Found() {
        // Проверка на успешное нахождение игры по имени
        String gameName = "Angry Bunny";
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database);

        assertNotNull(results);
        assertEquals(1, results.size());
        JSONObject game = results.get(0);
        assertEquals("Angry Bunny", game.getString("Name"));
        assertEquals(28 + 26, game.getInt("TotalReviews"));
    }

    @Test
    void testFindGameByExactName_NotFound() {
        // Проверка, если игра с таким именем не найдена
        String gameName = "Nonexistent Game";
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindGameByExactId_Found() {
        // Проверка на успешное нахождение игры по ID
        String gameId = "1235460";
        List<JSONObject> results = FindExactGame.findGameByExactId(gameId, database);

        assertNotNull(results);
        assertEquals(1, results.size());
        JSONObject game = results.get(0);
        assertEquals(1235460, game.getInt("ID"));
        assertEquals("Angry Bunny 2: Lost hole", game.getString("Name"));
    }

    @Test
    void testFindGameByExactId_NotFound() {
        // Проверка, если игра с таким ID не найдена
        String gameId = "9999999";
        List<JSONObject> results = FindExactGame.findGameByExactId(gameId, database);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    void testFindGameByExactName_CaseInsensitive() {
        // Проверка на регистронезависимый поиск
        String gameName = "infinity castle dungeon";
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database);

        assertNotNull(results);
        assertEquals(1, results.size());
        JSONObject game = results.get(0);
        assertEquals("Infinity Castle Dungeon", game.getString("Name"));
    }

    @Test
    void testFindGameByExactId_InvalidData() {
        // Проверка, если в базе данных есть некорректные данные
        Map<String, Object> corruptedDatabase = Map.of(
                "invalidKey", "This is not a valid game object"
        );

        List<JSONObject> results = FindExactGame.findGameByExactId("1235460", corruptedDatabase);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }
}
