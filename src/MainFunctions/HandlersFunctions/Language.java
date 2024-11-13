package MainFunctions.HandlersFunctions;

import MainFunctions.DataManageFunctions.Database;
import MainFunctions.DataManageFunctions.FindExactGame;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Language {

    private Message message = new Message();
    public void showAvailableLanguages(CallbackQuery call) {
        String gameId = call.getData().split("_", 2)[1];
        long chatId = call.getMessage().getChatId();

        List<JSONObject> games = FindExactGame.findGameByExactId(gameId, Database.readDatabase());
        if (!games.isEmpty()) {
            JSONObject game = games.get(0);


            List<String> languagesSub = game.optJSONArray("LanguagesSub") != null
                    ? game.optJSONArray("LanguagesSub").toList().stream().map(Object::toString).collect(Collectors.toList())
                    : Collections.emptyList();

            List<String> languagesAudio = game.optJSONArray("LanguagesAudio") != null
                    ? game.optJSONArray("LanguagesAudio").toList().stream().map(Object::toString).collect(Collectors.toList())
                    : Collections.emptyList();

            String languagesText = "Available Languages for " + game.optString("Name") + ":\n\n" +
                    "Subtitles:\n" +
                    (languagesSub.isEmpty() ? "No subtitles available." : String.join("\n", languagesSub)) + "\n\n" +
                    "Audio:\n" +
                    (languagesAudio.isEmpty() ? "No audio available." : String.join("\n", languagesAudio));


            message.sendMessage(chatId, languagesText);
        } else {

            message.sendMessage(chatId, "Game details not found.");
        }
    }
}
