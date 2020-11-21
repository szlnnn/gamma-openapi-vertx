package server;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import util.ProcessBuilderUtil;
import util.ZipUtils;

import java.io.IOException;
import java.nio.file.*;



public class OpenApiWebServer extends AbstractVerticle {
    HttpServer server;
    private static final String DIRECTORY_OF_WORKSPACES = "E:\\Egyetem\\GammaWrapper\\Workspaces\\";

    private final static String WORKSPACE = "0fa44362-3467-477d-9164-ecfe5ba2f8fc";

    @Override
    public void start(Future<Void> future) {
        OpenAPI3RouterFactory.create(this.vertx, "gamma-wrapper.yaml", ar ->
        {
            OpenAPI3RouterFactory routerFactory = null;
            if (ar.succeeded()) {
                routerFactory = ar.result();
                routerFactory.addHandlerByOperationId("getGeneratedCode", routingContext -> {
                    RequestParameters params = routingContext.get("parsedParameters");
                   // String projectName = params.pathParameter("projectName").getString();
                    //String filePath = params.pathParameter("filePath").getString();
                    String resultPath = params.pathParameter("resultPath").getString();
                    String projectName = "hu.bme.mit.gamma.tests";

                    //filePath = filePath.replace("_", "\\");
                    String filePath = "E:\\Egyetem\\GammaWrapper\\Workspaces\\0fa44362-3467-477d-9164-ecfe5ba2f8fc\\hu.bme.mit.gamma.tests\\model\\SSM\\System\\Mission.ggen";
                    ZipUtils zipUtils = new ZipUtils();
                    try {
                        ProcessBuilderUtil.generateCode(projectName, WORKSPACE, filePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                   // String resultZipFilePath = zipUtils.getOutputZipFilePath(WORKSPACE, projectName, resultPath);
                    routingContext
                            .response()// (1)
                            .setStatusCode(200)
                            .putHeader(HttpHeaders.CONTENT_TYPE, "application/zip")
                            .end();
                });

                routerFactory.addHandlerByOperationId("addProject", routingContext -> {

                    for (FileUpload f : routingContext.fileUploads()) {
                        try {
                            Path temp = Files.move
                                    (Paths.get(f.uploadedFileName()),Paths.get(DIRECTORY_OF_WORKSPACES +WORKSPACE + "\\" + f.fileName()));
                            String projectName = f.fileName().substring(0,f.fileName().lastIndexOf("."));
                            ProcessBuilderUtil.createEclipseProject(projectName,WORKSPACE,"owner@owner.hu");
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }

                    }

                    routingContext.response().end();
                });

                Router router = routerFactory.getRouter();

                router.errorHandler(404, routingContext -> {
                    JsonObject errorObject = new JsonObject()
                            .put("code", 404)
                            .put("message",
                                    (routingContext.failure() != null) ?
                                            routingContext.failure().getMessage() :
                                            "Not Found"
                            );
                    routingContext
                            .response()
                            .setStatusCode(404)
                            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
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

    @Override
    public void stop() {
        this.server.close();
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new OpenApiWebServer());


    }

}
