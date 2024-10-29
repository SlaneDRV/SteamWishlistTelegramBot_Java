package MainFunctions.HandlersFunctions;

import MainFunctions.DataManager;
import MainFunctions.Handlers;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Sorting {
    private Handlers handler = new Handlers();
    private Message message = new Message();

    public void handleSortCommand(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Sort Wishlist by:");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Sort by alphabet"));
        row1.add(new KeyboardButton("Sort by date"));
        row1.add(new KeyboardButton("Sort by reviews"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Back"));

        keyboard.add(row1);
        keyboard.add(row2);

        keyboardMarkup.setKeyboard(keyboard);
        message.setReplyMarkup(keyboardMarkup);

        try {
            handler.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sortWishlistByDate(long chatId) {
        List<JSONObject> wishlist = DataManager.readWishlist(chatId);
        Map<String, Object> database = DataManager.readDatabase();

        List<JSONObject> sortedWishlist = DataManager.sortWishlistByDate(wishlist, database);

        if (sortedWishlist.isEmpty()) {
            message.sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (JSONObject game : sortedWishlist) {
            String gameName = game.optString("Name");
            String price = game.optDouble("Price", 0.0) != 0.0 ? String.valueOf(game.optDouble("Price")) : "Free";

            InlineKeyboardButton gameButton = new InlineKeyboardButton();
            gameButton.setText(gameName + " - " + price);
            gameButton.setCallbackData("wishlist_" + gameName);

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(gameButton);
            rows.add(row);
        }

        inlineMarkup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Sorted Wishlist by Release Date:");
        message.setReplyMarkup(inlineMarkup);

        try {
            handler.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sortWishlistByReviews(long chatId) {
        List<JSONObject> wishlist = DataManager.readWishlist(chatId);
        Map<String, Object> database = DataManager.readDatabase();

        List<JSONObject> sortedWishlist = DataManager.sortWishlistByReviews(wishlist, database);

        if (sortedWishlist.isEmpty()) {
            message.sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (JSONObject game : sortedWishlist) {
            String gameName = game.optString("Name");
            String price = game.optDouble("Price", 0.0) != 0.0 ? String.valueOf(game.optDouble("Price")) : "Free";

            InlineKeyboardButton gameButton = new InlineKeyboardButton();
            gameButton.setText(gameName + " - " + price);
            gameButton.setCallbackData("wishlist_" + gameName);

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(gameButton);
            rows.add(row);
        }

        inlineMarkup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Sorted Wishlist by Total Reviews:");
        message.setReplyMarkup(inlineMarkup);

        try {
            handler.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    public void sortWishlistByAlphabet(long chatId) {
        List<JSONObject> wishlist = DataManager.readWishlist(chatId);

        List<JSONObject> sortedWishlist = DataManager.sortWishlistByAlphabet(wishlist);

        if (sortedWishlist.isEmpty()) {
            message.sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (JSONObject game : sortedWishlist) {
            String gameName = game.optString("Name");
            String price = game.optDouble("Price", 0.0) != 0.0 ? String.valueOf(game.optDouble("Price")) : "Free";

            InlineKeyboardButton gameButton = new InlineKeyboardButton();
            gameButton.setText(gameName + " - " + price);
            gameButton.setCallbackData("wishlist_" + gameName);

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(gameButton);
            rows.add(row);
        }

        inlineMarkup.setKeyboard(rows);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Sorted Wishlist by Alphabet:");
        message.setReplyMarkup(inlineMarkup);

        try {
            handler.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
