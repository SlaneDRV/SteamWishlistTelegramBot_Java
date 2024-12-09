package MainFunctions.HandlersFunctions;

import MainFunctions.DataManageFunctions.Database;
import MainFunctions.DataManageFunctions.FindGame;
import MainFunctions.DataManageFunctions.WishlistFunctions;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

class SearchsTest {

    // Моки зависимостей
    @Mock
    private Handlers mockHandler; // Мок объекта для обработки сообщений Telegram.

    @Mock
    private WishlistFunctions mockWishlistFunctions; // Мок для работы с "wishlist" (списком желаемого).

    @Mock
    private Database mockDatabase; // Мок базы данных.

    @Mock
    private FindGame mockFindGame; // Мок для поиска игр.

    private Searchs searchs; // Тестируемый объект.

    // Метод, который выполняется перед каждым тестом
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // Инициализация моков.
        searchs = new Searchs(); // Создание экземпляра тестируемого класса.
        searchs.handler = mockHandler; // Подключение мока обработчика.
    }

    // Тест: поиск игр по тегу, когда игры найдены
    @Test
    void testSearchGameByTag_withResults() throws TelegramApiException {
        long chatId = 12345L; // ID чата пользователя.
        String tag = "RPG"; // Тег для поиска.

        // Создаем результат поиска (список игр)
        List<Map.Entry<String, JSONObject>> mockGames = new ArrayList<>();
        JSONObject game = new JSONObject(); // Мок данных игры.
        game.put("Name", "The Witcher 3");
        game.put("ID", "123");
        mockGames.add(Map.entry("123", game)); // Добавление игры в результат поиска.

        // Настройка мока: возвращает список игр для указанного тега.
        when(mockFindGame.findGamesByTag(tag, mockDatabase.readDatabase())).thenReturn(mockGames);

        // Вызов метода поиска.
        searchs.searchGameByTag(chatId, tag);

        // Проверка: убедиться, что метод отправки сообщения вызван.
        verify(mockHandler, times(1)).execute(argThat((SendMessage message) ->
                message.getText().equals("Select a game:") && // Проверка текста сообщения.
                        message.getReplyMarkup() != null // Убедиться, что есть кнопки выбора игры.
        ));
    }

    // Тест: поиск игр по тегу, когда игры не найдены
    @Test
    void testSearchGameByTag_noResults() {
        long chatId = 12345L; // ID чата пользователя.
        String tag = "UnknownTag"; // Неизвестный тег.

        // Настройка мока: возвращает пустой список для неизвестного тега.
        when(mockFindGame.findGamesByTag(tag, mockDatabase.readDatabase())).thenReturn(new ArrayList<>());

        // Вызов метода поиска.
        searchs.searchGameByTag(chatId, tag);

        // Проверка: убедиться, что метод отправки сообщения вызван.
        verify(mockHandler, times(1)).execute(argThat((SendMessage message) ->
                message.getText().equals("No games found with that tag.") // Проверка текста сообщения.
        ));
    }

    // Тест: поиск игр по имени, когда игры найдены
    @Test
    void testSearchGameByName_withResults() throws TelegramApiException {
        long chatId = 12345L; // ID чата пользователя.
        String gameName = "Cyberpunk"; // Имя для поиска.

        // Создаем результат поиска (список игр)
        List<Map.Entry<String, JSONObject>> mockGames = new ArrayList<>();
        JSONObject game = new JSONObject(); // Мок данных игры.
        game.put("Name", "Cyberpunk 2077");
        game.put("ID", "456");
        mockGames.add(Map.entry("456", game)); // Добавление игры в результат поиска.

        // Настройка мока: возвращает список игр для указанного имени.
        when(mockFindGame.findGamesByName(gameName, mockDatabase.readDatabase())).thenReturn(mockGames);

        // Вызов метода поиска.
        searchs.searchGameByName(chatId, gameName);

        // Проверка: убедиться, что метод отправки сообщения вызван.
        verify(mockHandler, times(1)).execute(argThat((SendMessage message) ->
                message.getText().equals("Select a game:") && // Проверка текста сообщения.
                        message.getReplyMarkup() != null // Убедиться, что есть кнопки выбора игры.
        ));
    }

    // Тест: поиск игр по имени, когда игры не найдены
    @Test
    void testSearchGameByName_noResults() {
        long chatId = 12345L; // ID чата пользователя.
        String gameName = "NonExistentGame"; // Неизвестное имя.

        // Настройка мока: возвращает пустой список для неизвестного имени.
        when(mockFindGame.findGamesByName(gameName, mockDatabase.readDatabase())).thenReturn(new ArrayList<>());

        // Вызов метода поиска.
        searchs.searchGameByName(chatId, gameName);

        // Проверка: убедиться, что метод отправки сообщения вызван.
        verify(mockHandler, times(1)).execute(argThat((SendMessage message) ->
                message.getText().equals("No games found with that name.") // Проверка текста сообщения.
        ));
    }

    // Тест: поиск игры по точному имени в "wishlist", если игра найдена
    @Test
    void testSearchGameByExactNameInWishlist_withMatch() {
        long userId = 12345L; // ID пользователя.
        String gameName = "The Witcher 3"; // Имя игры.

        // Создаем мок "wishlist" с одной игрой
        List<JSONObject> wishlist = new ArrayList<>();
        JSONObject game = new JSONObject();
        game.put("Name", "The Witcher 3");
        wishlist.add(game);

        // Настройка мока: возвращает "wishlist"
        when(mockWishlistFunctions.readWishlist(userId)).thenReturn(wishlist);

        // Вызов метода поиска
        JSONObject result = searchs.searchGameByExactNameInWishlist(gameName, userId);

        // Проверка: игра найдена и имя совпадает
        assertNotNull(result);
        assertEquals("The Witcher 3", result.optString("Name"));
    }

    // Тест: поиск игры по точному имени в "wishlist", если игра не найдена
    @Test
    void testSearchGameByExactNameInWishlist_noMatch() {
        long userId = 12345L; // ID пользователя.
        String gameName = "Cyberpunk 2077"; // Имя игры.

        // Создаем мок "wishlist" с другой игрой
        List<JSONObject> wishlist = new ArrayList<>();
        JSONObject game = new JSONObject();
        game.put("Name", "The Witcher 3");
        wishlist.add(game);

        // Настройка мока: возвращает "wishlist"
        when(mockWishlistFunctions.readWishlist(userId)).thenReturn(wishlist);

        // Вызов метода поиска
        JSONObject result = searchs.searchGameByExactNameInWishlist(gameName, userId);

        // Проверка: игра не найдена
        assertNull(result);
    }
}
