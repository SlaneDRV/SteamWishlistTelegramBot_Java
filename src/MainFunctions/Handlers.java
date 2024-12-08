package MainFunctions;

import MainFunctions.HandlersFunctions.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;


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

        ImportHandler Import = new ImportHandler();
        SortsHandler sort = new SortsHandler();
        TagCountHandler tagCounter = new TagCountHandler();
        DownloadHandler download = new DownloadHandler();
        LanguageHandler language = new LanguageHandler();
        WishlistHandler wishlist = new WishlistHandler();
        GameDetailsHandler game = new GameDetailsHandler();
        SearchsHandler search = new SearchsHandler();
        RemovalHandler remove = new RemovalHandler();
        MessageHandler message = new MessageHandler();
        PriceHandler priceHandler = new PriceHandler();

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                message.sendMainMenu(chatId);
            } else if (messageText.equals("Back")) {
                message.sendMainMenu(chatId);
            } else if (messageText.equals("Find a New Game")) {
                message.sendSearchOptions(chatId);
            } else if (messageText.equals("Find by name")) {
                waitingForName = true;
                message.sendMessage(chatId, "Please enter the name of the game you're looking for.");
            } else if (messageText.equals("Find by tag")) {
                waitingForTag = true;
                message.sendMessage(chatId, "Please enter the tag of the game you're looking for.");
            } else if (waitingForTag) {
                search.searchGameByTag(chatId, messageText);
                waitingForTag = false;
            } else if (waitingForName) {
                search.searchGameByName(chatId, messageText);
                waitingForName = false;
            } else if (messageText.equals("Wishlist")) {
                wishlist.showWishlist(chatId);
            } else if (messageText.equals("View Wishlist")) {
                wishlist.showWishlist(chatId);
            } else if (messageText.equals("Remove Game from Wishlist")) {
                remove.promptForGameRemoval(chatId);
            } else if (messageText.equals("Download Wishlist")) {
                download.chooseDownloadFormat(chatId);
            } else if (messageText.startsWith("Download as")) {
                String formatChoice = messageText.split(" ")[2].toLowerCase();
                download.downloadWishlist(chatId, formatChoice);
            } else if (messageText.equals("Import Wishlist")) {
                Import.importWishlist(chatId);
            } else if(messageText.equals("Sort")){
                sort.handleSortCommand(chatId);
            } else if(messageText.equals("Sort by date")){
                sort.sortWishlistByDate(chatId);
            }
            else if(messageText.equals("Sort by reviews")){
                sort.sortWishlistByReviews(chatId);
            }
            else if(messageText.equals("Sort by alphabet")){
                sort.sortWishlistByAlphabet(chatId);
            } else if(messageText.equals("Count Tags")){
                tagCounter.handleTagCount(chatId);
            }
            else if (messageText.equals("Calculate Total Price")) {
                priceHandler.handleTotalPrice(chatId);
            }
        } else if (update.hasMessage() && update.getMessage().hasDocument()) {

            long chatId = update.getMessage().getChatId();
            String fileId = update.getMessage().getDocument().getFileId();
            String fileName = update.getMessage().getDocument().getFileName();


            Import.processImportFile(fileId, fileName, chatId);
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.startsWith("list_")) {
                boolean Wishlist = false;
                game.showGameDetails(chatId, callbackData, Wishlist);
            } else if (callbackData.startsWith("add_")) {
                wishlist.addToWishlistCallback(update.getCallbackQuery());
            } else if (callbackData.startsWith("wishlist_")) {
                boolean Wishlist = true;
                game.showGameDetails(chatId, callbackData, Wishlist);
            } else if (callbackData.startsWith("remove_")) {
                remove.removeGameFromWishlistCallback(update.getCallbackQuery());
            } else if (callbackData.startsWith("languages_")) {
                language.showAvailableLanguages(update.getCallbackQuery());
            } else if (callbackData.startsWith("price_")) {
                priceHandler.handlePriceRegion(chatId, update.getCallbackQuery());
            }
            else if (callbackData.startsWith("update_")) {

                int appId = Integer.parseInt(callbackData.split("_")[1]);
                System.out.println("callBackData: " + callbackData);
                boolean isWishlist = Boolean.parseBoolean(callbackData.split("_")[2]);
                game.updateGameInfo(chatId, appId, isWishlist);
            }
        }
    }

}
