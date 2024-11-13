package MainFunctions.DataManageFunctions;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class SortingFunctions {
    public static List<JSONObject> sortWishlistByDate(List<JSONObject> wishlist, Map<String, Object> database) {
        List<Map.Entry<JSONObject, LocalDate>> sortedWishlist = new ArrayList<>();

        DateTimeFormatter standardFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy");

        for (JSONObject game : wishlist) {
            String gameId = game.optString("ID");
            if (!gameId.isEmpty()) {
                List<JSONObject> dbGame = FindExactGame.findGameByExactId(gameId, database);
                if (!dbGame.isEmpty()) {
                    JSONObject gameData = dbGame.get(0);
                    String releaseDate = gameData.optString("ReleaseDate");

                    LocalDate parsedDate = parseReleaseDate(releaseDate, standardFormatter);
                    if (parsedDate != null) {
                        sortedWishlist.add(new AbstractMap.SimpleEntry<>(game, parsedDate));
                    }
                }
            }
        }

        sortedWishlist.sort((x, y) -> y.getValue().compareTo(x.getValue()));
        return sortedWishlist.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    private static LocalDate parseReleaseDate(String releaseDate, DateTimeFormatter standardFormatter) {
        try {
            if (releaseDate.equalsIgnoreCase("To be announced")) {
                return LocalDate.MAX;
            } else if (releaseDate.matches("\\d{4}")) {
                return LocalDate.parse(releaseDate + "-01-01");
            } else {
                return LocalDate.parse(releaseDate, standardFormatter);
            }
        } catch (DateTimeParseException e) {

            return null;
        }
    }


    public static List<JSONObject> sortWishlistByReviews(List<JSONObject> wishlist, Map<String, Object> database) {
        List<Map.Entry<JSONObject, Integer>> sortedWishlist = new ArrayList<>();

        for (JSONObject game : wishlist) {
            String gameId = game.optString("ID");
            if (!gameId.isEmpty()) {
                List<JSONObject> dbGame = FindExactGame.findGameByExactId(gameId, database);
                if (!dbGame.isEmpty()) {
                    JSONObject gameData = dbGame.get(0);
                    int positiveReviews = gameData.optInt("PositiveReviews", 0);
                    int negativeReviews = gameData.optInt("NegativeReviews", 0);
                    int totalReviews = positiveReviews + negativeReviews;
                    sortedWishlist.add(new AbstractMap.SimpleEntry<>(game, totalReviews));
                }
            }
        }

        sortedWishlist.sort((x, y) -> Integer.compare(y.getValue(), x.getValue()));
        return sortedWishlist.stream().map(Map.Entry::getKey).collect(Collectors.toList());
    }

    public static List<JSONObject> sortWishlistByAlphabet(List<JSONObject> wishlist) {

        wishlist.sort(Comparator.comparing(game -> game.optString("Name", "").toLowerCase()));
        return wishlist;
    }



}
