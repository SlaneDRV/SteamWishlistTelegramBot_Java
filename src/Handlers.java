import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;


public class Handlers extends TelegramLongPollingBot {

    @Override
    public String getBotUsername() {
        return "YourBotUsername";
    }

    @Override
    public String getBotToken() {
        return Config.TOKEN;
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
                waitingForName = true;
                sendMessage(chatId, "Please enter the name of the game you're looking for.");
            } else if (messageText.equals("Find by tag")) {
                waitingForTag = true;
                sendMessage(chatId, "Please enter the tag of the game you're looking for.");
            } else if (waitingForTag) {
                searchGameByTag(chatId, messageText);
                waitingForTag = false;
            } else if (waitingForName) {
                searchGameByName(chatId, messageText);
                waitingForName = false;
            } else if (messageText.equals("Wishlist")) {
                showWishlist(chatId);
            } else if (messageText.equals("View Wishlist")) {
                showWishlist(chatId);
            } else if (messageText.equals("Remove Game from Wishlist")) {
                promptForGameRemoval(chatId);
            } else if (messageText.equals("Download Wishlist")) {
                chooseDownloadFormat(chatId);
            } else if (messageText.startsWith("Download as")) {
                String formatChoice = messageText.split(" ")[2].toLowerCase();
                downloadWishlist(chatId, formatChoice);
            } else if (messageText.equals("Import Wishlist")) {
                importWishlist(chatId);
            } else if(messageText.equals("Sort")){
                handleSortCommand(chatId);
            } else if(messageText.equals("Sort by date")){
                sortWishlistByDate(chatId);
            }
            else if(messageText.equals("Sort by reviews")){
                sortWishlistByReviews(chatId);
            }
            else if(messageText.equals("Sort by alphabet")){
                sortWishlistByAlphabet(chatId);
            } else if(messageText.equals("Count Tags")){
                handleTagCount(chatId);
            }
        } else if (update.hasMessage() && update.getMessage().hasDocument()) {

            long chatId = update.getMessage().getChatId();
            String fileId = update.getMessage().getDocument().getFileId();
            String fileName = update.getMessage().getDocument().getFileName();


            processImportFile(fileId, fileName, chatId);
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.startsWith("list_")) {
                boolean wishlist = false;
                showGameDetails(chatId, callbackData, wishlist);
            } else if (callbackData.startsWith("add_")) {
                addToWishlistCallback(update.getCallbackQuery());
            } else if (callbackData.startsWith("wishlist_")) {
                boolean wishlist = true;
                showGameDetails(chatId, callbackData, wishlist);
            } else if (callbackData.startsWith("remove_")) {
                removeGameFromWishlistCallback(update.getCallbackQuery());
            } else if (callbackData.startsWith("languages_")) {
                showAvailableLanguages(update.getCallbackQuery());
            }
        }
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
        editMessageText(gameName + " removed from your wishlist.", call.getMessage().getChatId(), call.getMessage().getMessageId());
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

        sendMessage(chatId, "Searching for games by tag '" + tag + "'...");
        System.out.println("Searching for games by tag START");


        List<Map.Entry<String, JSONObject>> games = DataManager.findGamesByTag(tag, DataManager.readDatabase());

        System.out.println("Searching for games by tag FINISH");

        if (games.isEmpty()) {
            sendMessage(chatId, "No games found with that tag.");
            return;
        }


        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();


        for (int i = 0; i < Math.min(games.size(), 20); i++) {
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

        sendMessage(chatId, "Searching for games with name '" + gameName + "'...");
        System.out.println("Searching for games by name START");

        List<Map.Entry<String, JSONObject>> games = DataManager.findGamesByName(gameName, DataManager.readDatabase());

        System.out.println("Searching for games by name FINISH");

        if (games.isEmpty()) {
            sendMessage(chatId, "No games found with that name.");
            return;
        }

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        for (int i = 0; i < Math.min(games.size(), 10); i++) {
            JSONObject game = games.get(i).getValue();
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

    private void addToWishlistCallback(CallbackQuery call) {
        String gameName = call.getData().split("_", 2)[1];
        long chatId = call.getMessage().getChatId();

        Map<String, Object> database = DataManager.readDatabase();
        List<JSONObject> games = DataManager.findGameByExactName(gameName, database);

        if (!games.isEmpty()) {
            JSONObject gameData = games.get(0);

            JSONObject gameDataEnd = new JSONObject();
            gameDataEnd.put("ID", gameData.optString("ID"));
            gameDataEnd.put("Name", gameData.optString("Name"));
            gameDataEnd.put("Price", gameData.optString("Price"));

            DataManager.addGameToWishlist(chatId, gameDataEnd);

            editMessageText(gameData.optString("Name") + " has been added to your wishlist.",
                    chatId, call.getMessage().getMessageId());
        } else {

            editMessageText("Game not found in the database.", chatId, call.getMessage().getMessageId());
        }
    }


    private void showWishlist(long chatId) {

        List<JSONObject> wishlist = DataManager.readWishlist(chatId);

        if (wishlist.isEmpty()) {
            sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

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

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Your Wishlist:");
        message.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

        sendMessage(chatId, "You have " + wishlist.size() + " games in your Wishlist.");

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




    public static JSONObject findGameByExactNameInWishlist(String gameName, long userId) {

        List<JSONObject> wishlist = DataManager.readWishlist(userId);
        System.out.println("Wishlist: " + wishlist);

        for (JSONObject game : wishlist) {
            if (game.optString("Name").equals(gameName)) {
                return game;
            }
        }
        return null;
    }

    private void showGameDetails(long chatId, String callbackData, boolean wishlist) {
        System.out.println("Processing game details callback...");

        String identifier = callbackData.split("_", 2)[1].trim();
        System.out.println("Game identifier extracted: " + identifier);
        Map<String, Object> database = DataManager.readDatabase();

        JSONObject gameData;
        if (wishlist) {

            JSONObject gameDataWishlist = findGameByExactNameInWishlist(identifier, chatId);
            if (gameDataWishlist != null) {
                List<JSONObject> gameInfoList = DataManager.findGameByExactName(identifier, database);
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
                sendMessage(chatId, caption);
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

            sendMessageWithInlineKeyboard(chatId, "Would you like to manage this game in your Wishlist?", inlineKeyboardMarkup);
        } else {
            String errorMessage = wishlist
                    ? "Game '" + identifier + "' not found in your Wishlist."
                    : "Information about game ID '" + identifier + "' not found in the database.";
            sendMessage(chatId, errorMessage);
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

    private void showAvailableLanguages(CallbackQuery call) {
        String gameId = call.getData().split("_", 2)[1];
        long chatId = call.getMessage().getChatId();

        List<JSONObject> games = DataManager.findGameByExactId(gameId, DataManager.readDatabase());
        if (!games.isEmpty()) {
            JSONObject game = games.get(0);


            List<String> languagesSub = game.optJSONArray("LanguagesSub") != null
                    ? game.optJSONArray("LanguagesSub").toList().stream().map(Object::toString).collect(Collectors.toList())
                    : Collections.emptyList();

            List<String> languagesAudio = game.optJSONArray("LanguagesAudio") != null
                    ? game.optJSONArray("LanguagesAudio").toList().stream().map(Object::toString).collect(Collectors.toList())
                    : Collections.emptyList();

            String languagesText = "Available Languages for " + game.optString("Name") + ":\n\n" +
                    "Subtitles:\n" +
                    (languagesSub.isEmpty() ? "No subtitles available." : String.join("\n", languagesSub)) + "\n\n" +
                    "Audio:\n" +
                    (languagesAudio.isEmpty() ? "No audio available." : String.join("\n", languagesAudio));


            sendMessage(chatId, languagesText);
        } else {

            sendMessage(chatId, "Game details not found.");
        }
    }





    private void chooseDownloadFormat(long chatId) {
        ReplyKeyboardMarkup markup = new ReplyKeyboardMarkup();
        markup.setResizeKeyboard(true);
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("Download as TXT"));
        row.add(new KeyboardButton("Download as JSON"));
        row.add(new KeyboardButton("Download as YAML"));
        row.add(new KeyboardButton("Back"));

        keyboard.add(row);
        markup.setKeyboard(keyboard);

        sendMessageWithKeyboard(chatId, "Choose a format to download your wishlist:", markup);
    }

    private void downloadWishlist(long chatId, String formatChoice) {
        String filename = null;

        switch (formatChoice) {
            case "txt":
                filename = DataManager.generateWishlistFileTxt(chatId);
                break;
            case "json":
                filename = DataManager.generateWishlistFileJson(chatId);
                break;
            case "yaml":
                filename = DataManager.generateWishlistFileYaml(chatId);
                break;
            default:
                sendMessage(chatId, "Unknown format. Please choose again.");
                return;
        }

        if (filename != null) {
            sendDocument(chatId, filename);
            new File(filename).delete();
        }
    }

    private void sendDocument(long chatId, String filePath) {
        SendDocument document = new SendDocument();
        document.setChatId(String.valueOf(chatId));
        document.setDocument(new InputFile(new File(filePath)));
        try {
            execute(document);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }



    public void processImportFile(String fileId, String fileName, long chatId) {
        try {

            GetFile getFileRequest = new GetFile(fileId);
            org.telegram.telegrambots.meta.api.objects.File file = execute(getFileRequest);
            String filePath = file.getFilePath();

            String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

            byte[] downloadedFile = downloadFileAsByteArray(filePath);

            List<JSONObject> importedData;
            switch (fileExtension) {
                case "txt":
                    importedData = DataManager.readTxtFile(downloadedFile);
                    break;
                case "yaml":
                case "yml":
                    importedData = DataManager.readYamlFile(downloadedFile);
                    break;
                case "json":
                    importedData = DataManager.readJosnFile(downloadedFile);
                    break;
                default:
                    sendMessage(chatId, "Unsupported file format. Please upload a txt or yaml file.");
                    return;
            }

            DataManager.mergeWishlists(chatId, importedData);
            sendMessage(chatId, "Wishlist imported and updated successfully.");

        } catch (TelegramApiException | IOException e) {
            sendMessage(chatId, "An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private byte[] downloadFileAsByteArray(String filePath) throws TelegramApiException, IOException {
        InputStream fileStream = downloadFileAsStream(filePath);
        return fileStream.readAllBytes();
    }


    public void importWishlist(long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("Please send the wishlist file.");
        try {
            execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    ///sorting

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
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sortWishlistByDate(long chatId) {
        List<JSONObject> wishlist = DataManager.readWishlist(chatId);
        Map<String, Object> database = DataManager.readDatabase();

        List<JSONObject> sortedWishlist = DataManager.sortWishlistByDate(wishlist, database);

        if (sortedWishlist.isEmpty()) {
            sendMessage(chatId, "Your wishlist is empty.");
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
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sortWishlistByReviews(long chatId) {
        List<JSONObject> wishlist = DataManager.readWishlist(chatId);
        Map<String, Object> database = DataManager.readDatabase();

        List<JSONObject> sortedWishlist = DataManager.sortWishlistByReviews(wishlist, database);

        if (sortedWishlist.isEmpty()) {
            sendMessage(chatId, "Your wishlist is empty.");
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
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    public void sortWishlistByAlphabet(long chatId) {
        List<JSONObject> wishlist = DataManager.readWishlist(chatId);

        List<JSONObject> sortedWishlist = DataManager.sortWishlistByAlphabet(wishlist);

        if (sortedWishlist.isEmpty()) {
            sendMessage(chatId, "Your wishlist is empty.");
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
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    ///tag count

    public void handleTagCount(long chatId) {
        List<JSONObject> wishlist = DataManager.readWishlist(chatId);
        Map<String, Integer> tagCounter = new HashMap<>();
        Map<String, List<String>> tagToGames = new HashMap<>();

        List<String> gameIds = wishlist.stream()
                .map(game -> game.optString("ID"))
                .collect(Collectors.toList());

        for (String gameId : gameIds) {
            List<JSONObject> games = DataManager.findGameByExactId(gameId, DataManager.readDatabase());
            if (!games.isEmpty()) {
                JSONObject game = games.get(0);
                JSONArray tags = game.optJSONArray("TopTags");

                if (tags != null) {
                    for (int i = 0; i < tags.length(); i++) {
                        String tag = tags.optString(i);
                        tagCounter.put(tag, tagCounter.getOrDefault(tag, 0) + 1);

                        if (!tagToGames.containsKey(tag)) {
                            tagToGames.put(tag, new ArrayList<>());
                        }
                        tagToGames.get(tag).add(game.optString("Name"));
                    }
                }
            }
        }

        List<Map.Entry<String, Integer>> topTags = tagCounter.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .limit(10)
                .collect(Collectors.toList());

        ByteArrayOutputStream outputStream = createTagDistributionChart(topTags);

        StringBuilder tagListText = new StringBuilder("Top 10 Tags in Wishlist Games\n\n");
        for (Map.Entry<String, Integer> entry : topTags) {
            String tag = entry.getKey();
            List<String> games = tagToGames.get(tag);
            String gamesList = String.join(", ", games);
            tagListText.append(String.format("%s: %s\n\n", tag, gamesList));
        }

        sendTagChart(chatId, outputStream);
        sendTagListText(chatId, tagListText.toString());
    }

    private ByteArrayOutputStream createTagDistributionChart(List<Map.Entry<String, Integer>> topTags) {

        DefaultPieDataset dataset = new DefaultPieDataset();

        for (Map.Entry<String, Integer> entry : topTags) {
            String tag = entry.getKey();
            int count = entry.getValue();
            dataset.setValue(tag, count);
        }


        JFreeChart pieChart = ChartFactory.createPieChart("Top 10 Tags in Wishlist Games", dataset, true, true, false);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ChartUtils.writeChartAsPNG(outputStream, pieChart, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream;
    }

    private void sendTagChart(long chatId, ByteArrayOutputStream outputStream) {

        SendPhoto photoMessage = new SendPhoto();
        photoMessage.setChatId(String.valueOf(chatId));
        photoMessage.setPhoto(new InputFile(new ByteArrayInputStream(outputStream.toByteArray()), "tags_chart.png"));

        try {
            execute(photoMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendTagListText(long chatId, String tagListText) {

        final int MAX_MESSAGE_LENGTH = 4096;
        for (int i = 0; i < tagListText.length(); i += MAX_MESSAGE_LENGTH) {
            String msg = tagListText.substring(i, Math.min(i + MAX_MESSAGE_LENGTH, tagListText.length()));
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(msg);
            try {
                execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }

}
