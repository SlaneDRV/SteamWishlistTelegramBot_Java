package MainFunctions.DataManageFunctions;

import org.json.JSONObject;

import java.util.List;

public class MergeFile {

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
