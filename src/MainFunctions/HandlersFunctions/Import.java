package MainFunctions.HandlersFunctions;

import MainFunctions.DataManager;
import MainFunctions.Handlers;
import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class Import {

    private Handlers handler = new Handlers();
    private Message message = new Message();

    public void processImportFile(String fileId, String fileName, long chatId) {
        try {

            GetFile getFileRequest = new GetFile(fileId);
            org.telegram.telegrambots.meta.api.objects.File file = handler.execute(getFileRequest);
            String filePath = file.getFilePath();

            String fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
            byte[] downloadedFile = downloadFileAsByteArray(filePath);

            List<JSONObject> importedData;
            switch (fileExtension) {
                case "txt":
                    importedData = DataManager.readTxtFile(downloadedFile);
                    break;
                case "yaml":
                case "yml":
                    importedData = DataManager.readYamlFile(downloadedFile);
                    break;
                case "json":
                    importedData = DataManager.readJsonFile(downloadedFile);
                    break;
                default:
                    message.sendMessage(chatId, "Unsupported file format. Please upload a txt or yaml file.");
                    return;
            }

            DataManager.mergeWishlists(chatId, importedData);
            message.sendMessage(chatId, "Wishlist imported and updated successfully.");

        } catch (TelegramApiException | IOException e) {
            message.sendMessage(chatId, "An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private byte[] downloadFileAsByteArray(String filePath) throws TelegramApiException, IOException {

        File downloadedFile = handler.downloadFile(filePath);

        try (FileInputStream fileInputStream = new FileInputStream(downloadedFile);
             ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }

    public void importWishlist(long chatId) {
        SendMessage msg = new SendMessage();
        msg.setChatId(String.valueOf(chatId));
        msg.setText("Please send the wishlist file.");
        try {
            handler.execute(msg);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
