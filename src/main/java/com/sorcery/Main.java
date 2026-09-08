package com.sorcery;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            SorceryEngine engine = new SorceryEngine();
            engine.loadData(Paths.get("data"));
            List<Model.StudentResult> results = engine.process();

            // Exportar resultados automáticamente a public/results.json para la UI
            Path publicDir = Paths.get("public");
            if (!Files.exists(publicDir)) {
                Files.createDirectories(publicDir);
            }
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            try (FileWriter writer = new FileWriter(publicDir.resolve("results.json").toFile())) {
                gson.toJson(results, writer);
            }

            System.out.println("=== RESULTADOS SCHOOL OF SORCERY ===");
            long acceptedCount = results.stream().filter(Model.StudentResult::isAccepted).count();
            System.out.println("Total procesados: " + results.size());
            System.out.println("Admitidos: " + acceptedCount);
            System.out.println("Rechazados: " + (results.size() - acceptedCount));
            System.out.println("¡Fichero results.json generado con éxito en la carpeta public/!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}