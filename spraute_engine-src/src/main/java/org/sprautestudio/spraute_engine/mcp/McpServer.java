package org.sprautestudio.spraute_engine.mcp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.sprautestudio.spraute_engine.SprauteEngine;

public final class McpServer {
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static ServerSocket socket;
    private static Thread thread;
    private static int port;
    private static volatile String lastClient = "";
    private static volatile long lastCall;
    private static final String SESSION = UUID.randomUUID().toString();

    private McpServer() {
    }

    public static boolean running() {
        return socket != null && !socket.isClosed();
    }

    public static int port() {
        return port;
    }

    public static String lastClient() {
        return lastClient;
    }

    public static long lastCall() {
        return lastCall;
    }

    public static synchronized boolean start(int p) {
        stop();
        return false;
    }

    public static synchronized void stop() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException var1) {
            }
        }

        socket = null;
    }

    private static void serve(Socket c) {
        try {
            Socket e = c;

            label141: {
                label142: {
                    label152: {
                        label143: {
                            try {
                                c.setSoTimeout(120000);
                                InputStream in = new BufferedInputStream(c.getInputStream());
                                OutputStream out = c.getOutputStream();
                                String requestLine = readLine(in);
                                if (requestLine != null && !requestLine.isEmpty()) {
                                    String[] rl = requestLine.split(" ");
                                    String method = rl[0];
                                    String path = rl.length > 1 ? rl[1] : "/";
                                    Map<String, String> headers = new HashMap<>();

                                    String h;
                                    while ((h = readLine(in)) != null && !h.isEmpty()) {
                                        int i = h.indexOf(58);
                                        if (i > 0) {
                                            headers.put(h.substring(0, i).trim().toLowerCase(Locale.ROOT), h.substring(i + 1).trim());
                                        }
                                    }

                                    byte[] body = new byte[0];
                                    String len = headers.get("content-length");
                                    if (len != null) {
                                        int n = Integer.parseInt(len.trim());
                                        body = in.readNBytes(n);
                                    }

                                    if (!path.startsWith("/mcp") && !path.equals("/")) {
                                        respond(out, 404, "text/plain", "Spraute MCP: use /mcp", null);
                                        break label152;
                                    }

                                    String origin = headers.getOrDefault("origin", "");
                                    if (!origin.isEmpty() && !origin.matches("https?://(127\\.0\\.0\\.1|localhost)(:\\d+)?")) {
                                        respond(out, 403, "text/plain", "Spraute MCP: forbidden origin", null);
                                        break label142;
                                    }

                                    if (method.equals("POST") && !headers.getOrDefault("content-type", "").contains("application/json")) {
                                        respond(out, 400, "text/plain", "Spraute MCP: JSON expected", null);
                                        break label141;
                                    }

                                    switch (method) {
                                        case "POST":
                                            handlePost(out, new String(body, StandardCharsets.UTF_8), headers);
                                            break label143;
                                        case "GET":
                                            respond(out, 405, "text/plain", "Spraute MCP: no event stream, use POST", null);
                                            break label143;
                                        case "DELETE":
                                            respond(out, 200, "text/plain", "", null);
                                            break label143;
                                        case "OPTIONS":
                                            respond(out, 204, "text/plain", "", null);
                                            break label143;
                                        default:
                                            respond(out, 405, "text/plain", "", null);
                                            break label143;
                                    }
                                }
                            } catch (Throwable var16) {
                                if (c != null) {
                                    try {
                                        e.close();
                                    } catch (Throwable var15) {
                                        var16.addSuppressed(var15);
                                    }
                                }

                                throw var16;
                            }

                            if (c != null) {
                                c.close();
                            }

                            return;
                        }

                        if (c != null) {
                            c.close();
                        }

                        return;
                    }

                    if (c != null) {
                        c.close();
                    }

                    return;
                }

                if (c != null) {
                    c.close();
                }

                return;
            }

            if (c != null) {
                c.close();
            }
        } catch (Exception var17) {
            SprauteEngine.LOGGER.debug("Spraute MCP connection", (Throwable)var17);
        }
    }

    private static String readLine(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();

        int ch;
        while ((ch = in.read()) != -1 && ch != 10) {
            if (ch != 13) {
                b.write(ch);
            }
        }

        return ch == -1 && b.size() == 0 ? null : b.toString(StandardCharsets.UTF_8);
    }

    private static void respond(OutputStream out, int code, String type, String body, Map<String, String> extra) throws IOException {
        byte[] data = body.getBytes(StandardCharsets.UTF_8);
        StringBuilder sb = new StringBuilder();

        sb.append("HTTP/1.1 ").append(code).append(' ').append(switch (code) {
            case 200 -> "OK";
            case 202 -> "Accepted";
            case 204 -> "No Content";
            case 400 -> "Bad Request";
            case 403 -> "Forbidden";
            case 404 -> "Not Found";
            case 405 -> "Method Not Allowed";
            default -> "Error";
        }).append("\r\n");
        sb.append("Content-Type: ").append(type).append("; charset=utf-8\r\n");
        sb.append("Content-Length: ").append(data.length).append("\r\n");
        sb.append("Access-Control-Allow-Origin: http://127.0.0.1\r\n");
        sb.append("Connection: close\r\n");
        if (extra != null) {
            extra.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\r\n"));
        }

        sb.append("\r\n");
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.flush();
    }

    private static void handlePost(OutputStream out, String body, Map<String, String> headers) throws IOException {
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(body);
        } catch (Exception var9) {
            respond(out, 400, "application/json", GSON.toJson((JsonElement)error(JsonNull.INSTANCE, -32700, "Parse error")), null);
            return;
        }

        Map<String, String> extra = Map.of("Mcp-Session-Id", SESSION);
        if (parsed.isJsonArray()) {
            JsonArray answers = new JsonArray();

            for (JsonElement e : parsed.getAsJsonArray()) {
                JsonObject a = handle(e.getAsJsonObject(), headers);
                if (a != null) {
                    answers.add(a);
                }
            }

            if (answers.isEmpty()) {
                respond(out, 202, "application/json", "", extra);
            } else {
                respond(out, 200, "application/json", GSON.toJson((JsonElement)answers), extra);
            }
        } else {
            JsonObject a = handle(parsed.getAsJsonObject(), headers);
            if (a == null) {
                respond(out, 202, "application/json", "", extra);
            } else {
                respond(out, 200, "application/json", GSON.toJson((JsonElement)a), extra);
            }
        }
    }

    private static JsonObject handle(JsonObject msg, Map<String, String> headers) {
        JsonElement id = msg.get("id");
        if (msg.has("method")) {
            msg.get("method").getAsString();
        } else {
            String var10000 = "";
        }

        if (msg.has("params") && msg.get("params").isJsonObject()) {
            msg.getAsJsonObject("params");
        } else {
            new JsonObject();
        }

        if (id != null && !id.isJsonNull()) {
            lastCall = System.currentTimeMillis();
            return null;
        } else {
            return null;
        }
    }

    private static JsonObject result(JsonElement id, JsonObject r) {
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        o.add("id", id);
        o.add("result", r);
        return o;
    }

    private static JsonObject error(JsonElement id, int code, String message) {
        JsonObject o = new JsonObject();
        o.addProperty("jsonrpc", "2.0");
        o.add("id", id);
        JsonObject e = new JsonObject();
        e.addProperty("code", code);
        e.addProperty("message", message);
        o.add("error", e);
        return o;
    }
}
