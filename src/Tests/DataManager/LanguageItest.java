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

    private Language languageHandler; // Экземпляр обработчика языков
    private Database database;       // Экземпляр базы данных (тестовая)

    @BeforeEach
    void setUp() {
        // Создаем реальные или тестовые экземпляры классов
        database = new Database(); // Здесь можно использовать тестовую БД (например, SQLite)
        languageHandler = new Language();
        
        // Связываем обработчик с базой данных
        languageHandler.setDatabase(database);

        // Наполняем тестовую базу данных двумя играми: одна с языками, другая без языков
        Map<String, Object> game1 = new HashMap<>();
        game1.put("ID", 12345);
        game1.put("Name", "Dota 2");
        game1.put("LanguagesSub", List.of("English", "Spanish")); // Языки субтитров
        game1.put("LanguagesAudio", List.of("English", "French")); // Языки озвучки

        Map<String, Object> game2 = new HashMap<>();
        game2.put("ID", 67890);
        game2.put("Name", "American"); // У этой игры нет данных о языках

        // Добавляем данные в базу
        database.writeToDatabase("12345", game1);
        database.writeToDatabase("67890", game2);
    }

    @Test
    void testShowAvailableLanguages_Integration() {
        // Создаем тестовый CallbackQuery, который имитирует действие пользователя
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message telegramMessage = mock(Message.class);

        // Задаем данные, как будто пользователь запросил языки для игры с ID 12345
        when(callbackQuery.getData()).thenReturn("lang_12345");
        when(callbackQuery.getMessage()).thenReturn(telegramMessage);
        when(telegramMessage.getChatId()).thenReturn(123L); // Имитируем идентификатор чата Telegram

        // Вызываем метод обработчика для обработки события
        String result = languageHandler.showAvailableLanguages(callbackQuery);

        // Ожидаемый результат для игры "Dota 2"
        String expected = "Available Languages for Dota 2:\n\n" +
                "Subtitles:\nEnglish\nSpanish\n\n" +
                "Audio:\nEnglish\nFrench";

        // Проверяем, совпадает ли результат с ожидаемым
        assertEquals(expected, result, "Languages should match expected output.");
    }

    @Test
    void testShowAvailableLanguages_Integration_GameNotFound() {
        // Создаем CallbackQuery для игры, которой нет в базе данных
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message telegramMessage = mock(Message.class);

        // Указываем данные, как будто пользователь запросил игру с несуществующим ID
        when(callbackQuery.getData()).thenReturn("lang_99999");
        when(callbackQuery.getMessage()).thenReturn(telegramMessage);
        when(telegramMessage.getChatId()).thenReturn(789L); // Идентификатор чата Telegram

        // Вызываем метод обработчика
        String result = languageHandler.showAvailableLanguages(callbackQuery);

        // Ожидаемый результат: игра не найдена
        String expected = "Game details not found.";

        // Проверяем, что возвращаемое сообщение соответствует ожиданиям
        assertEquals(expected, result, "Should return 'Game details not found.' for unknown games.");
    }
}
