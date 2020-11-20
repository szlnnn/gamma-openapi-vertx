package util;

import java.io.IOException;

public class ProcessBuilderUtil {

    public void generateCode(String projectName, String workspace, String filePath) throws IOException, InterruptedException {
        String directoryOfHeadlessEclipse = "C:\\Eclipse-2020-09-workspace\\eclipse\\eclipse.exe";
        String commandToExecute = directoryOfHeadlessEclipse + " -consoleLog -data " + workspace + " " + filePath + " " + projectName;

        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(commandToExecute);
        pr.waitFor();
    }

    public void createEclipseProject(String projectName, String workspace) throws IOException, InterruptedException {
        String directoryOfHeadlessEclipse = "C:\\Eclipse-2020-09-workspace\\generator\\eclipse.exe";
        String commandToExecute = directoryOfHeadlessEclipse + " -consoleLog -data " + workspace + " "+ projectName;
        Runtime rt = Runtime.getRuntime();
        Process pr = rt.exec(commandToExecute);
        pr.waitFor();

    }
}
