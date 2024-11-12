package MainFunctions.HandlersFunctions;

import MainFunctions.DataManageFunctions.*;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import java.text.SimpleDateFormat;

public class PriceHandler {

    public void handleTotalPrice(long chatId, Update update) {

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        InlineKeyboardButton buttonRu = new InlineKeyboardButton();
        buttonRu.setText("🇷🇺");
        buttonRu.setCallbackData("price_ru");

        InlineKeyboardButton buttonUa = new InlineKeyboardButton();
        buttonUa.setText("🇺🇦");
        buttonUa.setCallbackData("price_ua");

        InlineKeyboardButton buttonTr = new InlineKeyboardButton();
        buttonTr.setText("🇹🇷");
        buttonTr.setCallbackData("price_tr");

        InlineKeyboardButton buttonKz = new InlineKeyboardButton();
        buttonKz.setText("🇰🇿");
        buttonKz.setCallbackData("price_kz");

        InlineKeyboardButton buttonPl = new InlineKeyboardButton();
        buttonPl.setText("🇵🇱");
        buttonPl.setCallbackData("price_pl");

        InlineKeyboardButton buttonCn = new InlineKeyboardButton();
        buttonCn.setText("🇨🇳");
        buttonCn.setCallbackData("price_cn");

        // Добавляем кнопки в строку
        rowsInline.add(List.of(buttonRu, buttonUa, buttonTr, buttonKz, buttonPl, buttonCn));

        markup.setKeyboard(rowsInline);

        Message message = new Message();

        message.sendMessageWithInlineKeyboard(chatId, "Select region:", markup);
    }

    private static final Map<String, Double> exchangeRates = new HashMap<>();
    static {
        exchangeRates.put("RUB", 0.013);
        exchangeRates.put("UAH", 0.027);
        exchangeRates.put("TRY", 0.060);
        exchangeRates.put("KZT", 0.0023);
        exchangeRates.put("PLN", 0.24);
        exchangeRates.put("CNY", 0.14);
        exchangeRates.put("USD", 1.0);
    }

    public void handlePriceRegion(long userId,CallbackQuery callbackQuery) {
        String regionCode = callbackQuery.getData().split("_")[1];
        List<JSONObject> wishlist = WishlistFunctions.readWishlist(userId);

        List<JSONObject> gameInfo = new ArrayList<>();
        for (JSONObject game : wishlist) {
            String gameId = game.optString("ID");
            List<JSONObject> games = FindExactGame.findGameByExactId(gameId, Database.readDatabase());
            if (!games.isEmpty()) {
                gameInfo.add(games.get(0));
            }
        }

        double totalPrice = 0.0;
        String currency = "USD";
        List<String> availableGames = new ArrayList<>();
        List<String> unavailableGames = new ArrayList<>();
        List<String> freeGames = new ArrayList<>();
        List<String> upcomingGames = new ArrayList<>();

        for (JSONObject game : gameInfo) {
            String gameName = game.optString("Name");
            String gamePriceStr = game.optString("Price");
            double gamePrice = parsePrice(gamePriceStr);

            if (gamePrice == 0.0) {
                freeGames.add(gameName);
                continue;
            }

            if (isUpcomingGame(game.optString("ReleaseDate"))) {
                upcomingGames.add(gameName);
                continue;
            }

            double regionalPrice = convertToRegionPrice(gamePrice, regionCode);
            totalPrice += regionalPrice;
            availableGames.add(gameName);
        }

        String regionName = getRegionName(regionCode);
        String currencySymbol = getCurrencySymbol(currency);

        String response = generateResponse(regionName, totalPrice, currencySymbol, availableGames, unavailableGames, freeGames, upcomingGames);

        System.out.println(response);
    }


    private double parsePrice(String priceStr) {
        try {
            priceStr = priceStr.replaceAll("[^0-9.,]", "").replace(",", ".");
            return Double.parseDouble(priceStr);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private double convertToRegionPrice(double price, String regionCode) {
        Double exchangeRate = exchangeRates.get(regionCode.toUpperCase());
        if (exchangeRate != null) {
            return price * exchangeRate;
        }
        return price;
    }

    private boolean isUpcomingGame(String releaseDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
            Date releaseDate = sdf.parse(releaseDateStr);
            return releaseDate.after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private String getRegionName(String regionCode) {
        switch (regionCode) {
            case "ru":
                return "Russia";
            case "ua":
                return "Ukraine";
            case "tr":
                return "Turkey";
            case "kz":
                return "Kazakhstan";
            case "pl":
                return "Poland";
            case "cn":
                return "China";
            default:
                return "Unknown region";
        }
    }

    private String getCurrencySymbol(String currency) {
        switch (currency) {
            case "RUB":
                return "₽";
            case "UAH":
                return "₴";
            case "TRY":
                return "₺";
            case "KZT":
                return "₸";
            case "PLN":
                return "zł";
            case "CNY":
                return "¥";
            default:
                return "$";
        }
    }

    private String generateResponse(String regionName, double totalPrice, String currencySymbol,
                                    List<String> availableGames, List<String> unavailableGames,
                                    List<String> freeGames, List<String> upcomingGames) {
        return String.format(
                "Total price of wishlist games available in %s: %s%.2f\n\n" +
                        "Available games:\n%s\n" +
                        "Free games:\n%s\n" +
                        "Upcoming games:\n%s\n" +
                        "Unavailable games:\n%s\n",
                regionName,
                currencySymbol,
                totalPrice,
                availableGames.isEmpty() ? "All games are available." : String.join("\n", availableGames),
                freeGames.isEmpty() ? "No free games." : String.join("\n", freeGames),
                upcomingGames.isEmpty() ? "No upcoming games." : String.join("\n", upcomingGames),
                unavailableGames.isEmpty() ? "All games are available." : String.join("\n", unavailableGames)
        );
    }
}
