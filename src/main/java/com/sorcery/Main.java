
package com.sorcery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {

    private static final Gson gson =
            new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        try {
            generateInitialResults();
            startWebServer();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void generateInitialResults() throws Exception {
        SorceryEngine engine = new SorceryEngine();

        engine.loadData(Paths.get("data"));

        List<Model.StudentResult> results = engine.process();

        Path publicDir = Paths.get("public");

        if (!Files.exists(publicDir)) {
            Files.createDirectories(publicDir);
        }

        try (FileWriter writer =
                     new FileWriter(
                             publicDir.resolve("results.json").toFile()
                     )) {

            gson.toJson(results, writer);
        }

        System.out.println("=== RESULTADOS SCHOOL OF SORCERY ===");
        System.out.println("Total procesados: " + results.size());

        long acceptedCount = results.stream()
                .filter(Model.StudentResult::isAccepted)
                .count();

        System.out.println("Admitidos: " + acceptedCount);
        System.out.println(
                "Rechazados: " + (results.size() - acceptedCount)
        );

        System.out.println(
                "¡Fichero results.json generado con éxito en public/!"
        );
    }

    private static void startWebServer() throws IOException {

        HttpServer server = HttpServer.create(
                new InetSocketAddress(8080),
                0
        );

        /*
         * Endpoint principal de la aplicación web.
         */
        server.createContext("/", exchange -> {
            serveStaticFile(exchange);
        });

        /*
         * Endpoint para procesar un applications.json enviado
         * desde el frontend.
         */
        server.createContext("/api/process", exchange -> {

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(
                        exchange,
                        405,
                        "{\"error\":\"Method not allowed\"}"
                );
                return;
            }

            try {
                String requestBody =
                        new String(
                                exchange.getRequestBody().readAllBytes(),
                                StandardCharsets.UTF_8
                        );

                Model.ApplicationsContainer container =
                        gson.fromJson(
                                requestBody,
                                Model.ApplicationsContainer.class
                        );

                if (container == null
                        || container.getApplications() == null) {

                    sendJsonResponse(
                            exchange,
                            400,
                            "{\"error\":\"Invalid applications JSON\"}"
                    );

                    return;
                }

                SorceryEngine engine = new SorceryEngine();

                /*
                 * Las reglas siguen cargándose desde el fichero
                 * council-rules.json del proyecto.
                 */
                engine.loadRules(Paths.get("data"));

                /*
                 * Las aplicaciones son las que acaba de subir
                 * el usuario desde la UI.
                 */
                engine.setApplications(
                        container.getApplications()
                );

                List<Model.StudentResult> results =
                        engine.process();

                /*
                * Guarda los últimos resultados procesados para que results.json siempre refleje el archivo actual de la aplicación.
                */
                Path publicDir = Paths.get("public");

                if (!Files.exists(publicDir)) {
                    Files.createDirectories(publicDir);
                }

                try (FileWriter writer =
                            new FileWriter(
                                    publicDir.resolve("results.json").toFile()
                            )) {

                    gson.toJson(results, writer);
                }

                String responseJson =
                        gson.toJson(results);

                sendJsonResponse(
                        exchange,
                        200,
                        responseJson
                );

            } catch (Exception e) {

                e.printStackTrace();

                String errorJson =
                        "{\"error\":\"Unable to process applications\"}";

                sendJsonResponse(
                        exchange,
                        500,
                        errorJson
                );
            }
        });

        server.setExecutor(null);

        server.start();

        System.out.println();
        System.out.println(
                "Web disponible en: http://localhost:8080"
        );
        System.out.println(
                "Pulsa Ctrl+C para detener el servidor."
        );
    }

    private static void serveStaticFile(
            HttpExchange exchange) throws IOException {

        String requestedPath =
                exchange.getRequestURI().getPath();

        if ("/".equals(requestedPath)) {
            requestedPath = "/index.html";
        }

        Path publicDirectory =
                Paths.get("public")
                        .toAbsolutePath()
                        .normalize();

        Path requestedFile =
                publicDirectory
                        .resolve(requestedPath.substring(1))
                        .normalize();

        /*
         * Seguridad básica:
         * impedimos salir fuera de la carpeta public.
         */
        if (!requestedFile.startsWith(publicDirectory)) {

            sendTextResponse(
                    exchange,
                    403,
                    "Forbidden",
                    "text/plain"
            );

            return;
        }

        File file =
                requestedFile.toFile();

        if (!file.exists() || file.isDirectory()) {

            sendTextResponse(
                    exchange,
                    404,
                    "Not found",
                    "text/plain"
            );

            return;
        }

        String contentType =
                getContentType(file.getName());

        byte[] fileBytes;

        try (FileInputStream input =
                     new FileInputStream(file)) {

            fileBytes = input.readAllBytes();
        }

        exchange.getResponseHeaders()
                .set("Content-Type", contentType);

        exchange.sendResponseHeaders(
                200,
                fileBytes.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(fileBytes);
        }
    }

    private static String getContentType(
            String fileName) {

        if (fileName.endsWith(".html")) {
            return "text/html; charset=UTF-8";
        }

        if (fileName.endsWith(".css")) {
            return "text/css; charset=UTF-8";
        }

        if (fileName.endsWith(".js")) {
            return "application/javascript; charset=UTF-8";
        }

        if (fileName.endsWith(".json")) {
            return "application/json; charset=UTF-8";
        }

        if (fileName.endsWith(".png")) {
            return "image/png";
        }

        if (fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }

        if (fileName.endsWith(".svg")) {
            return "image/svg+xml";
        }

        return "application/octet-stream";
    }

    private static void sendJsonResponse(
            HttpExchange exchange,
            int statusCode,
            String json) throws IOException {

        byte[] response =
                json.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        "application/json; charset=UTF-8"
                );

        exchange.sendResponseHeaders(
                statusCode,
                response.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(response);
        }
    }

    private static void sendTextResponse(
            HttpExchange exchange,
            int statusCode,
            String text,
            String contentType) throws IOException {

        byte[] response =
                text.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        contentType + "; charset=UTF-8"
                );

        exchange.sendResponseHeaders(
                statusCode,
                response.length
        );

        try (OutputStream output =
                     exchange.getResponseBody()) {

            output.write(response);
        }
    }
}

