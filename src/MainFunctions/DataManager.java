package MainFunctions;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DataManager {

    private static final String DATA = "Data";
    private static final String JSON = "JSON";
    private static final String GAMES = "Games";
    private static final String GAMES_FILE = "detailed_games_transformed.json";

    public static Map<String, Object> DATABASE;

    public static String getGamesPath() {
        return String.join(File.separator, DATA, JSON, GAMES, GAMES_FILE);
    }
    private static final String WISHLIST_DIR = "Data/JSON/Wishlists";

    public static String getWishlistPath(long userId) {
        return WISHLIST_DIR + File.separator + userId + "_wishlist.json";
    }


}
