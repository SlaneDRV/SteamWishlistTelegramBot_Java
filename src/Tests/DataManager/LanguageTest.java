package Tests.HandlersFunctions;

import MainFunctions.DataManageFunctions.Database;
import MainFunctions.DataManageFunctions.FindExactGame;
import MainFunctions.HandlersFunctions.Language;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class LanguageTest {

    private Language languageHandler;
    private Database mockDatabase;
    private Message mockMessage;

    @BeforeEach
    void setUp() {
        languageHandler = new Language();
        mockDatabase = mock(Database.class);
        mockMessage = mock(Message.class);
    }

    @Test
    void testShowAvailableLanguages_FoundWithLanguages() {
        // Przygotowanie danych gry
        Map<String, Object> gameData = Map.of(
                "ID", 12345,
                "Name", "Dota 2",
                "LanguagesSub", List.of("English", "Spanish"),
                "LanguagesAudio", List.of("English", "French")
        );
        JSONObject game = new JSONObject(gameData);

        when(mockDatabase.readDatabase()).thenReturn(Map.of("12345", gameData));

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        when(callbackQuery.getData()).thenReturn("lang_12345");
        Message telegramMessage = mock(Message.class);
        when(callbackQuery.getMessage()).thenReturn(telegramMessage);
        when(telegramMessage.getChatId()).thenReturn(123L);

        languageHandler.showAvailableLanguages(callbackQuery);

        verify(mockMessage).sendMessage(123L, "Available Languages for Test Game:\n\n" +
                "Subtitles:\nEnglish\nSpanish\n\n" +
                "Audio:\nEnglish\nFrench");
    }

    @Test
    void testShowAvailableLanguages_FoundWithoutLanguages() {
        // Gra bez dostępnych języków
        Map<String, Object> gameData = Map.of(
                "ID", 67890,
                "Name", "American"
        );
        JSONObject game = new JSONObject(gameData);

        when(mockDatabase.readDatabase()).thenReturn(Map.of("67890", gameData));

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        when(callbackQuery.getData()).thenReturn("lang_67890");
        Message telegramMessage = mock(Message.class);
        when(callbackQuery.getMessage()).thenReturn(telegramMessage);
        when(telegramMessage.getChatId()).thenReturn(456L);

        languageHandler.showAvailableLanguages(callbackQuery);

        verify(mockMessage).sendMessage(456L, "Available Languages for No Language Game:\n\n" +
                "Subtitles:\nNo subtitles available.\n\n" +
                "Audio:\nNo audio available.");
    }

    @Test
    void testShowAvailableLanguages_GameNotFound() {
        // Brak gry w bazie danych
        when(mockDatabase.readDatabase()).thenReturn(Map.of());

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        when(callbackQuery.getData()).thenReturn("lang_99999");
        Message telegramMessage = mock(Message.class);
        when(callbackQuery.getMessage()).thenReturn(telegramMessage);
        when(telegramMessage.getChatId()).thenReturn(789L);

        languageHandler.showAvailableLanguages(callbackQuery);

        verify(mockMessage).sendMessage(789L, "Game details not found.");
    }

    @Test
    void testShowAvailableLanguages_InvalidGameData() {
        // Nieprawidłowe dane gry
        when(mockDatabase.readDatabase()).thenReturn(Map.of("99999", "Invalid Data"));

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        when(callbackQuery.getData()).thenReturn("lang_99999");
        Message telegramMessage = mock(Message.class);
        when(callbackQuery.getMessage()).thenReturn(telegramMessage);
        when(telegramMessage.getChatId()).thenReturn(789L);

        languageHandler.showAvailableLanguages(callbackQuery);

        verify(mockMessage).sendMessage(789L, "Game details not found.");
    }
}
