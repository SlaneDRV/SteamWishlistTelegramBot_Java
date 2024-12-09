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

    // ���� ������������
    @Mock
    private Handlers mockHandler; // ��� ������� ��� ��������� ��������� Telegram.

    @Mock
    private WishlistFunctions mockWishlistFunctions; // ��� ��� ������ � "wishlist" (������� ���������).

    @Mock
    private Database mockDatabase; // ��� ���� ������.

    @Mock
    private FindGame mockFindGame; // ��� ��� ������ ���.

    private Searchs searchs; // ����������� ������.

    // �����, ������� ����������� ����� ������ ������
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this); // ������������� �����.
        searchs = new Searchs(); // �������� ���������� ������������ ������.
        searchs.handler = mockHandler; // ����������� ���� �����������.
    }

    // ����: ����� ��� �� ����, ����� ���� �������
    @Test
    void testSearchGameByTag_withResults() throws TelegramApiException {
        long chatId = 12345L; // ID ���� ������������.
        String tag = "RPG"; // ��� ��� ������.

        // ������� ��������� ������ (������ ���)
        List<Map.Entry<String, JSONObject>> mockGames = new ArrayList<>();
        JSONObject game = new JSONObject(); // ��� ������ ����.
        game.put("Name", "The Witcher 3");
        game.put("ID", "123");
        mockGames.add(Map.entry("123", game)); // ���������� ���� � ��������� ������.

        // ��������� ����: ���������� ������ ��� ��� ���������� ����.
        when(mockFindGame.findGamesByTag(tag, mockDatabase.readDatabase())).thenReturn(mockGames);

        // ����� ������ ������.
        searchs.searchGameByTag(chatId, tag);

        // ��������: ���������, ��� ����� �������� ��������� ������.
        verify(mockHandler, times(1)).execute(argThat((SendMessage message) ->
                message.getText().equals("Select a game:") && // �������� ������ ���������.
                        message.getReplyMarkup() != null // ���������, ��� ���� ������ ������ ����.
        ));
    }

    // ����: ����� ��� �� ����, ����� ���� �� �������
    @Test
    void testSearchGameByTag_noResults() {
        long chatId = 12345L; // ID ���� ������������.
        String tag = "UnknownTag"; // ����������� ���.

        // ��������� ����: ���������� ������ ������ ��� ������������ ����.
        when(mockFindGame.findGamesByTag(tag, mockDatabase.readDatabase())).thenReturn(new ArrayList<>());

        // ����� ������ ������.
        searchs.searchGameByTag(chatId, tag);

        // ��������: ���������, ��� ����� �������� ��������� ������.
        verify(mockHandler, times(1)).execute(argThat((SendMessage message) ->
                message.getText().equals("No games found with that tag.") // �������� ������ ���������.
        ));
    }

    // ����: ����� ��� �� �����, ����� ���� �������
    @Test
    void testSearchGameByName_withResults() throws TelegramApiException {
        long chatId = 12345L; // ID ���� ������������.
        String gameName = "Cyberpunk"; // ��� ��� ������.

        // ������� ��������� ������ (������ ���)
        List<Map.Entry<String, JSONObject>> mockGames = new ArrayList<>();
        JSONObject game = new JSONObject(); // ��� ������ ����.
        game.put("Name", "Cyberpunk 2077");
        game.put("ID", "456");
        mockGames.add(Map.entry("456", game)); // ���������� ���� � ��������� ������.

        // ��������� ����: ���������� ������ ��� ��� ���������� �����.
        when(mockFindGame.findGamesByName(gameName, mockDatabase.readDatabase())).thenReturn(mockGames);

        // ����� ������ ������.
        searchs.searchGameByName(chatId, gameName);

        // ��������: ���������, ��� ����� �������� ��������� ������.
        verify(mockHandler, times(1)).execute(argThat((SendMessage message) ->
                message.getText().equals("Select a game:") && // �������� ������ ���������.
                        message.getReplyMarkup() != null // ���������, ��� ���� ������ ������ ����.
        ));
    }

    // ����: ����� ��� �� �����, ����� ���� �� �������
    @Test
    void testSearchGameByName_noResults() {
        long chatId = 12345L; // ID ���� ������������.
        String gameName = "NonExistentGame"; // ����������� ���.

        // ��������� ����: ���������� ������ ������ ��� ������������ �����.
        when(mockFindGame.findGamesByName(gameName, mockDatabase.readDatabase())).thenReturn(new ArrayList<>());

        // ����� ������ ������.
        searchs.searchGameByName(chatId, gameName);

        // ��������: ���������, ��� ����� �������� ��������� ������.
        verify(mockHandler, times(1)).execute(argThat((SendMessage message) ->
                message.getText().equals("No games found with that name.") // �������� ������ ���������.
        ));
    }

    // ����: ����� ���� �� ������� ����� � "wishlist", ���� ���� �������
    @Test
    void testSearchGameByExactNameInWishlist_withMatch() {
        long userId = 12345L; // ID ������������.
        String gameName = "The Witcher 3"; // ��� ����.

        // ������� ��� "wishlist" � ����� �����
        List<JSONObject> wishlist = new ArrayList<>();
        JSONObject game = new JSONObject();
        game.put("Name", "The Witcher 3");
        wishlist.add(game);

        // ��������� ����: ���������� "wishlist"
        when(mockWishlistFunctions.readWishlist(userId)).thenReturn(wishlist);

        // ����� ������ ������
        JSONObject result = searchs.searchGameByExactNameInWishlist(gameName, userId);

        // ��������: ���� ������� � ��� ���������
        assertNotNull(result);
        assertEquals("The Witcher 3", result.optString("Name"));
    }

    // ����: ����� ���� �� ������� ����� � "wishlist", ���� ���� �� �������
    @Test
    void testSearchGameByExactNameInWishlist_noMatch() {
        long userId = 12345L; // ID ������������.
        String gameName = "Cyberpunk 2077"; // ��� ����.

        // ������� ��� "wishlist" � ������ �����
        List<JSONObject> wishlist = new ArrayList<>();
        JSONObject game = new JSONObject();
        game.put("Name", "The Witcher 3");
        wishlist.add(game);

        // ��������� ����: ���������� "wishlist"
        when(mockWishlistFunctions.readWishlist(userId)).thenReturn(wishlist);

        // ����� ������ ������
        JSONObject result = searchs.searchGameByExactNameInWishlist(gameName, userId);

        // ��������: ���� �� �������
        assertNull(result);
    }
}
