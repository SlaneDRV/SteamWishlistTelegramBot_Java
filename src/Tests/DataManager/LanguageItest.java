package IntegrationTests;

import MainFunctions.DataManageFunctions.Database;
import MainFunctions.HandlersFunctions.Language;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LanguageIntegrationTest {

    private Language languageHandler; // ��������� ����������� ������
    private Database database;       // ��������� ���� ������ (��������)

    @BeforeEach
    void setUp() {
        // ������� �������� ��� �������� ���������� �������
        database = new Database(); // ����� ����� ������������ �������� �� (��������, SQLite)
        languageHandler = new Language();
        
        // ��������� ���������� � ����� ������
        languageHandler.setDatabase(database);

        // ��������� �������� ���� ������ ����� ������: ���� � �������, ������ ��� ������
        Map<String, Object> game1 = new HashMap<>();
        game1.put("ID", 12345);
        game1.put("Name", "Dota 2");
        game1.put("LanguagesSub", List.of("English", "Spanish")); // ����� ���������
        game1.put("LanguagesAudio", List.of("English", "French")); // ����� �������

        Map<String, Object> game2 = new HashMap<>();
        game2.put("ID", 67890);
        game2.put("Name", "American"); // � ���� ���� ��� ������ � ������

        // ��������� ������ � ����
        database.writeToDatabase("12345", game1);
        database.writeToDatabase("67890", game2);
    }

    @Test
    void testShowAvailableLanguages_Integration() {
        // ������� �������� CallbackQuery, ������� ��������� �������� ������������
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message telegramMessage = mock(Message.class);

        // ������ ������, ��� ����� ������������ �������� ����� ��� ���� � ID 12345
        when(callbackQuery.getData()).thenReturn("lang_12345");
        when(callbackQuery.getMessage()).thenReturn(telegramMessage);
        when(telegramMessage.getChatId()).thenReturn(123L); // ��������� ������������� ���� Telegram

        // �������� ����� ����������� ��� ��������� �������
        String result = languageHandler.showAvailableLanguages(callbackQuery);

        // ��������� ��������� ��� ���� "Dota 2"
        String expected = "Available Languages for Dota 2:\n\n" +
                "Subtitles:\nEnglish\nSpanish\n\n" +
                "Audio:\nEnglish\nFrench";

        // ���������, ��������� �� ��������� � ���������
        assertEquals(expected, result, "Languages should match expected output.");
    }

    @Test
    void testShowAvailableLanguages_Integration_GameNotFound() {
        // ������� CallbackQuery ��� ����, ������� ��� � ���� ������
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message telegramMessage = mock(Message.class);

        // ��������� ������, ��� ����� ������������ �������� ���� � �������������� ID
        when(callbackQuery.getData()).thenReturn("lang_99999");
        when(callbackQuery.getMessage()).thenReturn(telegramMessage);
        when(telegramMessage.getChatId()).thenReturn(789L); // ������������� ���� Telegram

        // �������� ����� �����������
        String result = languageHandler.showAvailableLanguages(callbackQuery);

        // ��������� ���������: ���� �� �������
        String expected = "Game details not found.";

        // ���������, ��� ������������ ��������� ������������� ���������
        assertEquals(expected, result, "Should return 'Game details not found.' for unknown games.");
    }
}
