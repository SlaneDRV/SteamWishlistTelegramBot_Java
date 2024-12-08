package MainFunctions.DataManageFunctions;

import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class GenerateFileFunctions {

    public static final WishlistFunctions Wishlist = new WishlistFunctions();

    public static String generateWishlistFileTxt(long userId) {
        List<JSONObject> wishlist = Wishlist.readWishlist(userId);
        String filename = String.format("wishlist_%d.txt", userId);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            for (JSONObject game : wishlist) {
                String price = game.optDouble("Price", 0.0) != 0.0 ? String.valueOf(game.optDouble("Price")) : "Free";
                writer.write(String.format("%s: %s - %s%n", game.optString("ID"), game.optString("Name"), price));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filename;
    }

    public static List<JSONObject> filterWishlistFields(List<JSONObject> wishlist) {
        List<JSONObject> filteredWishlist = new ArrayList<>();

        for (JSONObject game : wishlist) {
            JSONObject filteredGame = new JSONObject();
            filteredGame.put("ID", game.optString("ID"));
            filteredGame.put("Name", game.optString("Name"));
            filteredGame.put("Price", game.optDouble("Price", 0.0) != 0.0 ? String.valueOf(game.optDouble("Price")) : "Free");
            filteredWishlist.add(filteredGame);
        }

        return filteredWishlist;
    }

    public static String generateWishlistFileJson(long userId) {
        List<JSONObject> wishlist = Wishlist.readWishlist(userId);
        List<JSONObject> filteredWishlist = filterWishlistFields(wishlist);
        String filename = String.format("wishlist_%d.json", userId);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            writer.write(filteredWishlist.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filename;
    }

    public static String generateWishlistFileYaml(long userId) {
        List<JSONObject> wishlist = Wishlist.readWishlist(userId);
        List<Map<String, Object>> filteredWishlist = filterWishlistFields(wishlist).stream()
                .map(JSONObject::toMap)
                .collect(Collectors.toList());

        String filename = String.format("wishlist_%d.yaml", userId);

        Yaml yaml = new Yaml();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename))) {
            yaml.dump(filteredWishlist, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return filename;
    }

}
