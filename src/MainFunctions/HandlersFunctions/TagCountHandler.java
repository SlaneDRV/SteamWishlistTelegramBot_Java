package MainFunctions.HandlersFunctions;

import MainFunctions.DataManageFunctions.DatabaseFunctions;
import MainFunctions.DataManageFunctions.FindExactGameFunctions;
import MainFunctions.DataManageFunctions.WishlistFunctions;
import MainFunctions.Handlers;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.general.DefaultPieDataset;
import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TagCountHandler {

    private Handlers handler = new Handlers();

    private final WishlistFunctions Wishlist = new WishlistFunctions();
    public void handleTagCount(long chatId) {
        List<JSONObject> wishlist = Wishlist.readWishlist(chatId);
        Map<String, Integer> tagCounter = new HashMap<>();
        Map<String, List<String>> tagToGames = new HashMap<>();

        List<String> gameIds = wishlist.stream()
                .map(game -> game.optString("ID"))
                .collect(Collectors.toList());

        for (String gameId : gameIds) {
            List<JSONObject> games = FindExactGameFunctions.findGameByExactId(gameId, DatabaseFunctions.readDatabase());
            if (!games.isEmpty()) {
                JSONObject game = games.get(0);
                JSONArray tags = game.optJSONArray("TopTags");

                if (tags != null) {
                    for (int i = 0; i < tags.length(); i++) {
                        String tag = tags.optString(i);
                        tagCounter.put(tag, tagCounter.getOrDefault(tag, 0) + 1);

                        if (!tagToGames.containsKey(tag)) {
                            tagToGames.put(tag, new ArrayList<>());
                        }
                        tagToGames.get(tag).add(game.optString("Name"));
                    }
                }
            }
        }

        List<Map.Entry<String, Integer>> topTags = tagCounter.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Collections.reverseOrder()))
                .limit(10)
                .collect(Collectors.toList());

        ByteArrayOutputStream outputStream = createTagDistributionChart(topTags);

        StringBuilder tagListText = new StringBuilder("Top 10 Tags in Wishlist Games\n\n");
        for (Map.Entry<String, Integer> entry : topTags) {
            String tag = entry.getKey();
            List<String> games = tagToGames.get(tag);
            String gamesList = String.join(", ", games);
            tagListText.append(String.format("%s: %s\n\n", tag, gamesList));
        }

        sendTagChart(chatId, outputStream);
        sendTagListText(chatId, tagListText.toString());
    }

    // Creates a pie chart based on the top tags and their counts.
    private ByteArrayOutputStream createTagDistributionChart(List<Map.Entry<String, Integer>> topTags) {
        DefaultPieDataset dataset = new DefaultPieDataset();
        for (Map.Entry<String, Integer> entry : topTags) {
            dataset.setValue(entry.getKey(), entry.getValue());
        }

        JFreeChart pieChart = ChartFactory.createPieChart("Top 10 Tags in Wishlist Games", dataset, true, true, false);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            ChartUtils.writeChartAsPNG(outputStream, pieChart, 800, 600);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outputStream;
    }

    // Sends a pie chart image to the specified chat

    private void sendTagChart(long chatId, ByteArrayOutputStream outputStream) {
        SendPhoto photoMessage = new SendPhoto();
        photoMessage.setChatId(String.valueOf(chatId));
        photoMessage.setPhoto(new InputFile(new ByteArrayInputStream(outputStream.toByteArray()), "tags_chart.png"));

        try {
            handler.execute(photoMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    //Sends the text representation of the top tags and associated games to the chat.

    private void sendTagListText(long chatId, String tagListText) {
        final int MAX_MESSAGE_LENGTH = 4096;
        for (int i = 0; i < tagListText.length(); i += MAX_MESSAGE_LENGTH) {
            String msg = tagListText.substring(i, Math.min(i + MAX_MESSAGE_LENGTH, tagListText.length()));
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(msg);
            try {
                handler.execute(message);
            } catch (TelegramApiException e) {
                e.printStackTrace();
            }
        }
    }
}
