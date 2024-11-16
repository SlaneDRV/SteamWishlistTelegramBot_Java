package SteamAPI;

import MainFunctions.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;

public class SteamApiClient {

    private static final String API_KEY = Config.STEAM_KEY;
    private static final String STEAM_API_URL = "http://store.steampowered.com/api/appdetails";
    public static CloseableHttpClient httpClient;

    public SteamApiClient() {
        httpClient = createHttpClient();
    }

    public CloseableHttpClient createHttpClient() {
        RequestConfig globalConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(globalConfig)
                .build();
    }

    public JsonNode fetchSteamGameDetails(int appid, String Region) {
        String url = String.format("%s?appids=%d&cc=%s&key=%s", STEAM_API_URL, appid, Region, API_KEY);
        try {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            String jsonString = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonString);
            JsonNode appNode = rootNode.get(String.valueOf(appid));

            if (appNode == null || !appNode.path("success").asBoolean()) {
                System.out.println("Failed or missing data for appid " + appid);
                return null;
            }

            JsonNode dataNode = appNode.path("data");
            if (!dataNode.isObject()) {
                System.out.println("Unexpected 'data' format for appid " + appid);
                return null;
            }

            return dataNode;

        } catch (Exception e) {
            System.out.println("Error fetching details for appid " + appid + ": " + e.getMessage());
            return null;
        }
    }

    public void close() throws Exception {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}
