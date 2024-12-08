package MainFunctions.HandlersFunctions;

import MainFunctions.DataManageFunctions.WishlistFunctions;
import SteamAPI.GameProcessor;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PriceHandler {

    private static final Map<String, Double> exchangeRates = Map.of(
            "ru", 0.013,  // Russian Ruble to USD
            "ua", 0.027,  // Ukrainian Hryvnia to USD
            "tr", 0.036,  // Turkish Lira to USD
            "kz", 0.0022, // Kazakhstani Tenge to USD
            "pl", 0.23,   // Polish Zloty to USD
            "cn", 0.14    // Chinese Yuan to USD
    );

    public static double getExchangeRate(String regionCode) {
        return exchangeRates.getOrDefault(regionCode, 1.0); // Default to 1.0 if regionCode not found
    }

    public void handleTotalPrice(long chatId) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        // Add buttons for each region
        rowsInline.add(createRegionButtons());
        markup.setKeyboard(rowsInline);

        MessageHandler message = new MessageHandler();
        message.sendMessageWithInlineKeyboard(chatId, "Select a region to calculate total price:", markup);
    }

    private List<InlineKeyboardButton> createRegionButtons() {
        List<InlineKeyboardButton> regionButtons = new ArrayList<>();
        Map<String, String> regions = Map.of(
                "ru", "🇷🇺",
                "ua", "🇺🇦",
                "tr", "🇹🇷",
                "kz", "🇰🇿",
                "pl", "🇵🇱",
                "cn", "🇨🇳"
        );

        for (Map.Entry<String, String> region : regions.entrySet()) {
            InlineKeyboardButton button = new InlineKeyboardButton();
            button.setText(region.getValue());
            button.setCallbackData("price_" + region.getKey());
            regionButtons.add(button);
        }
        return regionButtons;
    }

    public void handlePriceRegion(long chatId, CallbackQuery callbackQuery) {
        String regionCode = callbackQuery.getData().split("_")[1];
        List<JSONObject> wishlist = WishlistFunctions.readWishlist(chatId);

        if (wishlist.isEmpty()) {
            sendMessage(chatId, "Your wishlist is empty.");
            return;
        }

        // Prepare price calculations
        List<String> freeGames = new ArrayList<>();
        List<String> priceNotAnnouncedGames = new ArrayList<>();
        List<String> unavailableGames = new ArrayList<>();
        List<String> availableGames = new ArrayList<>();

        double totalUSPrice = 0.0;
        double totalRegionPrice = 0.0;

        GameProcessor gameProcessor = new GameProcessor();
        try {
            for (JSONObject game : wishlist) {
                String gameName = game.optString("Name", "Unknown");
                int appId = game.optInt("ID", -1);
                String gamePrice = game.optString("Price", "").toLowerCase();

                if (appId == -1) continue;

                try {
                    // Fetch prices
                    String usPriceStr = gameProcessor.getGamePriceByRegion(appId, "US");
                    String regionPriceStr = gameProcessor.getGamePriceByRegion(appId, regionCode);

                    double usPrice = parsePrice(usPriceStr);
                    double regionPrice = parsePrice(regionPriceStr);

                    // Check for "Price Not Announced"
                    if (gamePrice.equals("coming soon")) {
                        priceNotAnnouncedGames.add(gameName);
                    } else if (usPrice == 0.0) {
                        // Free games
                        freeGames.add(gameName);
                    } else if (regionPrice == 0.0) {
                        // Unavailable games
                        unavailableGames.add(gameName);
                    } else {
                        // Available games
                        availableGames.add(gameName);
                        totalUSPrice += usPrice;
                        totalRegionPrice += convertToRegionPrice(regionPrice, regionCode);
                    }

                } catch (Exception e) {
                    unavailableGames.add(gameName); // Treat as unavailable in case of failure
                }
            }
        } finally {
            try {
                gameProcessor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Build the response message
        String response = generateResponse(
                regionCode, totalUSPrice, totalRegionPrice,
                freeGames, priceNotAnnouncedGames, unavailableGames, availableGames
        );

        sendMessage(chatId, response);
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
        // For Turkey, prices are already in USD, so return directly
        if (regionCode.equals("tr")) {
            return price;
        }
        double exchangeRate = getExchangeRate(regionCode);
        return price * exchangeRate;
    }


    private String generateResponse(
            String regionCode, double totalUSPrice, double totalRegionPrice,
            List<String> freeGames, List<String> priceNotAnnouncedGames,
            List<String> unavailableGames, List<String> availableGames) {

        String regionName = getRegionName(regionCode);
        String currencySymbol = getCurrencySymbol(regionCode);

        // Если регион — Турция (или другой с долларами), цена остаётся как есть
        double localRegionPrice = regionCode.equals("tr")
                ? totalRegionPrice
                : totalRegionPrice / getExchangeRate(regionCode);

        // Формируем строку для цены с учётом региональной валюты
        String localCurrencyDisplay = regionCode.equals("tr")
                ? String.format("$%.2f", totalRegionPrice) // Турция отображает только в долларах
                : String.format("%.0f %s ($%.2f)", localRegionPrice, currencySymbol, totalRegionPrice);

        return String.format(
                "Wishlist Price Summary:\n\n" +
                        "Region: %s\n" +
                        "Total US Price: $%.2f\n" +
                        "Total %s Price: %s\n\n" +
                        "Available Games:\n%s\n\n" +
                        "Free Games:\n%s\n\n" +
                        "Price Not Announced:\n%s\n\n" +
                        "Unavailable Games:\n%s\n",
                regionName, totalUSPrice, regionName, localCurrencyDisplay,
                availableGames.isEmpty() ? "No available games." : String.join("\n", availableGames),
                freeGames.isEmpty() ? "No free games." : String.join("\n", freeGames),
                priceNotAnnouncedGames.isEmpty() ? "No games with unannounced prices." : String.join("\n", priceNotAnnouncedGames),
                unavailableGames.isEmpty() ? "No unavailable games." : String.join("\n", unavailableGames)
        );
    }




    private String getRegionName(String regionCode) {
        Map<String, String> regions = Map.of(
                "ru", "Russia",
                "ua", "Ukraine",
                "tr", "Turkey",
                "kz", "Kazakhstan",
                "pl", "Poland",
                "cn", "China"
        );
        return regions.getOrDefault(regionCode, "Unknown Region");
    }

    private String getCurrencySymbol(String regionCode) {
        Map<String, String> symbols = Map.of(
                "ru", "₽",
                "ua", "₴",
                "tr", "₺",
                "kz", "₸",
                "pl", "zł",
                "cn", "¥"
        );
        return symbols.getOrDefault(regionCode, "$");
    }

    private void sendMessage(long chatId, String text) {
        MessageHandler message = new MessageHandler();
        message.sendMessage(chatId, text);
    }
}
