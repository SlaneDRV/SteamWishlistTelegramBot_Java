package IntegrationTests;

import MainFunctions.DataManageFunctions.Database;
import MainFunctions.DataManageFunctions.FindExactGame;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * �������������� ���� ��� ������ FindExactGame.
 * ��������� �������������� ������� ������ � ����� ������.
 */
class FindExactGameIntegrationTest {

    private Database database; // ��������� ���� ������
    private Map<String, Object> dbData; // ������������� ���� ������ � ���� Map

    /**
     * �����, ������������� ����� ������ ������.
     * ����������� �������� ���� ������ � ��������� �������.
     */
    @BeforeEach
    void setUp() {
        // �������������� ���� ������
        database = new Database();
        dbData = new HashMap<>();

        // ������� �������� ������ ��� ������ ����
        Map<String, Object> game1 = new HashMap<>();
        game1.put("ID", 12345);
        game1.put("Name", "Dota 2");
        game1.put("LanguagesSub", List.of("English", "Spanish")); // ��������
        game1.put("LanguagesAudio", List.of("English", "French")); // �������
        game1.put("PositiveReviews", 150); // ������������� ������
        game1.put("NegativeReviews", 10); // ������������� ������

        // ��������� ������ ���� � ���� ������
        dbData.put(String.valueOf(game1.get("ID")), game1);

        // ������� �������� ������ ��� ������ ���� (��� �������� ������)
        Map<String, Object> game2 = new HashMap<>();
        game2.put("ID", 67890);
        game2.put("Name", "American");
        // ��� �������� ������
        game2.put("PositiveReviews", 50);
        game2.put("NegativeReviews", 5);

        // ��������� ������ ���� � ���� ������
        dbData.put(String.valueOf(game2.get("ID")), game2);

        // ������� �������� ������ ��� ������� ���� � ����������� �������
        Map<String, Object> game3 = new HashMap<>();
        game3.put("ID", 99999);
        game3.put("Name", "InvalidGame");
        game3.put("LanguagesSub", "Not a list"); // ������������ ��� ������
        game3.put("LanguagesAudio", null); // ����������� ������
        game3.put("PositiveReviews", "Many"); // ������������ ��� ������
        // ������������� ������ �����������

        // ��������� ������ ���� � ���� ������
        dbData.put(String.valueOf(game3.get("ID")), game3);

        // ���������� ��� �������� ������ � ���� ������
        database.writeDatabase(dbData);
    }

    /**
     * ������������ ������ findGameByExactName � ������������ �����.
     * ���������, ��� ����� ���������� ���������� ������ ��� ���� "Dota 2".
     */
    @Test
    void testFindGameByExactName_Found() {
        // ������ ��� ���� ��� ������
        String gameName = "Dota 2";

        // �������� ����� ������ �� �����
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database.readDatabase());

        // ���������, ��� ��������� �� ������ � �������� ���� ����
        assertFalse(results.isEmpty(), "���������� ������ �� ������ ���� �������.");
        assertEquals(1, results.size(), "������ ���� ������� ������ ���� ����.");

        // �������� ��������� ����
        JSONObject foundGame = results.get(0);

        // ���������, ��� �������� ���� ������������� ��������
        assertEquals(gameName, foundGame.optString("Name"), "�������� ���� ������ ��������� � �������.");

        // ��������� ������ ������ ���������� �������
        int expectedTotalReviews = 150 + 10; // ������������� + �������������
        assertEquals(expectedTotalReviews, foundGame.optInt("TotalReviews"), "����� ���������� ������� ������ ���� ����������.");

        // ��������� �������� ������
        JSONArray subtitles = foundGame.optJSONArray("LanguagesSub");
        JSONArray audio = foundGame.optJSONArray("LanguagesAudio");

        assertNotNull(subtitles, "������ ���� �������� ��������.");
        assertNotNull(audio, "������ ���� ��������� �������.");

        assertEquals(2, subtitles.length(), "������ ���� ��� ��������.");
        assertEquals("English", subtitles.getString(0));
        assertEquals("Spanish", subtitles.getString(1));

        assertEquals(2, audio.length(), "������ ���� ��� �������.");
        assertEquals("English", audio.getString(0));
        assertEquals("French", audio.getString(1));
    }

    /**
     * ������������ ������ findGameByExactName � �����, ������� ��� � ���� ������.
     * ���������, ��� ��������� ����� ������ �������.
     */
    @Test
    void testFindGameByExactName_NotFound() {
        // ������ ��� ����, ������� ��� � ���� ������
        String gameName = "Unknown Game";

        // �������� ����� ������ �� �����
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database.readDatabase());

        // ���������, ��� ��������� ������
        assertTrue(results.isEmpty(), "���������� ������ ������ ���� ������� ��� �������������� ����.");
    }

    /**
     * ������������ ������ findGameByExactName � �����, ������� ���������� ������.
     * ���������, ��� ����� ���� ������������ � �� ������������ � �����������.
     */
    @Test
    void testFindGameByExactName_InvalidData() {
        // ������ ��� ���� � ����������� �������
        String gameName = "InvalidGame";

        // �������� ����� ������ �� �����
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database.readDatabase());

        // ���������, ��� ���� � ����������� ������� �� ������������
        assertTrue(results.isEmpty(), "���� � ����������� ������� �� ������ ������������ � �����������.");
    }

    /**
     * ������������ ������ findGameByExactId � ������������ �����.
     * ���������, ��� ����� ���������� ���������� ������ ��� ���� � ID 12345.
     */
    @Test
    void testFindGameByExactId_Found() {
        // ������ ID ���� ��� ������
        String gameId = "12345";

        // �������� ����� ������ �� ID
        List<JSONObject> results = FindExactGame.findGameByExactId(gameId, database.readDatabase());

        // ���������, ��� ��������� �� ������ � �������� ���� ����
        assertFalse(results.isEmpty(), "���������� ������ �� ������ ���� �������.");
        assertEquals(1, results.size(), "������ ���� ������� ������ ���� ����.");

        // �������� ��������� ����
        JSONObject foundGame = results.get(0);

        // ���������, ��� ID ���� ������������� ��������
        assertEquals(Integer.parseInt(gameId), foundGame.optInt("ID"), "ID ���� ������ ��������� � �������.");

        // ���������, ��� �������� ���� �������������
        assertEquals("Dota 2", foundGame.optString("Name"), "�������� ���� ������ ��������� � ���������.");

        // ��������� �������� ������
        JSONArray subtitles = foundGame.optJSONArray("LanguagesSub");
        JSONArray audio = foundGame.optJSONArray("LanguagesAudio");

        assertNotNull(subtitles, "������ ���� �������� ��������.");
        assertNotNull(audio, "������ ���� ��������� �������.");

        assertEquals(2, subtitles.length(), "������ ���� ��� ��������.");
        assertEquals("English", subtitles.getString(0));
        assertEquals("Spanish", subtitles.getString(1));

        assertEquals(2, audio.length(), "������ ���� ��� �������.");
        assertEquals("English", audio.getString(0));
        assertEquals("French", audio.getString(1));
    }

    /**
     * ������������ ������ findGameByExactId � �����, ������� ��� � ���� ������.
     * ���������, ��� ��������� ����� ������ �������.
     */
    @Test
    void testFindGameByExactId_NotFound() {
        // ������ ID ����, ������� ��� � ���� ������
        String gameId = "99999"; // ��������������, ��� ��� ���� ���� �����������, ���� ����� ���������� ������

        // �������� ����� ������ �� ID
        List<JSONObject> results = FindExactGame.findGameByExactId(gameId, database.readDatabase());

        // ���������, ��� ��������� ������
        assertTrue(results.isEmpty(), "���������� ������ ������ ���� ������� ��� �������������� ����.");
    }

    /**
     * ������������ ������ findGameByExactId � �����, ������� ���������� ������.
     * ���������, ��� ����� ���� ������������ � �� ������������ � �����������.
     */
    @Test
    void testFindGameByExactId_InvalidData() {
        // ������ ID ���� � ����������� �������
        String gameId = "99999"; // � ����� ������ ���� � ID 99999 ����� ���������� ������

        // �������� ����� ������ �� ID
        List<JSONObject> results = FindExactGame.findGameByExactId(gameId, database.readDatabase());

        // ���������, ��� ���� � ����������� ������� �� ������������
        assertTrue(results.isEmpty(), "���� � ����������� ������� �� ������ ������������ � �����������.");
    }

    /**
     * �������������� ����: ����� �� ����� � ������ ��������� � ���������.
     * ���������, ��� ����� �������������� � �������� � ��������.
     */
    @Test
    void testFindGameByExactName_CaseInsensitiveAndTrim() {
        // ������ ��� ���� � ������� ���������� � ���������
        String gameName = "  dota 2  ";

        // �������� ����� ������ �� �����
        List<JSONObject> results = FindExactGame.findGameByExactName(gameName, database.readDatabase());

        // ���������, ��� ��������� �� ������ � �������� ���� ����
        assertFalse(results.isEmpty(), "���������� ������ �� ������ ���� �������.");
        assertEquals(1, results.size(), "������ ���� ������� ������ ���� ����.");

        // �������� ��������� ����
        JSONObject foundGame = results.get(0);

        // ���������, ��� �������� ���� ������������� ��������
        assertEquals("Dota 2", foundGame.optString("Name"), "�������� ���� ������ ��������� � �������.");
    }
}
