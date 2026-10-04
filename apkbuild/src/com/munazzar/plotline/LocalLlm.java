package com.munazzar.plotline;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;

/*
 * On-device answers for Insights: llama.cpp's llama-server, built for arm64 and shipped as
 * lib/arm64-v8a/libllamaserver.so (native libraries are the only files an app may execute).
 * It listens on 127.0.0.1 with a random key; only this class talks to it, and tokens are
 * streamed to the page with window.__llm(id, kind, text). Models are GGUF files downloaded
 * once into the app's private files. The server stops after 10 idle minutes.
 */
final class LocalLlm {
    static final Object L = new Object();
    static Process proc; static String procModel = ""; static int port; static String key = "";
    static long lastUse;
    static String dlName = "", dlErr = ""; static volatile long dlLoaded, dlTotal; static volatile boolean dlBusy;
    static final Map<String, HttpURLConnection> live = new HashMap<>();
    static Thread watchdog;

    static File dir(android.content.Context a) { File d = new File(a.getFilesDir(), "models"); d.mkdirs(); return d; }
    static File bin(android.content.Context a) { return new File(a.getApplicationInfo().nativeLibraryDir, "libllamaserver.so"); }
    static boolean supported(android.content.Context a) { return bin(a).exists(); }

    static String status(android.content.Context a) {
        try {
            JSONObject o = new JSONObject();
            o.put("ok", supported(a));
            JSONObject d = new JSONObject();
            d.put("name", dlName); d.put("loaded", dlLoaded); d.put("total", dlTotal); d.put("busy", dlBusy); d.put("err", dlErr);
            o.put("dl", d);
            JSONObject have = new JSONObject();
            File[] fs = dir(a).listFiles();
            if (fs != null) for (File f : fs) if (f.getName().endsWith(".gguf")) have.put(f.getName(), f.length());
            o.put("have", have);
            synchronized (L) { o.put("running", alive() ? procModel : ""); }
            o.put("cores", Runtime.getRuntime().availableProcessors());
            return o.toString();
        } catch (Exception e) { return "{\"ok\":false}"; }
    }

    static boolean alive() {
        if (proc == null) return false;
        try { proc.exitValue(); return false; } catch (IllegalThreadStateException e) { return true; }
    }

    /* download with resume into <name>.part, renamed when complete */
    static void download(final MainActivity a, final String url, final String name) {
        synchronized (L) { if (dlBusy) return; dlBusy = true; dlName = name; dlErr = ""; dlLoaded = 0; dlTotal = 0; }
        new Thread(new Runnable() { public void run() {
            File part = new File(dir(a), name + ".part"), done = new File(dir(a), name);
            try {
                String u = url;
                for (int hop = 0; hop < 6; hop++) {
                    long have = part.exists() ? part.length() : 0;
                    HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
                    c.setInstanceFollowRedirects(false); c.setConnectTimeout(20000); c.setReadTimeout(60000);
                    if (have > 0) c.setRequestProperty("Range", "bytes=" + have + "-");
                    int code = c.getResponseCode();
                    if (code >= 300 && code < 400) { u = new URL(new URL(u), c.getHeaderField("Location")).toString(); c.disconnect(); continue; }
                    if (code != 200 && code != 206) throw new Exception("HTTP " + code);
                    if (code == 200) have = 0;
                    long len = c.getContentLengthLong();
                    dlTotal = len > 0 ? have + len : 0; dlLoaded = have;
                    try (InputStream in = c.getInputStream(); OutputStream out = new FileOutputStream(part, code == 206)) {
                        byte[] b = new byte[1 << 16]; int n;
                        while ((n = in.read(b)) > 0) { out.write(b, 0, n); dlLoaded += n; }
                    }
                    if (dlTotal > 0 && part.length() < dlTotal) throw new Exception("Download incomplete");
                    if (!part.renameTo(done)) throw new Exception("Couldn't save the model");
                    break;
                }
                if (!done.exists()) throw new Exception("Too many redirects");
            } catch (Exception e) { dlErr = e.getMessage() == null ? "Download failed" : e.getMessage(); }
            finally { dlBusy = false; }
        } }).start();
    }

    static void delete(MainActivity a, String name) {
        synchronized (L) { if (name.equals(procModel)) stop(); }
        new File(dir(a), name).delete(); new File(dir(a), name + ".part").delete();
    }

    /* start (or reuse) the server for this model in the background; reports window.__llm(id,'ready'|'err',msg) */
    static void start(final MainActivity a, final String id, final String name, final int ctx, final int threads) {
        new Thread(new Runnable() { public void run() {
            String err = ensure(a, name, ctx, threads);
            send(a, id, err.isEmpty() ? "ready" : "err", err);
        } }).start();
    }

    /* blocking: make sure the server runs this model; "" when ready, else the reason. Also used by ReplyService with the app closed. */
    static String ensure(android.content.Context a, String name, int ctx, int threads) {
        String err;
        boolean reuse;
        synchronized (L) {
            lastUse = System.currentTimeMillis();
            reuse = alive() && name.equals(procModel);
            err = "";
            if (!reuse) {
                stop();
                File m = new File(dir(a), name);
                if (!m.exists()) err = "Model not downloaded";
                else if (!supported(a)) err = "On-device engine missing";
                else try {
                    try (ServerSocket s = new ServerSocket(0)) { port = s.getLocalPort(); }
                    byte[] r = new byte[18]; new SecureRandom().nextBytes(r);
                    StringBuilder k = new StringBuilder(); for (byte x : r) k.append(String.format("%02x", x)); key = k.toString();
                    ProcessBuilder pb = new ProcessBuilder(bin(a).getAbsolutePath(), "-m", m.getAbsolutePath(), "--host", "127.0.0.1", "--port", String.valueOf(port),
                            "--api-key", key, "-c", String.valueOf(ctx), "-t", String.valueOf(threads), "-tb", String.valueOf(threads), "-ngl", "0", "-np", "1", "--jinja", "--no-warmup");
                    pb.redirectErrorStream(true); pb.redirectOutput(new File(a.getCacheDir(), "llama-server.log"));
                    pb.environment().put("HOME", a.getFilesDir().getAbsolutePath());
                    proc = pb.start(); procModel = name;
                } catch (Exception e) { proc = null; procModel = ""; err = "Couldn't start the on-device engine: " + e.getMessage(); }
            }
        }
        if (err.isEmpty()) { startWatchdog(); err = health(reuse ? 30000 : 180000); }
        if (!err.isEmpty() && !reuse) stop();
        return err;
    }

    /* blocking, non-streaming completion; body is an OpenAI-style request. Returns the message text. */
    static String complete(String body, int timeoutMs) throws Exception {
        HttpURLConnection c = null;
        String id = "c" + System.nanoTime();
        try {
            synchronized (L) { lastUse = System.currentTimeMillis(); if (!alive()) throw new Exception("The on-device engine isn't running"); }
            JSONObject req = new JSONObject(body); req.put("stream", false); req.put("cache_prompt", true);
            c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/v1/chat/completions").openConnection();
            synchronized (L) { live.put(id, c); }
            c.setDoOutput(true); c.setConnectTimeout(5000); c.setReadTimeout(timeoutMs);
            c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("Authorization", "Bearer " + key);
            try (OutputStream o = c.getOutputStream()) { o.write(req.toString().getBytes(StandardCharsets.UTF_8)); }
            int code = c.getResponseCode();
            if (code != 200) throw new Exception("HTTP " + code);
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) { String ln; while ((ln = r.readLine()) != null) sb.append(ln).append('\n'); }
            return new JSONObject(sb.toString()).getJSONArray("choices").getJSONObject(0).getJSONObject("message").optString("content", "");
        } finally {
            synchronized (L) { live.remove(id); lastUse = System.currentTimeMillis(); }
            if (c != null) c.disconnect();
        }
    }

    static String health(long waitMs) {
        long end = System.currentTimeMillis() + waitMs;
        while (System.currentTimeMillis() < end) {
            if (!alive()) return "The on-device engine stopped. The phone may be low on memory.";
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/health").openConnection();
                c.setConnectTimeout(1000); c.setReadTimeout(2000);
                if (c.getResponseCode() == 200) { c.disconnect(); return ""; }
                c.disconnect();
            } catch (Exception ignored) { }
            try { Thread.sleep(300); } catch (InterruptedException e) { break; }
        }
        return "The on-device model took too long to load.";
    }

    static void stop() {
        synchronized (L) { if (proc != null) { proc.destroy(); try { if (!proc.waitFor(2, java.util.concurrent.TimeUnit.SECONDS)) proc.destroyForcibly(); } catch (Exception ignored) { } } proc = null; procModel = ""; }
    }

    static void startWatchdog() {
        if (watchdog != null && watchdog.isAlive()) return;
        watchdog = new Thread(new Runnable() { public void run() {
            while (true) {
                try { Thread.sleep(30000); } catch (InterruptedException e) { return; }
                synchronized (L) { if (proc == null) return; if (live.isEmpty() && System.currentTimeMillis() - lastUse > 10 * 60000) { stop(); return; } }
            }
        } });
        watchdog.setDaemon(true); watchdog.start();
    }

    /* stream a chat completion; body is an OpenAI-style JSON request (messages, max_tokens, …) */
    static void chat(final MainActivity a, final String id, final String body) {
        new Thread(new Runnable() { public void run() {
            HttpURLConnection c = null;
            try {
                synchronized (L) { lastUse = System.currentTimeMillis(); if (!alive()) throw new Exception("The on-device engine isn't running"); }
                JSONObject req = new JSONObject(body); req.put("stream", true); req.put("cache_prompt", true);
                c = (HttpURLConnection) new URL("http://127.0.0.1:" + port + "/v1/chat/completions").openConnection();
                synchronized (L) { live.put(id, c); }
                c.setDoOutput(true); c.setConnectTimeout(5000); c.setReadTimeout(10 * 60000); c.setChunkedStreamingMode(0);
                c.setRequestProperty("Content-Type", "application/json"); c.setRequestProperty("Authorization", "Bearer " + key);
                try (OutputStream o = c.getOutputStream()) { o.write(req.toString().getBytes(StandardCharsets.UTF_8)); }
                int code = c.getResponseCode();
                if (code != 200) {
                    String msg = "HTTP " + code;
                    try (InputStream es = c.getErrorStream()) { if (es != null) { byte[] b = new byte[600]; int n = es.read(b); if (n > 0) msg += ": " + new String(b, 0, n, StandardCharsets.UTF_8); } }
                    throw new Exception(msg);
                }
                try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
                    String ln;
                    while ((ln = r.readLine()) != null) {
                        if (!ln.startsWith("data:")) continue;
                        String d = ln.substring(5).trim();
                        if (d.equals("[DONE]")) break;
                        try {
                            JSONObject j = new JSONObject(d);
                            JSONObject delta = j.getJSONArray("choices").getJSONObject(0).optJSONObject("delta");
                            String t = delta == null ? null : delta.optString("content", null);
                            if (t != null && t.length() > 0 && !t.equals("null")) send(a, id, "t", t);
                        } catch (Exception ignored) { }
                    }
                }
                send(a, id, "done", "");
            } catch (Exception e) {
                boolean cancelled; synchronized (L) { cancelled = !live.containsKey(id); }
                send(a, id, cancelled ? "done" : "err", e.getMessage() == null ? "Failed" : e.getMessage());
            } finally {
                synchronized (L) { live.remove(id); lastUse = System.currentTimeMillis(); }
                if (c != null) c.disconnect();
            }
        } }).start();
    }

    static void cancel(String id) {
        HttpURLConnection c; synchronized (L) { c = live.remove(id); }
        if (c != null) try { c.disconnect(); } catch (Exception ignored) { }
    }

    static void send(MainActivity a, String id, String kind, String text) {
        a.js("window.__llm&&window.__llm(" + JSONObject.quote(id) + "," + JSONObject.quote(kind) + "," + JSONObject.quote(text) + ")");
    }
}
