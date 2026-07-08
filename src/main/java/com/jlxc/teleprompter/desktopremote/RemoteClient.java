package com.jlxc.teleprompter.desktopremote;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class RemoteClient {
    private final String ip;
    private final int port;
    private final HttpClient http;

    RemoteClient(String ip, int port) {
        this.ip = ip;
        this.port = port;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(2500))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    String endpoint() {
        return ip + ":" + port;
    }

    PingResult ping() {
        try {
            HttpResponse<String> r = request("GET", "/api/ping", null, "text/plain; charset=utf-8", 2500);
            if (r.statusCode() != 200) return PingResult.fail("HTTP " + r.statusCode());
            String body = r.body() == null ? "" : r.body();
            boolean ok = JsonUtil.bool(body, "ok", false);
            if (!ok) return PingResult.fail("提词端返回 ok=false");
            PingResult out = new PingResult();
            out.ok = true;
            out.version = JsonUtil.integer(body, "version", 1);
            out.scriptUpload = JsonUtil.bool(body, "scriptUpload", false);
            out.scriptManage = JsonUtil.bool(body, "scriptManage", false);
            out.remotePrompt = JsonUtil.bool(body, "remotePrompt", false);
            out.http = JsonUtil.bool(body, "http", true);
            out.udp = JsonUtil.bool(body, "udp", true);
            out.message = "已连接：" + endpoint();
            return out;
        } catch (Exception e) {
            return PingResult.fail(humanError(e));
        }
    }

    boolean scroll(int dy, boolean useUdp, boolean httpFallback) throws IOException, InterruptedException {
        if (useUdp) {
            try {
                sendUdp("SCROLL " + dy);
                return true;
            } catch (IOException udpError) {
                if (!httpFallback) throw udpError;
            }
        }
        if (!httpFallback && useUdp) return true;
        scrollHttp(dy);
        return true;
    }

    void pause(boolean paused, boolean useUdp, boolean httpFallback) throws IOException, InterruptedException {
        if (useUdp) {
            try {
                sendUdp("PAUSE " + paused);
                return;
            } catch (IOException e) {
                if (!httpFallback) throw e;
            }
        }
        HttpResponse<String> r = request("POST", "/api/remote/pause?paused=" + paused, new byte[0], "text/plain; charset=utf-8", 2500);
        ensureOk(r);
    }

    void top(boolean useUdp, boolean httpFallback) throws IOException, InterruptedException {
        if (useUdp) {
            try {
                sendUdp("TOP");
                return;
            } catch (IOException e) {
                if (!httpFallback) throw e;
            }
        }
        HttpResponse<String> r = request("POST", "/api/remote/top", new byte[0], "text/plain; charset=utf-8", 2500);
        ensureOk(r);
    }

    UploadResult uploadScript(String title, String content) {
        try {
            return uploadScriptJson(title, content);
        } catch (Exception jsonError) {
            try {
                UploadResult fallback = uploadScriptPlainText(title, content);
                fallback.usedFallback = true;
                return fallback;
            } catch (Exception textError) {
                UploadResult fail = new UploadResult();
                fail.ok = false;
                fail.message = "发送失败：" + humanError(textError);
                fail.detail = "JSON 接口失败：" + humanError(jsonError) + "\n纯文本备用接口失败：" + humanError(textError);
                return fail;
            }
        }
    }

    UploadResult updateScript(String id, String title, String content) {
        try {
            String json = "{\"title\":\"" + JsonUtil.escape(title) + "\",\"content\":\"" + JsonUtil.escape(content) + "\"}";
            byte[] body = json.getBytes(StandardCharsets.UTF_8);
            String path = "/api/remote/scripts/update?id=" + encode(id);
            HttpResponse<String> r = request("POST", path, body, "application/json; charset=utf-8", 10000);
            if (r.statusCode() < 200 || r.statusCode() >= 300) throw new IOException("HTTP " + r.statusCode());
            UploadResult out = parseUploadResult(r.body(), false);
            out.message = "已保存到提词器：" + out.title + "，" + out.length + " 字";
            return out;
        } catch (Exception e) {
            UploadResult fail = new UploadResult();
            fail.ok = false;
            fail.message = "保存失败：" + humanError(e);
            return fail;
        }
    }

    ListResult listScripts() {
        try {
            HttpResponse<String> r = request("GET", "/api/remote/scripts", null, "text/plain; charset=utf-8", 4000);
            if (r.statusCode() != 200) return ListResult.fail("HTTP " + r.statusCode());
            String body = r.body() == null ? "" : r.body();
            if (!JsonUtil.bool(body, "ok", false)) {
                String err = JsonUtil.string(body, "error", "提词端返回 ok=false");
                return ListResult.fail(err);
            }
            ListResult result = new ListResult();
            result.ok = true;
            result.count = JsonUtil.integer(body, "count", 0);
            result.scripts = parseScriptSummaries(body);
            if (result.count == 0) result.count = result.scripts.size();
            return result;
        } catch (Exception e) {
            return ListResult.fail(humanError(e));
        }
    }

    ScriptDetailResult getScript(String id) {
        try {
            HttpResponse<String> r = request("GET", "/api/remote/scripts/get?id=" + encode(id), null, "text/plain; charset=utf-8", 6000);
            if (r.statusCode() != 200) return ScriptDetailResult.fail("HTTP " + r.statusCode());
            String body = r.body() == null ? "" : r.body();
            if (!JsonUtil.bool(body, "ok", false)) return ScriptDetailResult.fail(JsonUtil.string(body, "error", "提词端返回 ok=false"));
            ScriptDetailResult out = new ScriptDetailResult();
            out.ok = true;
            out.id = JsonUtil.string(body, "id", id);
            out.title = JsonUtil.string(body, "title", "未命名文稿");
            out.content = JsonUtil.string(body, "content", "");
            out.length = JsonUtil.integer(body, "length", out.content == null ? 0 : out.content.length());
            out.createdAt = JsonUtil.longValue(body, "createdAt", 0);
            out.updatedAt = JsonUtil.longValue(body, "updatedAt", 0);
            return out;
        } catch (Exception e) {
            return ScriptDetailResult.fail(humanError(e));
        }
    }

    BasicResult deleteScript(String id) {
        try {
            HttpResponse<String> r = request("POST", "/api/remote/scripts/delete?id=" + encode(id), new byte[0], "text/plain; charset=utf-8", 5000);
            ensureOk(r);
            return BasicResult.ok("已删除文稿");
        } catch (Exception e) {
            return BasicResult.fail(humanError(e));
        }
    }

    BasicResult startPrompt(String id) {
        try {
            HttpResponse<String> r = request("POST", "/api/remote/prompt/start?id=" + encode(id), new byte[0], "text/plain; charset=utf-8", 5000);
            ensureOk(r);
            return BasicResult.ok("已发送：开始提词");
        } catch (Exception e) {
            return BasicResult.fail(humanError(e));
        }
    }

    BasicResult stopPrompt() {
        try {
            HttpResponse<String> r = request("POST", "/api/remote/prompt/stop", new byte[0], "text/plain; charset=utf-8", 5000);
            ensureOk(r);
            return BasicResult.ok("已发送：关闭提词");
        } catch (Exception e) {
            return BasicResult.fail(humanError(e));
        }
    }

    PromptStatusResult promptStatus() {
        try {
            HttpResponse<String> r = request("GET", "/api/remote/prompt/status", null, "text/plain; charset=utf-8", 4000);
            if (r.statusCode() != 200) return PromptStatusResult.fail("HTTP " + r.statusCode());
            String body = r.body() == null ? "" : r.body();
            if (!JsonUtil.bool(body, "ok", false)) return PromptStatusResult.fail(JsonUtil.string(body, "error", "提词端返回 ok=false"));
            PromptStatusResult out = new PromptStatusResult();
            out.ok = true;
            out.prompting = JsonUtil.bool(body, "prompting", JsonUtil.bool(body, "running", JsonUtil.bool(body, "isPrompting", false)));
            out.id = JsonUtil.string(body, "id", JsonUtil.string(body, "scriptId", ""));
            out.title = JsonUtil.string(body, "title", JsonUtil.string(body, "scriptTitle", ""));
            out.message = out.prompting ? (out.title == null || out.title.isBlank() ? "正在提词" : "正在提词：" + out.title) : "未提词";
            return out;
        } catch (Exception e) {
            return PromptStatusResult.fail(humanError(e));
        }
    }

    private UploadResult uploadScriptJson(String title, String content) throws IOException, InterruptedException {
        String json = "{\"title\":\"" + JsonUtil.escape(title) + "\",\"content\":\"" + JsonUtil.escape(content) + "\"}";
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        HttpResponse<String> r = request("POST", "/api/remote/scripts/add", body, "application/json; charset=utf-8", 8000);
        if (r.statusCode() < 200 || r.statusCode() >= 300) throw new IOException("HTTP " + r.statusCode());
        return parseUploadResult(r.body(), false);
    }

    private UploadResult uploadScriptPlainText(String title, String content) throws IOException, InterruptedException {
        String path = "/api/remote/scripts/add?title=" + encode(title == null ? "" : title);
        byte[] body = content.getBytes(StandardCharsets.UTF_8);
        HttpResponse<String> r = request("POST", path, body, "text/plain; charset=utf-8", 10000);
        if (r.statusCode() < 200 || r.statusCode() >= 300) throw new IOException("HTTP " + r.statusCode());
        return parseUploadResult(r.body(), true);
    }

    private UploadResult parseUploadResult(String body, boolean fallback) throws IOException {
        UploadResult out = new UploadResult();
        out.ok = JsonUtil.bool(body, "ok", false);
        out.usedFallback = fallback;
        out.id = JsonUtil.string(body, "id", "");
        out.title = JsonUtil.string(body, "title", "未命名文稿");
        out.length = JsonUtil.integer(body, "length", 0);
        out.createdAt = JsonUtil.longValue(body, "createdAt", 0);
        out.updatedAt = JsonUtil.longValue(body, "updatedAt", 0);
        if (!out.ok) {
            out.message = JsonUtil.string(body, "error", "提词端返回 ok=false");
            throw new IOException(out.message);
        }
        out.message = "已发送到提词器：" + out.title + "，" + out.length + " 字" + (fallback ? "（备用接口）" : "");
        return out;
    }

    private List<ScriptSummary> parseScriptSummaries(String json) {
        List<ScriptSummary> list = new ArrayList<>();
        Matcher arrayMatcher = Pattern.compile("\\\"scripts\\\"\\s*:\\s*\\[(.*?)]", Pattern.DOTALL).matcher(json == null ? "" : json);
        if (!arrayMatcher.find()) return list;
        String arr = arrayMatcher.group(1);
        Matcher objectMatcher = Pattern.compile("\\{(.*?)\\}", Pattern.DOTALL).matcher(arr);
        while (objectMatcher.find()) {
            String obj = "{" + objectMatcher.group(1) + "}";
            ScriptSummary s = new ScriptSummary();
            s.id = JsonUtil.string(obj, "id", "");
            s.title = JsonUtil.string(obj, "title", "未命名文稿");
            s.length = JsonUtil.integer(obj, "length", 0);
            s.createdAt = JsonUtil.longValue(obj, "createdAt", 0);
            s.updatedAt = JsonUtil.longValue(obj, "updatedAt", 0);
            list.add(s);
        }
        return list;
    }

    private void scrollHttp(int dy) throws IOException, InterruptedException {
        HttpResponse<String> r = request("POST", "/api/remote/scroll?dy=" + dy, new byte[0], "text/plain; charset=utf-8", 2500);
        ensureOk(r);
    }

    private void sendUdp(String text) throws IOException {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        InetAddress address = InetAddress.getByName(ip);
        DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, port);
        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(500);
            socket.send(packet);
        }
    }

    private HttpResponse<String> request(String method, String path, byte[] body, String contentType, long timeoutMs) throws IOException, InterruptedException {
        String safePath = path.startsWith("/") ? path : "/" + path;
        URI uri = URI.create("http://" + ip + ":" + port + safePath);
        HttpRequest.Builder b = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Accept", "application/json, text/plain, */*");
        if (body == null) {
            b.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            b.header("Content-Type", contentType);
            b.method(method, HttpRequest.BodyPublishers.ofByteArray(body));
        }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    private void ensureOk(HttpResponse<String> r) throws IOException {
        if (r.statusCode() < 200 || r.statusCode() >= 300) throw new IOException("HTTP " + r.statusCode());
        String body = r.body();
        if (body != null && body.trim().startsWith("{") && !JsonUtil.bool(body, "ok", true)) {
            throw new IOException(JsonUtil.string(body, "error", "提词端返回 ok=false"));
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    static boolean isValidIpOrHost(String input) {
        if (input == null || input.trim().isEmpty()) return false;
        String s = input.trim();
        if (s.contains(" ") || s.contains("/")) return false;
        if (s.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
            String[] parts = s.split("\\.");
            for (String part : parts) {
                try {
                    int n = Integer.parseInt(part);
                    if (n < 0 || n > 255) return false;
                } catch (Exception e) { return false; }
            }
        }
        return true;
    }

    static String humanError(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null && (t instanceof java.util.concurrent.ExecutionException || t instanceof RuntimeException)) t = t.getCause();
        if (t instanceof java.net.http.HttpTimeoutException || t instanceof SocketTimeoutException) {
            return "请求超时，请检查两台设备是否在同一局域网";
        }
        if (t instanceof ConnectException) return "无法连接，请检查提词器端是否开启遥控服务";
        if (t instanceof UnknownHostException) return "无法解析 IP / 主机名";
        String msg = t.getMessage();
        if (msg == null || msg.isBlank()) msg = t.getClass().getSimpleName();
        return msg;
    }

    static final class PingResult {
        boolean ok;
        boolean scriptUpload;
        boolean scriptManage;
        boolean remotePrompt;
        boolean http;
        boolean udp;
        int version;
        String message;

        static PingResult fail(String msg) {
            PingResult r = new PingResult();
            r.ok = false;
            r.message = msg;
            return r;
        }
    }

    static final class BasicResult {
        boolean ok;
        String message;
        static BasicResult ok(String msg) { BasicResult r = new BasicResult(); r.ok = true; r.message = msg; return r; }
        static BasicResult fail(String msg) { BasicResult r = new BasicResult(); r.ok = false; r.message = msg; return r; }
    }

    static final class UploadResult {
        boolean ok;
        boolean usedFallback;
        String id;
        String title;
        int length;
        long createdAt;
        long updatedAt;
        String message;
        String detail;
    }

    static final class ScriptSummary {
        String id;
        String title;
        int length;
        long createdAt;
        long updatedAt;
    }

    static final class ScriptDetailResult {
        boolean ok;
        String id;
        String title;
        String content;
        int length;
        long createdAt;
        long updatedAt;
        String message;
        static ScriptDetailResult fail(String msg) { ScriptDetailResult r = new ScriptDetailResult(); r.ok = false; r.message = msg; return r; }
    }

    static final class PromptStatusResult {
        boolean ok;
        boolean prompting;
        String id;
        String title;
        String message;
        static PromptStatusResult fail(String msg) { PromptStatusResult r = new PromptStatusResult(); r.ok = false; r.message = msg; return r; }
    }

    static final class ListResult {
        boolean ok;
        int count;
        List<ScriptSummary> scripts = new ArrayList<>();
        String message;

        static ListResult fail(String msg) {
            ListResult r = new ListResult();
            r.ok = false;
            r.message = msg;
            return r;
        }
    }
}
