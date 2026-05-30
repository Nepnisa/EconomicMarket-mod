package com.market.api;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.*;

public class StaticFileHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.startsWith("/web/")) {
            String file = path.substring(5);
            if (file.isEmpty()) file = "index.html";
            InputStream in = getClass().getClassLoader().getResourceAsStream("web/" + file);
            if (in != null) {
                byte[] data = in.readAllBytes(); in.close();
                exchange.getResponseHeaders().set("Content-Type", getContentType(file));
                exchange.sendResponseHeaders(200, data.length);
                OutputStream os = exchange.getResponseBody();
                os.write(data); os.close();
                return;
            }
            Path cfgFile = FabricLoader.getInstance().getConfigDir().resolve("economicmarket-web").resolve(file);
            if (Files.exists(cfgFile)) {
                byte[] data = Files.readAllBytes(cfgFile);
                exchange.getResponseHeaders().set("Content-Type", getContentType(file));
                exchange.sendResponseHeaders(200, data.length);
                OutputStream os = exchange.getResponseBody();
                os.write(data); os.close();
                return;
            }
        }
        exchange.sendResponseHeaders(404, -1);
        exchange.close();
    }

    private String getContentType(String file) {
        if (file.endsWith(".html")) return "text/html; charset=utf-8";
        if (file.endsWith(".css")) return "text/css";
        if (file.endsWith(".js")) return "application/javascript";
        return "application/octet-stream";
    }
}