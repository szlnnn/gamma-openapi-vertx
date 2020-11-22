package server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import joptsimple.internal.Strings;
import service.Provider;
import service.Validator;
import util.ProcessBuilderUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;


public class OpenApiWebServer extends AbstractVerticle {
    public static final String APPLICATION_JSON = "application/json";
    public static final String MESSAGE = "message";
    public static final String WORKSPACE = "workspace";
    public static final String PARSED_PARAMETERS = "parsedParameters";
    HttpServer server;
    private static final String DIRECTORY_OF_WORKSPACES = "E:\\Egyetem\\GammaWrapper\\Workspaces\\";

    @Override
    public void start(Future<Void> future) {
        OpenAPI3RouterFactory.create(this.vertx, "gamma-wrapper.yaml", ar ->
        {

            OpenAPI3RouterFactory routerFactory;
            if (ar.succeeded()) {
                routerFactory = ar.result();
                routerFactory.addHandlerByOperationId("runOperation", routingContext -> {
                    ErrorHandlerPOJO errorHandlerPOJO = null;
                    RequestParameters params = routingContext.get(PARSED_PARAMETERS);
                    String projectName = params.pathParameter("projectName").getString();
                    String workspace = params.pathParameter(WORKSPACE).getString();
                    String filePath = params.pathParameter("filePath").getString();
                    boolean success = false;
                    try {
                        errorHandlerPOJO = getErrorObject(workspace,projectName);
                       if (errorHandlerPOJO.getErrorObject() == null){
                           success = true;
                            ProcessBuilderUtil.runGammaOperations(projectName, workspace, filePath.replace("_", "\\"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (success) {
                        routingContext
                                .response()
                                .setStatusCode(200)
                                .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                                .end();
                    } else {
                        sendErrorResponse(routingContext, errorHandlerPOJO);
                    }
                });


                routerFactory.addHandlerByOperationId("getResult", routingContext -> {
                    ErrorHandlerPOJO errorHandlerPOJO = null;
                    String zipPath = null;
                    RequestParameters params = routingContext.get(PARSED_PARAMETERS);
                    String projectName = params.pathParameter("projectName").getString();
                    String workspace = params.pathParameter(WORKSPACE).getString();
                    JsonObject json = routingContext.getBodyAsJson();
                    JsonArray jsonArray = json.getJsonArray("resultDirs");

                    boolean success = false;
                    try {
                        errorHandlerPOJO = getErrorObject(workspace,projectName);
                        if (errorHandlerPOJO.getErrorObject() == null){
                            success = true;
                            zipPath = Provider.getResultZipFilePath(jsonArray,DIRECTORY_OF_WORKSPACES.concat(workspace),projectName);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    if (success && zipPath != null) {
                        routingContext
                                .response()
                                .setStatusCode(200)
                                .putHeader(HttpHeaders.CONTENT_TYPE, "application/zip")
                                .sendFile(zipPath);
                    } else {
                        sendErrorResponse(routingContext, errorHandlerPOJO);
                    }
                });

                routerFactory.addHandlerByOperationId("addProject", routingContext -> {
                    RequestParameters params = routingContext.get(PARSED_PARAMETERS);
                    String ownerContact = routingContext.request().formAttributes().get("contactEmail");
                    String workspace = params.pathParameter(WORKSPACE).getString();
                    boolean success = true;
                    for (FileUpload f : routingContext.fileUploads()) {
                        try {
                            if (!Validator.checkIfWorkspaceExists(workspace) || f.size() == 0 || Validator.checkIfProjectAlreadyExistsUnderWorkspace(workspace, f.fileName().substring(0, f.fileName().lastIndexOf(".")))) {
                                success = false;
                            } else {
                                Files.move(Paths.get(f.uploadedFileName()), Paths.get(DIRECTORY_OF_WORKSPACES + workspace + "\\" + f.fileName()));
                                String projectName = f.fileName().substring(0, f.fileName().lastIndexOf("."));
                                ProcessBuilderUtil.createEclipseProject(projectName, workspace, ownerContact);
                            }
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    if (success) {
                        routingContext.response().end();
                    } else {
                        JsonObject errorObject = new JsonObject()
                                .put("code", 401)
                                .put(MESSAGE, "Project already exists under this workspace, delete it and resend this request or did not provide a valid workspace");
                        routingContext.response()
                                .setStatusCode(401)
                                .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                                .end(errorObject.encode());
                    }
                });

                routerFactory.addHandlerByOperationId("addWorkspace", routingContext -> {
                    String workspaceUUID = "";
                    try {
                        workspaceUUID = ProcessBuilderUtil.createWorkspaceForUser();
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                    if (Strings.isNullOrEmpty(workspaceUUID)) {
                        routingContext
                                .response()
                                .setStatusCode(500)
                                .end();
                    } else {
                        routingContext
                                .response()
                                .setStatusCode(200)
                                .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                                .end(Json.encode(workspaceUUID));
                    }
                });


                Router router = routerFactory.getRouter();

                router.errorHandler(404, routingContext -> {
                    JsonObject errorObject = new JsonObject()
                            .put("code", 404)
                            .put(MESSAGE,
                                    (routingContext.failure() != null) ?
                                            routingContext.failure().getMessage() :
                                            "Not Found"
                            );
                    routingContext
                            .response()
                            .setStatusCode(404)
                            .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                            .end(errorObject.encode());
                });


                server = vertx.createHttpServer(new HttpServerOptions().setPort(8080).setHost("localhost")); // <5>
                server.requestHandler(router).listen();
                future.complete();
            } else {
                future.fail(ar.cause());
            }


        });
    }

    private ErrorHandlerPOJO getErrorObject(String workspace, String projectName) throws IOException {
        ErrorHandlerPOJO errorHandlerPOJO = new ErrorHandlerPOJO(null,0);
        JsonObject errorObject;
        if (!Validator.checkIfProjectAlreadyExistsUnderWorkspace(workspace, projectName)) {
            errorObject = new JsonObject()
                    .put("code", 401)
                    .put(MESSAGE, "Project" + projectName + " does not exists under this workspace!");
            errorHandlerPOJO.setStatusCode(401);
            errorHandlerPOJO.setErrorObject(errorObject);
        } else if (Validator.checkIfProjectIsUnderLoad(workspace, projectName)) {
            errorObject = new JsonObject()
                    .put("code", 503)
                    .put(MESSAGE, "This project is already under operation!");
            errorHandlerPOJO.setStatusCode(503);
            errorHandlerPOJO.setErrorObject(errorObject);
        }
        return errorHandlerPOJO;
    }

    private void sendErrorResponse(RoutingContext routingContext, ErrorHandlerPOJO errorHandlerPOJO) {
        routingContext.response()
                .setStatusCode(errorHandlerPOJO.getStatusCode())
                .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON)
                .end(errorHandlerPOJO.getErrorObject().encode());
    }

    @Override
    public void stop() {
        this.server.close();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new OpenApiWebServer());


    }


    public class ErrorHandlerPOJO {
        int statusCode;
        JsonObject errorObject;

        public ErrorHandlerPOJO(JsonObject errorObject, int statusCode) {
            this.errorObject = errorObject;
            this.statusCode = statusCode;
        }


        public JsonObject getErrorObject() {
            return errorObject;
        }

        public void setErrorObject(JsonObject errorObject) {
            this.errorObject = errorObject;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }


    }

}
