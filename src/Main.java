/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */



/**
 *
 * @author Anton Sasnouski
 */
import MainFunctions.DataManager;
import MainFunctions.Handlers;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) throws TelegramApiException {

        Handlers bot = new Handlers();

        TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
        try {
            botsApi.registerBot(bot);
            DataManager.preloadDatabase().thenRun(() -> {
                try {
                    botsApi.registerBot(bot);
                    System.out.println("Bot successfully started!");
                } catch (TelegramApiException e) {
                    System.out.println("Failed to register bot: " + e.getMessage());
                }
            });
            
        } catch (TelegramApiException e) {
        }
    }
}


