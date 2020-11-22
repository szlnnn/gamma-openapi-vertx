package entity;

public class WorkspaceProjectWrapper {
    String workspace;
    String projectName;

    public WorkspaceProjectWrapper(String workspace, String projectName) {
        this.workspace = workspace;
        this.projectName = projectName;
    }

    public String getWorkspace() {
        return workspace;
    }

    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }



}
