package MainFunctions.HandlersFunctions;

import MainFunctions.DataManageFunctions.WishlistFunctions;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public class RemovalHandler {

    private final MessageHandler message = new MessageHandler();
    private final WishlistFunctions Wishlist = new WishlistFunctions();
    public void promptForGameRemoval(long chatId) {
        List<JSONObject> wishlist = Wishlist.readWishlist(chatId);
        if (wishlist.isEmpty()) {
            message.sendMessage(chatId, "Your wishlist is empty.");
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
        message.sendMessageWithInlineKeyboard(chatId, "Choose a game to remove from your wishlist:", markup);
    }

    public void removeGameFromWishlistCallback(CallbackQuery call) {
        String gameName = call.getData().split("_", 2)[1];
        WishlistFunctions.removeGameFromWishlist(call.getMessage().getChatId(), gameName);

        String currentText = call.getMessage().getText();
        String newText = gameName + " removed from your wishlist.";

        message.editMessageText(newText, call.getMessage().getChatId(), call.getMessage().getMessageId(), currentText);

    }
}
