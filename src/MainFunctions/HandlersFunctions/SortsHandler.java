package MainFunctions.HandlersFunctions;

import MainFunctions.DataManageFunctions.DatabaseFunctions;
import MainFunctions.DataManageFunctions.SortingFunctions;
import MainFunctions.DataManageFunctions.WishlistFunctions;
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

public class SortsHandler {
    private Handlers handler = new Handlers();
    private MessageHandler message = new MessageHandler();
    private final WishlistFunctions Wishlist = new WishlistFunctions();

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

    private InlineKeyboardMarkup createGameKeyboard(List<JSONObject> sortedWishlist) {
        InlineKeyboardMarkup inlineMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        for (JSONObject game : sortedWishlist) {
            String gameName = game.optString("Name");
            String priceStr = game.optString("Price");

            String price;

            if (priceStr == null || priceStr.isEmpty() || priceStr.equalsIgnoreCase("N/A")) {
                price = "Free";
            } else {
                try {

                    double priceValue = Double.parseDouble(priceStr.replace("$", "").trim());
                    if (priceValue == 0.0) {
                        price = "Free";
                    } else {
                        price = "$" + priceStr;
                    }
                } catch (NumberFormatException e) {
                    price = "Free";
                }
            }

            InlineKeyboardButton gameButton = new InlineKeyboardButton();
            gameButton.setText(gameName + " - " + price);
            gameButton.setCallbackData("wishlist_" + gameName);

            List<InlineKeyboardButton> row = new ArrayList<>();
            row.add(gameButton);
            rows.add(row);
        }

        inlineMarkup.setKeyboard(rows);
        return inlineMarkup;
    }


    public void sortWishlistByDate(long chatId) {
        List<JSONObject> wishlist = Wishlist.readWishlist(chatId);
        Map<String, Object> database = DatabaseFunctions.readDatabase();

        List<JSONObject> sortedWishlist = SortingFunctions.sortWishlistByDate(wishlist, database);

        if (sortedWishlist.isEmpty()) {
            message.sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

        InlineKeyboardMarkup inlineMarkup = createGameKeyboard(sortedWishlist);

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
        List<JSONObject> wishlist = Wishlist.readWishlist(chatId);
        Map<String, Object> database = DatabaseFunctions.readDatabase();

        List<JSONObject> sortedWishlist = SortingFunctions.sortWishlistByReviews(wishlist, database);

        if (sortedWishlist.isEmpty()) {
            message.sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

        InlineKeyboardMarkup inlineMarkup = createGameKeyboard(sortedWishlist);

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
        List<JSONObject> wishlist = Wishlist.readWishlist(chatId);

        List<JSONObject> sortedWishlist = SortingFunctions.sortWishlistByAlphabet(wishlist);

        if (sortedWishlist.isEmpty()) {
            message.sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

        InlineKeyboardMarkup inlineMarkup = createGameKeyboard(sortedWishlist);

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
