package Tests.DataManager;

import MainFunctions.DataManageFunctions.SortingFunctions;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class SortingFunctionsTest {

    private List<JSONObject> wishlist;
    private Map<String, Object> database;

    @BeforeEach
    void setUp() {
        // Przygotowanie bazy danych z przykładami rzeczywistych gier
        database = Map.of(
                "570", Map.of(
                        "ID", "570",
                        "Name", "Dota 2",
                        "ReleaseDate", "Jul 9, 2013",
                        "PositiveReviews", 1200000,
                        "NegativeReviews", 100000
                ),
                "730", Map.of(
                        "ID", "730",
                        "Name", "Counter-Strike: Global Offensive",
                        "ReleaseDate", "Aug 21, 2012",
                        "PositiveReviews", 1500000,
                        "NegativeReviews", 150000
                ),
                "440", Map.of(
                        "ID", "440",
                        "Name", "Team Fortress 2",
                        "ReleaseDate", "Oct 10, 2007",
                        "PositiveReviews", 700000,
                        "NegativeReviews", 50000
                ),
                "1091500", Map.of(
                        "ID", "1091500",
                        "Name", "Cyberpunk 2077",
                        "ReleaseDate", "Dec 10, 2020",
                        "PositiveReviews", 500000,
                        "NegativeReviews", 250000
                ),
                "271590", Map.of(
                        "ID", "271590",
                        "Name", "Grand Theft Auto V",
                        "ReleaseDate", "Apr 14, 2015",
                        "PositiveReviews", 2000000,
                        "NegativeReviews", 300000
                )
        );

        // Przygotowanie wishlist
        wishlist = List.of(
                new JSONObject(Map.of("ID", "570", "Name", "Dota 2")),
                new JSONObject(Map.of("ID", "730", "Name", "Counter-Strike: Global Offensive")),
                new JSONObject(Map.of("ID", "440", "Name", "Team Fortress 2")),
                new JSONObject(Map.of("ID", "1091500", "Name", "Cyberpunk 2077")),
                new JSONObject(Map.of("ID", "271590", "Name", "Grand Theft Auto V"))
        );
    }

    @Test
    void testSortWishlistByDate() {
        List<JSONObject> sorted = SortingFunctions.sortWishlistByDate(wishlist, database);

        assertNotNull(sorted);
        assertEquals(5, sorted.size());
        assertEquals("Dota 2", sorted.get(0).getString("Name")); // Najnowsza gra w wishlist
        assertEquals("Team Fortress 2", sorted.get(4).getString("Name")); // Najstarsza gra w wishlist
    }

    @Test
    void testSortWishlistByReviews() {
        List<JSONObject> sorted = SortingFunctions.sortWishlistByReviews(wishlist, database);

        assertNotNull(sorted);
        assertEquals(5, sorted.size());
        assertEquals("Grand Theft Auto V", sorted.get(0).getString("Name")); // Najwięcej recenzji
        assertEquals("Cyberpunk 2077", sorted.get(4).getString("Name")); // Najmniej recenzji
    }

    @Test
    void testSortWishlistByAlphabet() {
        List<JSONObject> sorted = SortingFunctions.sortWishlistByAlphabet(wishlist);

        assertNotNull(sorted);
        assertEquals(5, sorted.size());
        assertEquals("Counter-Strike: Global Offensive", sorted.get(0).getString("Name")); // Pierwsza alfabetycznie
        assertEquals("Team Fortress 2", sorted.get(4).getString("Name")); // Ostatnia alfabetycznie
    }
}
