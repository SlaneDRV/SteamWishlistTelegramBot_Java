package MainFunctions;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DataManager {

    private static final String TGBOT_DIR = "TgBot";
    private static final String STEAMAPI_DIR = "SteamAPI";
    private static final String JSON_DIR = "JSON";
    private static final String DETAILED_GAMES_FILE = "detailed_games_transformed.json";

    public static Map<String, Object> DATABASE;

    public static String getDetailedGamesPath() {
        return String.join(File.separator, TGBOT_DIR, STEAMAPI_DIR, JSON_DIR, DETAILED_GAMES_FILE);
    }
    private static final String WISHLIST_DIR = "Wishlists";

    public static String getWishlistPath(long userId) {
        return WISHLIST_DIR + File.separator + userId + "_wishlist.json";
    }


}
