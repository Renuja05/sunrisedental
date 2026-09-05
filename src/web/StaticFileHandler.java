package web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;


public class StaticFileHandler implements HttpHandler {

    private final File webRoot;

    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "html", "text/html; charset=utf-8",
            "css", "text/css; charset=utf-8",
            "js", "application/javascript; charset=utf-8",
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "svg", "image/svg+xml",
            "ico", "image/x-icon"
    );

    public StaticFileHandler(File webRoot) {
        this.webRoot = webRoot;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/")) path = "/login.html";

        // Prevent directory traversal (e.g. "/../../etc/passwd")
        File file = new File(webRoot, path).getCanonicalFile();
        if (!file.getPath().startsWith(webRoot.getCanonicalPath()) || !file.isFile()) {
            byte[] notFound = "404 Not Found".getBytes();
            exchange.sendResponseHeaders(404, notFound.length);
            exchange.getResponseBody().write(notFound);
            exchange.close();
            return;
        }

        String extension = "";
        int dot = file.getName().lastIndexOf('.');
        if (dot >= 0) extension = file.getName().substring(dot + 1).toLowerCase();
        String contentType = CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");

        byte[] bytes = Files.readAllBytes(file.toPath());
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
