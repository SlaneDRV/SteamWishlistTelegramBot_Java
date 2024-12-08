package MainFunctions.DataManageFunctions;

import org.json.JSONObject;

import java.util.List;

public class MergeFileFunctions {

    /*
        This function compares the imported wishlist with the current wishlist of a user.
        It adds new games from the imported wishlist to the current wishlist if they are not already present.
     */

    public void mergeWishlists(long userId, List<JSONObject> importedData) {

        List<JSONObject> currentWishlist = WishlistFunctions.readWishlist(userId);

        for (JSONObject importedGame : importedData) {
            String importedGameId = importedGame.optString("ID");
            String importedGameName = importedGame.optString("Name");

            boolean existsInWishlist = currentWishlist.stream().anyMatch(existingGame ->
                    importedGameId.equals(existingGame.optString("ID")) &&
                            importedGameName.equals(existingGame.optString("Name"))
            );

            if (!existsInWishlist) {
                currentWishlist.add(importedGame);
            }
        }

        WishlistFunctions.saveWishlist(userId, currentWishlist);
    }
}
