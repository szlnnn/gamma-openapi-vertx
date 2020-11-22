package service;

import io.vertx.core.json.JsonArray;
import org.apache.commons.io.FileUtils;
import util.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Provider {

    private static final String RESULT_DIR_NAME = "result";

    /**
     * @param requiredDirectories Every artifact path we want to retrieve, if contains {.} we will return the whole project
     *                            example: { resultDirs: ["src-gen", "model/Crossroad.gcd"]}
     * @param workspace Workspace which contains the project
     *                  example: full path of workspace: C:\wf\01234-2314
     * @param projectName Name of the project where we will zip the results
     *                    example: gamma.test.project
     * @return The path to the result.zip file
     */
    public static String getResultZipFilePath(JsonArray requiredDirectories, String workspace, String projectName) throws IOException {
        List<String> resultDirs = convertJsonArrayToStringArray(requiredDirectories);
        String pathPrefix = workspace + "\\" + projectName + "\\";
        deletePreviousResultZip(pathPrefix);
        if (resultDirs.contains(".")) {
            return ZipUtils.getOutputZipFilePath(workspace, projectName, "");
        }

        try {
            File result = new File(pathPrefix + RESULT_DIR_NAME);
            if (result.exists()) {
                deleteDirectory(result);
            }
            Files.createDirectories(Paths.get(pathPrefix + RESULT_DIR_NAME));
            resultDirs.stream()
                    .filter(relativePath -> new File(pathPrefix + relativePath).exists())
                    .forEach(relativePath -> {
                        if (new File(pathPrefix + relativePath) .isDirectory()) {
                            try {
                                copyDirectory(pathPrefix + relativePath, result.getPath() + "\\" + relativePath);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        } else {
                            try {
                                FileUtils.copyFile(new File(pathPrefix  + relativePath), new File(result.getPath() + "\\" +relativePath));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }


                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
        return ZipUtils.getOutputZipFilePath(workspace, projectName, "\\" + RESULT_DIR_NAME);
    }

    private static List<String> convertJsonArrayToStringArray(JsonArray requiredDirectories) {
        ArrayList<String> paths = new ArrayList<>();
        if (requiredDirectories != null) {
            for (int i = 0; i < requiredDirectories.size(); i++) {
                paths.add(requiredDirectories.getString(i));
            }
        }
        return paths;
    }


    private static boolean deleteDirectory(File directoryToBeDeleted) throws IOException {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        Files.delete(Paths.get(directoryToBeDeleted.getPath()));
        return true;
    }

    private static void copyDirectory(String sourceDirectoryLocation, String destinationDirectoryLocation) throws IOException {
        File sourceDirectory = new File(sourceDirectoryLocation);
        File destinationDirectory = new File(destinationDirectoryLocation);
        FileUtils.copyDirectory(sourceDirectory, destinationDirectory);
    }

    private static void deletePreviousResultZip(String pathPrefix) throws IOException {
        File zip = new File(pathPrefix + RESULT_DIR_NAME + ".zip");
        if (zip.exists()) {
            Files.delete(Paths.get(zip.getPath()));
        }
    }
}
