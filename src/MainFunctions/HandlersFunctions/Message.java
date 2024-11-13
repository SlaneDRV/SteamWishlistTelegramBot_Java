package MainFunctions.HandlersFunctions;

import MainFunctions.Handlers;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Message {
    private Handlers handler = new Handlers();
    public void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            handler.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }



    public void editMessageText(String newText, long chatId, int messageId, String currentText) {
        if (newText.equals(currentText)) {
            System.out.println("Text is not modified, skipping editMessageText to avoid error.");
            return;
        }

        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setMessageId(messageId);
        message.setText(newText);

        try {
            handler.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    public void sendMainMenu(long chatId) {
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

    public void sendSearchOptions(long chatId) {
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



    public void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        try {
            handler.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }





    public void sendMessageWithInlineKeyboard(long chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        try {
            handler.execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendDocument(long chatId, String filePath) {
        SendDocument document = new SendDocument();
        document.setChatId(String.valueOf(chatId));
        document.setDocument(new InputFile(new File(filePath)));
        try {
            handler.execute(document);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
