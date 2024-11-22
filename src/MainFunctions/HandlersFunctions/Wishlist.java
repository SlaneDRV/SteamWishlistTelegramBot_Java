package MainFunctions.HandlersFunctions;

import MainFunctions.DataManageFunctions.Database;
import MainFunctions.DataManageFunctions.FindExactGame;
import MainFunctions.DataManageFunctions.WishlistFunctions;
import MainFunctions.Handlers;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Wishlist {
    private Handlers handler = new Handlers();
    private Message message = new Message();

    private final WishlistFunctions Wishlist = new WishlistFunctions();

    public void showWishlist(long chatId) {

        List<JSONObject> wishlist = Wishlist.readWishlist(chatId);

        if (wishlist.isEmpty()) {
            message.sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (JSONObject game : wishlist) {
            String gameName = game.optString("Name");
            String priceStr = game.optString("Price", "Free");

            String price;
            if (priceStr.equalsIgnoreCase("N/A")) {
                price = "N/A";
            } else {
                try {
                    if (!priceStr.equals("Free")) {
                        price = priceStr.replace("$", "").trim();
                        double priceValue = Double.parseDouble(price);
                        price = "$" + price;
                    } else if (priceStr.equals("Coming Soon")) {
                        price = "Coming Soon";
                    } else {
                        price = "Free";
                    }
                } catch (NumberFormatException e) {
                    price = "N/A";
                }
            }

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(gameName + " - " + price);
            button.setCallbackData("wishlist_" + gameName);

            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(button);
            rowsInline.add(rowInline);
        }



        inlineKeyboardMarkup.setKeyboard(rowsInline);

        SendMessage Message = new SendMessage();
        Message.setChatId(String.valueOf(chatId));
        Message.setText("Your Wishlist:");
        Message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            handler.execute(Message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        message.sendMessage(chatId, "You have " + wishlist.size() + " games in your Wishlist.");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Calculate Total Price"));
        row1.add(new KeyboardButton("Count Tags"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Download Wishlist"));
        row2.add(new KeyboardButton("Import Wishlist"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("Remove Game from Wishlist"));
        row3.add(new KeyboardButton("Sort"));

        KeyboardRow row4 = new KeyboardRow();
        row4.add(new KeyboardButton("Back"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);
        keyboard.add(row4);

        keyboardMarkup.setKeyboard(keyboard);

        message.sendMessageWithKeyboard(chatId, "Choose an option:", keyboardMarkup);
    }

    public void addToWishlistCallback(CallbackQuery call) {
        String gameName = call.getData().split("_", 2)[1];
        long chatId = call.getMessage().getChatId();

        Map<String, Object> database = Database.readDatabase();
        List<JSONObject> games = FindExactGame.findGameByExactName(gameName, database);

        if (!games.isEmpty()) {
            JSONObject gameData = games.get(0);

            JSONObject gameDataEnd = new JSONObject();
            gameDataEnd.put("ID", gameData.optString("ID"));
            gameDataEnd.put("Name", gameData.optString("Name"));
            gameDataEnd.put("Price", gameData.optString("Price"));

            Wishlist.addGameToWishlist(chatId, gameDataEnd);

            String currentText = call.getMessage().getText();
            String newText = gameData.optString("Name") + " has been added to your wishlist.";

            message.editMessageText(newText, chatId, call.getMessage().getMessageId(), currentText);

        } else {
            String currentText = call.getMessage().getText();
            String newText = "Game not found in the database.";

            message.editMessageText(newText, chatId, call.getMessage().getMessageId(), currentText);

        }
    }
}
