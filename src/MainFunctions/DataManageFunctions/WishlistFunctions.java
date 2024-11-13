package MainFunctions.DataManageFunctions;

import MainFunctions.DataManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class WishlistFunctions {


    public static List<JSONObject> readWishlist(long userId) {
        String filename = DataManager.getWishlistPath(userId);
        System.out.println("Reading wishlist from: " + filename);

        if (!Files.exists(Paths.get(filename))) {
            System.out.println("Wishlist file does not exist: " + filename);
            return new ArrayList<>();
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(filename)));

            JSONArray jsonArray = new JSONArray(content);
            List<JSONObject> wishlist = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                wishlist.add(jsonArray.getJSONObject(i));
            }
            return wishlist;

        } catch (IOException e) {
            System.out.println("Error reading wishlist file: " + e.getMessage());
            return new ArrayList<>();
        } catch (JSONException e) {
            System.out.println("Invalid JSON format in wishlist file: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public static void saveWishlist(long userId, List<JSONObject> wishlist) {
        String filename = DataManager.getWishlistPath(userId);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            System.out.println("Save wishlist for user: " + userId);
            JSONArray jsonArray = new JSONArray(wishlist);
            writer.write(jsonArray.toString(2)); // Параметр 2 для отступов
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static List<JSONObject> addGameToWishlist(long userId, JSONObject game) {
        List<JSONObject> wishlist = readWishlist(userId);

        if (!checkWishlist(userId, game.optString("Name"))) {
            wishlist.add(game);
            saveWishlist(userId, wishlist);
            System.out.println("Added game to wishlist of user: " + userId);
        }

        return wishlist;
    }

    public static boolean checkWishlist(long userId, String gameName) {
        List<JSONObject> wishlist = readWishlist(userId);

        for (JSONObject game : wishlist) {
            if (game.optString("Name").equals(gameName)) {
                return true;
            }
        }
        return false;
    }

    public static List<JSONObject> removeGameFromWishlist(long userId, String gameName) {
        System.out.println("Remove game from wishlist of user: " + userId);
        List<JSONObject> wishlist = readWishlist(userId);
        List<JSONObject> newWishlist = new ArrayList<>();
        for (JSONObject game : wishlist) {
            if (!game.optString("Name").equals(gameName)) {
                newWishlist.add(game);
            }
        }
        saveWishlist(userId, newWishlist);
        return newWishlist;
    }
}
