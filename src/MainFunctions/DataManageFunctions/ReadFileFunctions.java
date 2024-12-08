package MainFunctions.DataManageFunctions;

import org.json.JSONArray;
import org.json.JSONObject;
import org.yaml.snakeyaml.Yaml;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ReadFileFunctions {

    public static List<JSONObject> readTxtFile(byte[] fileContent) {
        List<JSONObject> importedData = new ArrayList<>();
        String content = new String(fileContent);
        String[] lines = content.strip().split("\n");

        for (String line : lines) {

            int lastDashIndex = line.lastIndexOf(" - ");
            if (lastDashIndex != -1) {
                String idAndName = line.substring(0, lastDashIndex);
                String price = line.substring(lastDashIndex + 3);

                int firstColonIndex = idAndName.indexOf(':');
                if (firstColonIndex != -1) {
                    String gameId = idAndName.substring(0, firstColonIndex).trim();
                    String gameName = idAndName.substring(firstColonIndex + 1).trim();
                    JSONObject gameInfo = new JSONObject();
                    gameInfo.put("ID", Integer.parseInt(gameId));
                    gameInfo.put("Name", gameName);
                    gameInfo.put("Price", price.strip());
                    importedData.add(gameInfo);
                } else {
                    System.out.println("Error parsing line (ID and Name): " + line);
                }
            } else {
                System.out.println("Error parsing line (Price): " + line);
            }
        }
        System.out.println(" Successfuly readig of TXT FILE !!!");
        return importedData;
    }

    public static List<JSONObject> readYamlFile(byte[] fileContent) {
        Yaml yaml = new Yaml();
        List<JSONObject> importedData = new ArrayList<>();

        List<Map<String, Object>> yamlData = yaml.load(new ByteArrayInputStream(fileContent));
        if (yamlData != null) {
            for (Map<String, Object> gameData : yamlData) {

                importedData.add(new JSONObject(gameData));
            }
        }
        System.out.println(" Successfuly readig of YAML FILE !!!");
        return importedData;
    }

    public static List<JSONObject> readJsonFile(byte[] downloadedFile) {
        List<JSONObject> gamesList = new ArrayList<>();

        try {

            String jsonContent = new String(downloadedFile);
            JSONArray jsonArray = new JSONArray(jsonContent);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject game = jsonArray.getJSONObject(i);
                gamesList.add(game);
            }

        } catch (Exception e) {
            System.out.println("Error reading JSON file: " + e.getMessage());
            e.printStackTrace();
        }

        return gamesList;
    }

}
