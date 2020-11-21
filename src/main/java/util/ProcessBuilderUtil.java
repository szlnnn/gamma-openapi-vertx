package util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

public class ProcessBuilderUtil {

    private static final String DIRECTORY_OF_WORKSPACES = "E:\\Egyetem\\GammaWrapper\\Workspaces\\";
    private static final String DIRECTORY_OF_GENERATOR_HEADLESS_ECLIPSE = "E:\\Egyetem\\GammaWrapper\\HeadlessEclipse\\generator\\eclipse.exe";
    private static final String DIRECTORY_OF_GAMMA_HEADLESS_ECLIPSE = "E:\\Egyetem\\GammaWrapper\\HeadlessEclipse\\gammaapi\\eclipse.exe";
    private static final String CONSTANT_ARGUMENTS = " -consoleLog -data ";
    public static final String PROJECT_DESCRIPTOR_JSON = "projectDescriptor.json";
    public static final String UNDER_OPERATION_PROPERTY = "underOperation";


    public static void generateCode(String projectName, String workspace, String filePath) throws IOException {
        String commandToExecute = DIRECTORY_OF_GAMMA_HEADLESS_ECLIPSE + CONSTANT_ARGUMENTS + DIRECTORY_OF_WORKSPACES + workspace + " " + filePath
                + " " + DIRECTORY_OF_WORKSPACES + workspace + "\\" + projectName + "\\" + PROJECT_DESCRIPTOR_JSON;
        updateUnderOperationStatus(projectName, workspace, true);
        Runtime rt = Runtime.getRuntime();
        rt.exec(commandToExecute);

    }

    private static void updateUnderOperationStatus(String projectName, String workspace, Boolean status) throws IOException {
        File jsonFile = new File(DIRECTORY_OF_WORKSPACES + workspace + "\\" + projectName + "\\" + PROJECT_DESCRIPTOR_JSON);
        String jsonString = FileUtils.readFileToString(jsonFile);
        JsonElement jElement = new JsonParser().parse(jsonString);
        JsonObject jObject = jElement.getAsJsonObject();
        jObject.remove(UNDER_OPERATION_PROPERTY);
        jObject.addProperty(UNDER_OPERATION_PROPERTY, status);

        Gson gson = new Gson();
        String resultingJson = gson.toJson(jElement);
        FileUtils.writeStringToFile(jsonFile, resultingJson);

    }

    private static void createProjectJSONFile(String workspace, String projectName, String ownerContact) {
        JSONObject jsonObject = new JSONObject();
        Date today = new Date();
        DateTime dtOrg = new DateTime(today);
        DateTime expirationDate = dtOrg.plusDays(30);

        jsonObject.put("projectName", projectName);
        jsonObject.put("owner", ownerContact);
        jsonObject.put("creationDate", today.getTime());
        jsonObject.put("expirationDate", expirationDate.toDate().getTime());
        jsonObject.put(UNDER_OPERATION_PROPERTY, false);

        try {
            FileWriter file = new FileWriter(DIRECTORY_OF_WORKSPACES + workspace + "\\" + projectName + "\\" + PROJECT_DESCRIPTOR_JSON);
            file.write(jsonObject.toJSONString());
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void createEclipseProject(String projectName, String workspace, String ownerContact) throws IOException, InterruptedException {
        String commandToExecute = DIRECTORY_OF_GENERATOR_HEADLESS_ECLIPSE + CONSTANT_ARGUMENTS + DIRECTORY_OF_WORKSPACES + workspace + " " + projectName;
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(commandToExecute);
        pr.waitFor();
        createProjectJSONFile(workspace, projectName, ownerContact);
    }

    public static String createWorkspaceForUser() throws IOException, InterruptedException {
        String workspace = String.valueOf(UUID.randomUUID());
        String commandToExecute = DIRECTORY_OF_GENERATOR_HEADLESS_ECLIPSE + CONSTANT_ARGUMENTS + DIRECTORY_OF_WORKSPACES + workspace;
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(commandToExecute);
        pr.waitFor();
        return workspace;
    }
}
