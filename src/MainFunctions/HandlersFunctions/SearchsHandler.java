package MainFunctions.HandlersFunctions;

import MainFunctions.DataManageFunctions.DatabaseFunctions;
import MainFunctions.DataManageFunctions.FindGameFunctions;
import MainFunctions.DataManageFunctions.WishlistFunctions;
import MainFunctions.Handlers;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchsHandler {
    private final Handlers handler = new Handlers();
    private final MessageHandler message = new MessageHandler();
    private static final WishlistFunctions Wishlist = new WishlistFunctions();

    private InlineKeyboardMarkup createInlineKeyboard(List<Map.Entry<String, JSONObject>> games, int maxButtons) {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (int i = 0; i < Math.min(games.size(), maxButtons); i++) {
            JSONObject game = games.get(i).getValue();
            String gameName = game.optString("Name");
            String gameId = game.optString("ID");

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(gameName);
            button.setCallbackData("list_" + gameId);

            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);
        return inlineKeyboardMarkup;
    }



    public void searchGameByTag(long chatId, String tag) {
        message.sendMessage(chatId, "Searching for games by tag '" + tag + "'...");
        System.out.println("Searching for games by tag START");

        List<Map.Entry<String, JSONObject>> games = FindGameFunctions.findGamesByTag(tag, DatabaseFunctions.readDatabase());

        System.out.println("Searching for games by tag FINISH");

        if (games.isEmpty()) {
            message.sendMessage(chatId, "No games found with that tag.");
            return;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = createInlineKeyboard(games, 20);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Select a game:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            handler.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    public void searchGameByName(long chatId, String gameName) {
        message.sendMessage(chatId, "Searching for games with name '" + gameName + "'...");
        System.out.println("Searching for games by name START");

        List<Map.Entry<String, JSONObject>> games = FindGameFunctions.findGamesByName(gameName, DatabaseFunctions.readDatabase());

        System.out.println("Searching for games by name FINISH");

        if (games.isEmpty()) {
            message.sendMessage(chatId, "No games found with that name.");
            return;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = createInlineKeyboard(games, 10);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Select a game:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            handler.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }



    public static JSONObject searchGameByExactNameInWishlist(String gameName, long userId) {

        List<JSONObject> wishlist = Wishlist.readWishlist(userId);
        System.out.println("Wishlist: " + wishlist);

        for (JSONObject game : wishlist) {
            if (game.optString("Name").equals(gameName)) {
                return game;
            }
        }
        return null;
    }
}
