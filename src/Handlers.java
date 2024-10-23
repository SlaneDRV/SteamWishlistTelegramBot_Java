
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

public class Handlers extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "YourBotUsername"; // Имя бота в Telegram
    }

    @Override
    public String getBotToken() {
        return Config.TOKEN; // Токен бота
    }
    private boolean waitingForTag = false;
    private boolean waitingForName = false;

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendMainMenu(chatId);
            } else if (messageText.equals("Back")) {
                sendMainMenu(chatId);
            } else if (messageText.equals("Find a New Game")) {
                sendSearchOptions(chatId);
            } else if (messageText.equals("Find by name")) {
                waitingForName = true; // Флаг для ожидания имени игры
                sendMessage(chatId, "Please enter the name of the game you're looking for.");
            } else if (messageText.equals("Find by tag")) {
                waitingForTag = true; // Флаг для ожидания тега
                sendMessage(chatId, "Please enter the tag of the game you're looking for.");
            } else if (waitingForTag) {
                // Пользователь ввел тег
                searchGameByTag(chatId, messageText);
                waitingForTag = false; // Сбрасываем флаг после обработки
            } else if (waitingForName) {
                // Пользователь ввел имя
                searchGameByName(chatId, messageText);
                waitingForName = false; // Сбрасываем флаг после обработки
            } else if (messageText.equals("Wishlist")) {
                showWishlist(chatId);
            } else if (messageText.equals("View Wishlist")) {
                showWishlist(chatId);
            } else if (messageText.equals("Remove Game from Wishlist")) {
                promptForGameRemoval(chatId); // Вызов функции удаления игры
            }

        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.startsWith("list_")) {
                showGameDetailsList(chatId, callbackData);
            } else if (callbackData.startsWith("add_")) {
                // Извлекаем имя игры из callbackData
                String gameName = callbackData.split("_", 2)[1];
                // Вызываем метод добавления в вишлист
                addToWishlist(chatId, gameName);
            } else if (callbackData.startsWith("wishlist_")) {
                // Извлекаем имя игры из callbackData
                showGameDetails(chatId, callbackData);
            } else if (callbackData.startsWith("remove_")) {
                // Обработка удаления игры из вишлиста
                removeGameFromWishlistCallback(update.getCallbackQuery());
            }
            // Обработка других callback данных здесь
        }
    }

    private void promptForGameRemoval(long chatId) {
        List<JSONObject> wishlist = DataManager.readWishlist(chatId);
        if (wishlist.isEmpty()) {
            sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (JSONObject game : wishlist) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(game.optString("Name"));
            button.setCallbackData("remove_" + game.optString("Name"));
            rowsInline.add(List.of(button));
        }

        markup.setKeyboard(rowsInline);
        sendMessageWithInlineKeyboard(chatId, "Choose a game to remove from your wishlist:", markup);
    }

    private void removeGameFromWishlistCallback(CallbackQuery call) {
        String gameName = call.getData().split("_", 2)[1];
        DataManager.removeGameFromWishlist(call.getMessage().getChatId(), gameName);
        sendMessage(call.getMessage().getChatId(), gameName + " removed from your wishlist.");
        editMessageText("Game removed from your wishlist.", call.getMessage().getChatId(), call.getMessage().getMessageId());
    }

    private void editMessageText(String text, long chatId, int messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setMessageId(messageId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMainMenu(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Find a New Game"));
        row1.add(new KeyboardButton("Wishlist"));

        keyboard.add(row1);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        sendMessageWithKeyboard(chatId, "Welcome! Choose an option:", keyboardMarkup);
    }

    private void sendSearchOptions(long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Find by name"));
        row1.add(new KeyboardButton("Find by tag"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Back"));

        keyboard.add(row1);
        keyboard.add(row2);
        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);

        sendMessageWithKeyboard(chatId, "Choose a search option or go back:", keyboardMarkup);
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void searchGameByTag(long chatId, String tag) {
        // Отправляем сообщение о поиске
        sendMessage(chatId, "Searching for games by tag '" + tag + "'...");
        System.out.println("Searching for games by tag START");

        // Ищем игры по тегу
        List<Map.Entry<String, JSONObject>> games = DataManager.findGamesByTag(tag, DataManager.readDatabase());

        System.out.println("Searching for games by tag FINISH");

        if (games.isEmpty()) {
            sendMessage(chatId, "No games found with that tag.");
            return;
        }

        // Создаем Inline клавиатуру для выбора игр
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Ограничиваем список до первых 20 игр
        for (int i = 0; i < Math.min(games.size(), 20); i++) {
            JSONObject game = games.get(i).getValue(); // Получаем JSONObject из Entry
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

        // Отправляем сообщение с клавиатурой
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Select a game:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void searchGameByName(long chatId, String gameName) {
        // Отправляем сообщение о поиске
        sendMessage(chatId, "Searching for games with name '" + gameName + "'...");
        System.out.println("Searching for games by name START");

        // Ищем игры по имени
        List<Map.Entry<String, JSONObject>> games = DataManager.findGamesByName(gameName, DataManager.readDatabase());

        System.out.println("Searching for games by name FINISH");

        if (games.isEmpty()) {
            sendMessage(chatId, "No games found with that name.");
            return;
        }

        // Создаем Inline клавиатуру для выбора игр
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Ограничиваем список до первых 10 игр
        for (int i = 0; i < Math.min(games.size(), 10); i++) {
            JSONObject game = games.get(i).getValue(); // Получаем JSONObject из Entry
            String gameNameResult = game.optString("Name");
            String gameId = game.optString("ID");

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(gameNameResult);
            button.setCallbackData("list_" + gameId);

            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        // Отправляем сообщение с клавиатурой
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Select a game:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void addToWishlist(long chatId, String gameName) {
        // Чтение базы данных
        Map<String, Object> database = DataManager.readDatabase();


        List<JSONObject> games = DataManager.findGameByExactName(gameName, database);

        if (!games.isEmpty()) {
            JSONObject gameData = games.get(0); 

            JSONObject gameDataEnd = new JSONObject();
            gameDataEnd.put("ID", gameData.optString("ID"));
            gameDataEnd.put("Name", gameData.optString("Name"));
            gameDataEnd.put("Price", gameData.optString("Price"));

            DataManager.addGameToWishlist(chatId, gameDataEnd);

            sendMessage(chatId, gameData.optString("Name") + " has been added to your wishlist.");
        } else {
            sendMessage(chatId, "Game not found in the database.");
        }
    }

    private void showWishlist(long chatId) {
        // Чтение вишлиста пользователя
        List<JSONObject> wishlist = DataManager.readWishlist(chatId);

        if (wishlist.isEmpty()) {
            sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

        // Создаем Inline клавиатуру для отображения игр из вишлиста
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (JSONObject game : wishlist) {
            String gameName = game.optString("Name");
            String price = game.optDouble("Price", 0.0) != 0.0 ? game.optString("Price") : "Free";

            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(gameName + " - " + price);
            button.setCallbackData("wishlist_" + gameName);

            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(button);
            rowsInline.add(rowInline);
        }

        inlineKeyboardMarkup.setKeyboard(rowsInline);

        // Отправляем сообщение с вишлистом
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Your Wishlist:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        // Отправляем количество игр в вишлисте
        sendMessage(chatId, "You have " + wishlist.size() + " games in your Wishlist.");

        // Создаем основное меню с опциями для управления вишлистом
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

        sendMessageWithKeyboard(chatId, "Choose an option:", keyboardMarkup);
    }
    /////////////////////

    private void showGameDetailsList(long chatId, String callbackData) {
        System.out.println("Processing list callback...");
        String gameId = callbackData.split("_", 2)[1];
        System.out.println("Game ID extracted: " + gameId);

        Map<String, Object> database = DataManager.readDatabase();
        System.out.println("Available game IDs: " + database.keySet());

        // Извлекаем игру как Map<String, Object>
        Map<String, Object> gameDataMap = (Map<String, Object>) database.get(gameId);

        if (gameDataMap != null) {
            // Преобразуем Map в JSONObject
            JSONObject game = new JSONObject(gameDataMap);

            // Логика обработки, если игра найдена
            String imageUrl = game.optString("ImageURL", null);
            int totalReviews = game.optInt("PositiveReviews", 0) + game.optInt("NegativeReviews", 0);
            double positivePercentage = totalReviews > 0
                    ? (game.optInt("PositiveReviews", 0) / (double) totalReviews) * 100 : 0;

            String developer = game.optString("Developer");
            String publisher = game.optString("Publisher");
            String tags = game.optJSONArray("TopTags") != null
                    ? String.join(", ", game.optJSONArray("TopTags").toList().stream()
                            .map(Object::toString).collect(Collectors.toList()))
                    : "No tags found";

            String price = game.optString("Price", "Free").equals("Free") ? "Free" : game.optString("Price");
            String dayPeak = game.optString("DayPeak", "N/A");
            String platforms = game.optString("Platforms", "N/A");
            String href = "https://store.steampowered.com/app/" + game.optString("ID");

            // Экранирование HTML символов
            String name = escapeHtml(game.optString("Name"));
            String shortDescription = escapeHtml(game.optString("ShortDesc"));
            developer = escapeHtml(developer);
            publisher = escapeHtml(publisher);
            String releaseDate = escapeHtml(game.optString("ReleaseDate"));

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
                sendMessage(chatId, caption);
            }

            // Создаем Inline клавиатуру для управления игрой
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
            InlineKeyboardButton button = new InlineKeyboardButton();

            if (DataManager.checkWishlist(chatId, game.optString("Name"))) {
                button.setText("Remove " + name + " from Wishlist");
                button.setCallbackData("remove_" + name);
                rowsInline.add(List.of(button));
            } else {
                button.setText("Add " + name + " to Wishlist");
                button.setCallbackData("add_" + name);
                rowsInline.add(List.of(button));
            }

            InlineKeyboardButton languagesButton = new InlineKeyboardButton();
            languagesButton.setText("Show available languages");
            languagesButton.setCallbackData("languages_" + game.optString("ID"));
            rowsInline.add(List.of(languagesButton));

            // Добавить кнопку обновления информации, если это ваш ID
            if (chatId == Config.TG_ID) { // Замените TgID на ваш действительный ID
                InlineKeyboardButton updateButton = new InlineKeyboardButton();
                updateButton.setText("Update game info");
                updateButton.setCallbackData("update_" + game.optString("ID"));
                rowsInline.add(List.of(updateButton));
            }

            inlineKeyboardMarkup.setKeyboard(rowsInline);

            sendMessageWithInlineKeyboard(chatId, "Would you like to manage this game in your Wishlist?", inlineKeyboardMarkup);
        } else {
            sendMessage(chatId, "No details found for the game ID: " + gameId);
        }
    }

// Экранирование HTML символов
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
            execute(photo);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageWithInlineKeyboard(long chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public static JSONObject findGameByExactNameInWishlist(String gameName, long userId) {
        // Читаем список желаемых игр из файла или базы данных
        List<JSONObject> wishlist = DataManager.readWishlist(userId);
        System.out.println("Wishlist: " + wishlist);
        // Ищем игру с точным именем
        for (JSONObject game : wishlist) {
            if (game.optString("Name").equals(gameName)) {
                return game;  // Возвращаем найденную игру
            }
        }
        return null;  // Если игра не найдена, возвращаем null
    }

    private void showGameDetails(long chatId, String callbackData) {
        System.out.println("Processing game details callback...");
        String gameName = callbackData.split("_", 2)[1].trim(); // Обрезаем пробелы
        System.out.println("Game name extracted: " + gameName);

        JSONObject gameData = findGameByExactNameInWishlist(gameName, chatId);
        System.out.println("GameData: " + gameData);

        if (gameData != null) {  // Проверяем на null
            Map<String, Object> database = DataManager.readDatabase();

            // Найдем информацию о игре в базе данных по полному названию
            List<JSONObject> gameInfoList = DataManager.findGameByExactName(gameName, database);
            JSONObject gameInfo = gameInfoList.isEmpty() ? null : gameInfoList.get(0);  // Получаем первый элемент или null
            System.out.println("GameInfo: " + gameInfo);

            if (gameInfo != null) {
                // Получаем информацию о игре
                String imageUrl = gameInfo.optString("ImageURL", null);
                int totalReviews = gameInfo.optInt("PositiveReviews", 0) + gameInfo.optInt("NegativeReviews", 0);
                double positivePercentage = totalReviews > 0
                        ? (gameInfo.optInt("PositiveReviews", 0) / (double) totalReviews) * 100 : 0;

                String developer = escapeHtml(gameInfo.optString("Developer"));
                String publisher = escapeHtml(gameInfo.optString("Publisher"));
                String tags = gameInfo.optJSONArray("TopTags") != null
                        ? String.join(", ", gameInfo.optJSONArray("TopTags").toList().stream()
                                .map(Object::toString).collect(Collectors.toList())) : "No tags found";

                String price = gameInfo.optString("Price", "Free").equals("Free") ? "Free" : gameInfo.optString("Price");
                String dayPeak = gameInfo.optString("DayPeak", "N/A");
                String platforms = gameInfo.optString("Platforms", "N/A");
                String href = "https://store.steampowered.com/app/" + gameInfo.optString("ID");

                // Экранирование HTML символов
                String name = escapeHtml(gameInfo.optString("Name"));
                String shortDescription = escapeHtml(gameInfo.optString("ShortDesc"));
                String releaseDate = escapeHtml(gameInfo.optString("ReleaseDate"));

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
                    sendMessage(chatId, caption);
                }

                // Создаем Inline клавиатуру для управления игрой
                InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

                InlineKeyboardButton manageButton = new InlineKeyboardButton();
                manageButton.setText("Remove " + name + " from Wishlist");
                manageButton.setCallbackData("remove_" + name);
                rowsInline.add(List.of(manageButton));

                InlineKeyboardButton languagesButton = new InlineKeyboardButton();
                languagesButton.setText("Show available languages");
                languagesButton.setCallbackData("languages_" + gameInfo.optString("ID"));
                rowsInline.add(List.of(languagesButton));

                // Добавить кнопку обновления информации, если это ваш ID
                if (chatId == Config.TG_ID) { // Замените Config.TG_ID на ваш действительный ID
                    InlineKeyboardButton updateButton = new InlineKeyboardButton();
                    updateButton.setText("Update game info");
                    updateButton.setCallbackData("update_" + gameInfo.optString("ID"));
                    rowsInline.add(List.of(updateButton));
                }

                inlineKeyboardMarkup.setKeyboard(rowsInline);

                sendMessageWithInlineKeyboard(chatId, "Would you like to manage this game in your Wishlist?", inlineKeyboardMarkup);
            } else {
                sendMessage(chatId, "Information about '" + gameName + "' not found in the database.");
            }
        } else {
            sendMessage(chatId, "Game '" + gameName + "' not found in your Wishlist.");
        }
    }

}
