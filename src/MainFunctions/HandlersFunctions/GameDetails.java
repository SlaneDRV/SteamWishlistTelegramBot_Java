package MainFunctions.HandlersFunctions;

import MainFunctions.Config;
import MainFunctions.DataManageFunctions.Database;
import MainFunctions.DataManageFunctions.FindExactGame;
import MainFunctions.Handlers;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GameDetails {

    private Handlers handler = new Handlers();
    private Searchs search = new Searchs();
    private Message message = new Message();
    public void showGameDetails(long chatId, String callbackData, boolean wishlist) {
        System.out.println("Processing game details callback...");

        String identifier = callbackData.split("_", 2)[1].trim();
        System.out.println("Game identifier extracted: " + identifier);
        Map<String, Object> database = Database.readDatabase();

        JSONObject gameData;
        if (wishlist) {

            JSONObject gameDataWishlist = search.searchGameByExactNameInWishlist(identifier, chatId);
            if (gameDataWishlist != null) {
                List<JSONObject> gameInfoList = FindExactGame.findGameByExactName(identifier, database);
                gameData = gameInfoList.isEmpty() ? null : gameInfoList.get(0);
            }
            else {
                gameData = null;
            }

        } else {

            gameData = new JSONObject((Map<String, Object>) database.get(identifier));
        }

        if (gameData != null) {
            String imageUrl = gameData.optString("ImageURL", null);
            int totalReviews = gameData.optInt("PositiveReviews", 0) + gameData.optInt("NegativeReviews", 0);
            double positivePercentage = totalReviews > 0
                    ? (gameData.optInt("PositiveReviews", 0) / (double) totalReviews) * 100 : 0;

            String developer = escapeHtml(gameData.optString("Developer"));
            String publisher = escapeHtml(gameData.optString("Publisher"));
            String tags = gameData.optJSONArray("TopTags") != null
                    ? String.join(", ", gameData.optJSONArray("TopTags").toList().stream()
                    .map(Object::toString).collect(Collectors.toList())) : "No tags found";

            String price = gameData.optString("Price", "Free").equals("Free") ? "Free" : gameData.optString("Price");
            String dayPeak = gameData.optString("DayPeak", "N/A");
            String platforms = gameData.optString("Platforms", "N/A");
            String href = "https://store.steampowered.com/app/" + gameData.optString("ID");

            String name = escapeHtml(gameData.optString("Name"));
            String shortDescription = escapeHtml(gameData.optString("ShortDesc"));
            String releaseDate = escapeHtml(gameData.optString("ReleaseDate"));

            String caption = String.format(
                    "<b>%s</b>\n\n"
                            + "<i>%s</i>\n\n"
                            + "<b>Total reviews:</b> %d (%.2f%% positive)\n"
                            + "<b>Release date:</b> %s\n"
                            + "<b>Developer:</b> %s\n"
                            + "<b>Publisher:</b> %s\n\n"
                            + "<b>Tags:</b> %s\n"
                            + "<b>Price:</b> %s\n"
                            + "<b>Max Online Yesterday:</b> %s\n"
                            + "<b>Platforms:</b> %s\n"
                            + "%s",
                    name, shortDescription, totalReviews, positivePercentage, releaseDate, developer, publisher,
                    tags, price, dayPeak, platforms, href
            );

            if (imageUrl != null) {
                sendPhoto(chatId, imageUrl, caption);
            } else {
                message.sendMessage(chatId, caption);
            }

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

            InlineKeyboardButton wishlistButton = new InlineKeyboardButton();
            if (wishlist) {
                wishlistButton.setText("Remove " + name + " from Wishlist");
                wishlistButton.setCallbackData("remove_" + name);
            } else {
                wishlistButton.setText("Add " + name + " to Wishlist");
                wishlistButton.setCallbackData("add_" + name);
            }
            rowsInline.add(List.of(wishlistButton));

            InlineKeyboardButton languagesButton = new InlineKeyboardButton();
            languagesButton.setText("Show available languages");
            languagesButton.setCallbackData("languages_" + gameData.optString("ID"));
            rowsInline.add(List.of(languagesButton));

            if (chatId == Config.TG_ID) {
                InlineKeyboardButton updateButton = new InlineKeyboardButton();
                updateButton.setText("Update game info");
                updateButton.setCallbackData("update_" + gameData.optString("ID"));
                rowsInline.add(List.of(updateButton));
            }

            inlineKeyboardMarkup.setKeyboard(rowsInline);

            message.sendMessageWithInlineKeyboard(chatId, "Would you like to manage this game in your Wishlist?", inlineKeyboardMarkup);
        } else {
            String errorMessage = wishlist
                    ? "Game '" + identifier + "' not found in your Wishlist."
                    : "Information about game ID '" + identifier + "' not found in the database.";
            message.sendMessage(chatId, errorMessage);
        }
    }

    private String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void sendPhoto(long chatId, String imageUrl, String caption) {
        SendPhoto photo = new SendPhoto();
        photo.setChatId(String.valueOf(chatId));
        photo.setPhoto(new InputFile(imageUrl));
        photo.setCaption(caption);
        photo.setParseMode("HTML");
        try {
            handler.execute(photo);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
