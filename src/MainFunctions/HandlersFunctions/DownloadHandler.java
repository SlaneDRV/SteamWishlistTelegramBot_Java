package MainFunctions.HandlersFunctions;

import MainFunctions.DataManageFunctions.GenerateFileFunctions;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadHandler {
    private MessageHandler message = new MessageHandler();

    private GenerateFileFunctions file = new GenerateFileFunctions();
    public void chooseDownloadFormat(long chatId) {
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

        message.sendMessageWithKeyboard(chatId, "Choose a format to download your wishlist:", markup);
    }

    public void downloadWishlist(long chatId, String formatChoice) {
        String filename = null;

        switch (formatChoice) {
            case "txt":
                filename = file.generateWishlistFileTxt(chatId);
                break;
            case "json":
                filename = file.generateWishlistFileJson(chatId);
                break;
            case "yaml":
                filename = file.generateWishlistFileYaml(chatId);
                break;
            default:
                message.sendMessage(chatId, "Unknown format. Please choose again.");
                return;
        }

        if (filename != null) {
            message.sendDocument(chatId, filename);
            new File(filename).delete();
        }
    }
}
