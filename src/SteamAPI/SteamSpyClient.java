package SteamAPI;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class SteamSpyClient {

    private static final String STEAMSPY_API_URL = "https://steamspy.com/api.php";
    private CloseableHttpClient httpClient;

    public SteamSpyClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public Map<String, Object> fetchSteamSpyData(int appid) {
        String url = String.format("%s?request=appdetails&appid=%d", STEAMSPY_API_URL, appid);
        String jsonString = "";
        try {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(jsonString, new TypeReference<Map<String, Object>>() {});

            return data;

        } catch (Exception e) {
            System.out.println("Error fetching SteamSpy data for appid " + appid + ": " + e.getMessage());
            System.out.println("Response JSON: " + jsonString);
            return new HashMap<>();
        }
    }
}
