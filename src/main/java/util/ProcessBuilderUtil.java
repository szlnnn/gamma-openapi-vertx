package util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import entity.WorkspaceProjectWrapper;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ProcessBuilderUtil {

    private static final String DIRECTORY_OF_WORKSPACES = "E:/Egyetem/GammaWrapper/Workspaces/";
    private static final String DIRECTORY_OF_GENERATOR_HEADLESS_ECLIPSE = "E:/Egyetem/GammaWrapper/HeadlessEclipse/generator/eclipse.exe";
    private static final String DIRECTORY_OF_GAMMA_HEADLESS_ECLIPSE = "E:/Egyetem/GammaWrapper/HeadlessEclipse/gammaapi/eclipse";
    private static final String CONSTANT_ARGUMENTS = " -consoleLog -data ";
    public static final String PROJECT_DESCRIPTOR_JSON = "projectDescriptor.json";
    private static final String ROOT_WRAPPER_JSON = "wrapperList.json";
    public static final String UNDER_OPERATION_PROPERTY = "underOperation";


    public static long runGammaOperations(String projectName, String workspace, String filePath) throws IOException {
        updateUnderOperationStatus(projectName, workspace, true);
        ProcessBuilder pb = new ProcessBuilder(DIRECTORY_OF_GAMMA_HEADLESS_ECLIPSE, "-consoleLog", "-data", DIRECTORY_OF_WORKSPACES + workspace,
                getFullFilePath(filePath, workspace, projectName), DIRECTORY_OF_WORKSPACES + workspace + "/" + projectName + "/" + PROJECT_DESCRIPTOR_JSON);
        pb.redirectErrorStream(true);
        pb.inheritIO();
       return pb.start().pid();
    }

    public static void stopOperation(String projectName, String workspace,int pid) throws IOException {
        String cmd = "taskkill /F /T /PID " + pid;
        Runtime.getRuntime().exec(cmd);
        updateUnderOperationStatus(projectName,workspace, false);
    }

    private static String getFullFilePath(String filePath, String workspace, String projectName) {
        return DIRECTORY_OF_WORKSPACES + workspace + "/" + projectName + "/" + filePath;
    }

    private static void updateUnderOperationStatus(String projectName, String workspace, Boolean status) throws IOException {
        File jsonFile = new File(DIRECTORY_OF_WORKSPACES + workspace + "/" + projectName + "/" + PROJECT_DESCRIPTOR_JSON);
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
            FileWriter file = new FileWriter(DIRECTORY_OF_WORKSPACES + workspace + "/" + projectName + "/" + PROJECT_DESCRIPTOR_JSON);
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
        addWorkspaceProjectWrapperToRootJson(projectName, workspace);
        createProjectJSONFile(workspace, projectName, ownerContact);
    }

    public static String createWorkspaceForUser() throws IOException, InterruptedException {
        String workspace = String.valueOf(UUID.randomUUID());
        String commandToExecute = DIRECTORY_OF_GENERATOR_HEADLESS_ECLIPSE + CONSTANT_ARGUMENTS + DIRECTORY_OF_WORKSPACES + workspace;
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(commandToExecute);
        pr.waitFor();
        addWorkspaceProjectWrapperToRootJson(null, workspace);
        return workspace;
    }

    private static void addWorkspaceProjectWrapperToRootJson(String projectName, String workspace) throws IOException {
        List<WorkspaceProjectWrapper> yourList = getWrapperListFromJson();
        if (yourList == null) {
            yourList = new ArrayList<>();
        }
        yourList.add(new WorkspaceProjectWrapper(workspace, projectName));
        try {
            FileWriter writer = new FileWriter(DIRECTORY_OF_WORKSPACES + ROOT_WRAPPER_JSON);
            new Gson().toJson(yourList, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<WorkspaceProjectWrapper> getWrapperListFromJson() throws IOException {
        File jsonFile = new File(DIRECTORY_OF_WORKSPACES + ROOT_WRAPPER_JSON);
        if (!jsonFile.exists()) {
            Files.createFile(Paths.get(jsonFile.getPath()));
        }
        String jsonString = FileUtils.readFileToString(jsonFile);
        JsonElement jElement = new JsonParser().parse(jsonString);
        Type listType = new TypeToken<List<WorkspaceProjectWrapper>>() {
        }.getType();

        return new Gson().fromJson(jElement, listType);
    }
}
