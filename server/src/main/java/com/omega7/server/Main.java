package com.omega7.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.Executors;

/** Blind Ω7 relay. It never parses or decrypts message plaintext. TLS must be terminated by the deployment edge. */
public final class Main {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_DEVICES = 7;
    private static final int MAX_BODY = 384 * 1024;
    private static final int MAX_PREKEYS_PER_UPLOAD = 64;
    private static final String DB_URL = env("OMEGA7_DB_URL", "");
    private static final String DB_USER = env("OMEGA7_DB_USER", "");
    private static final String DB_PASSWORD = env("OMEGA7_DB_PASSWORD", "");
    private static final byte[] AUTH_SECRET = env("OMEGA7_AUTH_SECRET", "").getBytes(StandardCharsets.UTF_8);
    private static final String BOOTSTRAP_SECRET = env("OMEGA7_BOOTSTRAP_SECRET", "");

    public static void main(String[] args) throws Exception {
        requireProductionSecrets();
        try (Connection c = db()) { init(c); }
        int port = Integer.parseInt(env("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 128);
        server.createContext("/v1/health", Main::health);
        server.createContext("/v1/devices/register", Main::register);
        server.createContext("/v1/bootstrap", Main::bootstrap);
        server.createContext("/v1/pair/invites", Main::createInvite);
        server.createContext("/v1/pair/approve", Main::approve);
        server.createContext("/v1/devices/revoke", Main::revoke);
        server.createContext("/v1/keys/prekeys", Main::uploadPreKeys);
        server.createContext("/v1/keys", Main::keys);
        server.createContext("/v1/messages", Main::messages);
        server.createContext("/v1/sync", Main::sync);
        server.setExecutor(Executors.newFixedThreadPool(Math.max(4, Runtime.getRuntime().availableProcessors())));
        server.start();
        System.out.println("Ω7 relay listening on " + port);
    }

    private static void requireProductionSecrets() {
        requireSecret("OMEGA7_DB_URL", DB_URL);
        requireSecret("OMEGA7_DB_USER", DB_USER);
        requireSecret("OMEGA7_DB_PASSWORD", DB_PASSWORD);
        requireSecret("OMEGA7_AUTH_SECRET", new String(AUTH_SECRET, StandardCharsets.UTF_8));
        requireSecret("OMEGA7_BOOTSTRAP_SECRET", BOOTSTRAP_SECRET);
        if (AUTH_SECRET.length < 32 || BOOTSTRAP_SECRET.length() < 32) throw new IllegalStateException("Sekrety Ω7 muszą mieć co najmniej 32 znaki.");
    }
    private static void requireSecret(String name, String value) { if (value == null || value.isBlank()) throw new IllegalStateException("Brak wymaganego sekretu środowiskowego: " + name); }

    private static void init(Connection c) throws Exception {
        try (InputStream in = Main.class.getResourceAsStream("/schema.sql")) {
            if (in == null) throw new IllegalStateException("schema.sql missing");
            String sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            for (String statement : sql.split(";")) if (!statement.isBlank()) try (Statement s = c.createStatement()) { s.execute(statement); }
        }
    }

    private static void health(HttpExchange x) throws IOException {
        if (!method(x, "GET")) return;
        try (Connection c = db(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT 1")) { json(x, 200, obj().put("status", "ok")); }
        catch (Exception e) { json(x, 503, obj().put("status", "degraded")); }
    }

    private static void bootstrap(HttpExchange x) throws IOException {
        if (!method(x, "POST")) return;
        if (!BOOTSTRAP_SECRET.equals(x.getRequestHeaders().getFirst("X-Bootstrap-Secret"))) { json(x, 403, error("Brak uprawnień.")); return; }
        JsonNode b = readJson(x); String g = text(b, "groupId", 128); int d = integer(b, "deviceId", 1, 127); require(g != null, "groupId");
        try (Connection c = db()) {
            c.setAutoCommit(false);
            try (PreparedStatement p = c.prepareStatement("INSERT INTO omega_groups(group_id) VALUES(?) ON CONFLICT DO NOTHING")) { p.setString(1, g); p.executeUpdate(); }
            byte[] token = random(32); insertDevice(c, g, d, b, token); insertPreKeys(c, g, d, b); c.commit();
            json(x, 201, obj().put("deviceId", d).put("authToken", tokenString(token))); java.util.Arrays.fill(token, (byte) 0);
        } catch (Exception e) { json(x, 409, error("Nie można utworzyć właściciela grupy.")); }
    }

    private static void createInvite(HttpExchange x) throws IOException {
        if (!method(x, "POST")) return;
        String auth = auth(x); if (auth == null) { json(x, 401, error("Brak uwierzytelnienia.")); return; }
        JsonNode b = readJson(x); String group = text(b, "groupId", 128); int owner = integer(b, "ownerDeviceId", 1, 127); require(group != null, "groupId");
        try (Connection c = db()) {
            if (!authorized(c, group, owner, auth)) { json(x, 403, error("Brak uprawnień.")); return; }
            if (!hasCapacity(c, group)) { json(x, 409, error("Limit 7 urządzeń został osiągnięty.")); return; }
            byte[] token = random(32); UUID id = UUID.randomUUID();
            try (PreparedStatement p = c.prepareStatement("INSERT INTO invites(invite_id,group_id,owner_device_id,token_hash,expires_at) VALUES(?,?,?,?,now()+interval '5 minutes')")) {
                p.setObject(1, id); p.setString(2, group); p.setInt(3, owner); p.setBytes(4, hmac(token)); p.executeUpdate();
            }
            json(x, 201, obj().put("inviteId", id.toString()).put("expiresInSeconds", 300).put("inviteToken", tokenString(token))); java.util.Arrays.fill(token, (byte) 0);
        } catch (Exception e) { json(x, 503, error("Nie można utworzyć zaproszenia.")); }
    }

    /** Binds the owner's authenticated approval to the exact joining request before registration is allowed. */
    private static void approve(HttpExchange x) throws IOException {
        if (!method(x, "POST")) return;
        String token = auth(x); if (token == null) { json(x, 401, error("Brak uwierzytelnienia.")); return; }
        JsonNode b = readJson(x);
        String group = text(b, "groupId", 128), inviteRaw = text(b, "inviteId", 64), requestSignature = text(b, "requestSignature", 1024);
        int owner = integer(b, "ownerDeviceId", 1, 127), target = integer(b, "targetDeviceId", 1, 127);
        require(group != null && inviteRaw != null && requestSignature != null, "groupId/inviteId/requestSignature");
        if (owner == target) { json(x, 400, error("Właściciel nie może zatwierdzić własnego urządzenia.")); return; }
        UUID invite; try { invite = UUID.fromString(inviteRaw); } catch (Exception e) { json(x, 400, error("Nieprawidłowy inviteId.")); return; }
        try (Connection c = db()) {
            if (!authorized(c, group, owner, token)) { json(x, 403, error("Brak uprawnień.")); return; }
            byte[] requestHash = hmac(requestSignature.getBytes(StandardCharsets.UTF_8));
            try (PreparedStatement p = c.prepareStatement("UPDATE invites SET approved_device_id=?,approved_request_hash=?,approved_at=now() WHERE invite_id=? AND group_id=? AND owner_device_id=? AND consumed_at IS NULL AND approved_at IS NULL AND expires_at>now()")) {
                p.setInt(1, target); p.setBytes(2, requestHash); p.setObject(3, invite); p.setString(4, group); p.setInt(5, owner);
                if (p.executeUpdate() != 1) { json(x, 409, error("Zaproszenie jest nieważne, wygasło lub zostało już zatwierdzone.")); return; }
            }
            java.util.Arrays.fill(requestHash, (byte) 0);
            json(x, 200, obj().put("approved", true).put("deviceId", target));
        } catch (Exception e) { json(x, 503, error("Nie można zapisać zatwierdzenia.")); }
    }

    private static void register(HttpExchange x) throws IOException {
        if (!method(x, "POST")) return;
        JsonNode body = readJson(x);
        String group = text(body, "groupId", 128), inviteToken = text(body, "inviteToken", 512), requestSignature = text(body, "requestSignature", 1024);
        int device = integer(body, "deviceId", 1, 127);
        require(group != null && inviteToken != null && requestSignature != null, "groupId/inviteToken/requestSignature");
        try (Connection c = db()) {
            c.setAutoCommit(false);
            UUID inviteId = consumeInvite(c, group, inviteToken, device, requestSignature);
            if (inviteId == null) { c.rollback(); json(x, 403, error("Nieprawidłowe, wygasłe lub niezatwierdzone zaproszenie.")); return; }
            try (PreparedStatement lock = c.prepareStatement("SELECT group_id FROM omega_groups WHERE group_id=? FOR UPDATE")) {
                lock.setString(1, group);
                try (ResultSet locked = lock.executeQuery()) { if (!locked.next()) { c.rollback(); json(x, 404, error("Grupa nie istnieje.")); return; } }
            }
            if (!hasCapacity(c, group)) { c.rollback(); json(x, 409, error("Limit 7 urządzeń został osiągnięty.")); return; }
            byte[] authToken = random(32); insertDevice(c, group, device, body, authToken); insertPreKeys(c, group, device, body); c.commit();
            json(x, 201, obj().put("deviceId", device).put("authToken", tokenString(authToken))); java.util.Arrays.fill(authToken, (byte) 0);
        } catch (Exception e) { json(x, 400, error("Rejestracja urządzenia odrzucona.")); }
    }

    private static boolean hasCapacity(Connection c, String group) throws SQLException {
        try (PreparedStatement count = c.prepareStatement("SELECT count(*) FROM devices WHERE group_id=? AND active=true")) { count.setString(1, group); try (ResultSet rs = count.executeQuery()) { rs.next(); return rs.getInt(1) < MAX_DEVICES; } }
    }

    private static void insertDevice(Connection c, String group, int device, JsonNode b, byte[] token) throws Exception {
        try (PreparedStatement p = c.prepareStatement("INSERT INTO devices(group_id,device_id,registration_id,identity_key,signed_prekey_id,signed_prekey,signed_prekey_signature,kyber_prekey_id,kyber_prekey,kyber_prekey_signature,auth_token_hash) VALUES (?,?,?,?,?,?,?,?,?,?,?)")) {
            p.setString(1, group); p.setInt(2, device); p.setInt(3, integer(b, "registrationId", 1, Integer.MAX_VALUE)); p.setBytes(4, b64(b, "identityKey"));
            p.setInt(5, integer(b, "signedPreKeyId", 1, Integer.MAX_VALUE)); p.setBytes(6, b64(b, "signedPreKey")); p.setBytes(7, b64(b, "signedPreKeySignature"));
            p.setInt(8, integer(b, "kyberPreKeyId", 1, Integer.MAX_VALUE)); p.setBytes(9, b64(b, "kyberPreKey")); p.setBytes(10, b64(b, "kyberPreKeySignature")); p.setBytes(11, hmac(token)); p.executeUpdate();
        }
    }

    private static void insertPreKeys(Connection c, String group, int device, JsonNode b) throws Exception {
        JsonNode list = b.path("preKeys");
        if (list.isArray() && list.size() > MAX_PREKEYS_PER_UPLOAD) throw new IllegalArgumentException("Za dużo prekeys.");
        if (list.isArray() && list.size() > 0) {
            try (PreparedStatement p = c.prepareStatement("INSERT INTO one_time_prekeys(group_id,device_id,prekey_id,prekey) VALUES(?,?,?,?) ON CONFLICT DO NOTHING")) {
                for (JsonNode item : list) { int id = integer(item, "id", 1, Integer.MAX_VALUE); byte[] key = b64(item, "key"); if (key.length == 0 || key.length > 4096) throw new IllegalArgumentException("Nieprawidłowy prekey."); p.setString(1, group); p.setInt(2, device); p.setInt(3, id); p.setBytes(4, key); p.addBatch(); }
                p.executeBatch();
            }
        } else if (b.hasNonNull("preKeyId") && b.hasNonNull("preKey")) {
            int id = integer(b, "preKeyId", 1, Integer.MAX_VALUE); byte[] key = b64(b, "preKey"); if (key.length == 0 || key.length > 4096) throw new IllegalArgumentException("Nieprawidłowy prekey.");
            try (PreparedStatement p = c.prepareStatement("INSERT INTO one_time_prekeys(group_id,device_id,prekey_id,prekey) VALUES(?,?,?,?) ON CONFLICT DO NOTHING")) { p.setString(1, group); p.setInt(2, device); p.setInt(3, id); p.setBytes(4, key); p.executeUpdate(); }
        }
    }

    private static void uploadPreKeys(HttpExchange x) throws IOException {
        if (!method(x, "POST")) return;
        String token = auth(x); if (token == null) { json(x, 401, error("Brak uwierzytelnienia.")); return; }
        JsonNode b = readJson(x); String group = text(b, "groupId", 128); int device = integer(b, "deviceId", 1, 127); JsonNode list = b.path("preKeys"); require(group != null && list.isArray(), "groupId/preKeys");
        if (list.size() < 1 || list.size() > MAX_PREKEYS_PER_UPLOAD) { json(x, 400, error("Nieprawidłowa liczba prekeys.")); return; }
        try (Connection c = db()) { if (!authorized(c, group, device, token)) { json(x, 403, error("Brak uprawnień.")); return; } insertPreKeys(c, group, device, b); json(x, 202, obj().put("accepted", true).put("count", list.size())); }
        catch (Exception e) { json(x, 400, error("Nie można zapisać prekeys.")); }
    }

    private static UUID consumeInvite(Connection c, String group, String token, int device, String requestSignature) throws Exception {
        byte[] hash = hmac(token.getBytes(StandardCharsets.UTF_8)); byte[] requestHash = hmac(requestSignature.getBytes(StandardCharsets.UTF_8));
        try (PreparedStatement p = c.prepareStatement("UPDATE invites SET consumed_at=now() WHERE group_id=? AND token_hash=? AND consumed_at IS NULL AND expires_at>now() AND approved_device_id=? AND approved_request_hash=? AND approved_at IS NOT NULL RETURNING invite_id")) {
            p.setString(1, group); p.setBytes(2, hash); p.setInt(3, device); p.setBytes(4, requestHash); try (ResultSet r = p.executeQuery()) { return r.next() ? UUID.fromString(r.getString(1)) : null; }
        } finally { java.util.Arrays.fill(hash, (byte) 0); java.util.Arrays.fill(requestHash, (byte) 0); }
    }

    private static void revoke(HttpExchange x) throws IOException {
        if (!method(x, "POST")) return; String token = auth(x); if (token == null) { json(x, 401, error("Brak uwierzytelnienia.")); return; }
        JsonNode b = readJson(x); String group = text(b, "groupId", 128); int owner = integer(b, "ownerDeviceId", 1, 127); int target = integer(b, "targetDeviceId", 1, 127); require(group != null, "groupId");
        if (owner == target) { json(x, 400, error("Właściciel nie może odwołać własnego urządzenia.")); return; }
        try (Connection c = db()) { if (!authorized(c, group, owner, token)) { json(x, 403, error("Brak uprawnień.")); return; }
            try (PreparedStatement p = c.prepareStatement("UPDATE devices SET active=false,updated_at=now() WHERE group_id=? AND device_id=? AND active=true")) { p.setString(1, group); p.setInt(2, target); if (p.executeUpdate() == 0) { json(x, 404, error("Urządzenie nie istnieje lub jest już odwołane.")); return; } }
            json(x, 200, obj().put("revoked", true).put("deviceId", target));
        } catch (Exception e) { json(x, 503, error("Nie można odwołać urządzenia.")); }
    }

    private static void keys(HttpExchange x) throws IOException {
        if (!method(x, "GET")) return; String token = auth(x); if (token == null) { json(x, 401, error("Brak uwierzytelnienia.")); return; }
        String[] parts = x.getRequestURI().getPath().split("/"); if (parts.length < 5) { json(x, 400, error("Brak identyfikatora.")); return; }
        String group = parts[3]; int device; try { device = Integer.parseInt(parts[4]); } catch (Exception e) { json(x, 400, error("Nieprawidłowe urządzenie.")); return; }
        String requesterRaw = q(x, "requesterDeviceId"); if (requesterRaw == null) { json(x, 400, error("Brak requesterDeviceId.")); return; }
        int requester; try { requester = Integer.parseInt(requesterRaw); } catch (Exception e) { json(x, 400, error("Nieprawidłowe requesterDeviceId.")); return; }
        if (requester < 1 || requester > 127 || requester == device) { json(x, 400, error("Nieprawidłowe urządzenie żądające.")); return; }
        try (Connection c = db()) {
            if (!authorized(c, group, requester, token)) { json(x, 403, error("Brak uprawnień.")); return; }
            c.setAutoCommit(false);
            try (PreparedStatement query = c.prepareStatement("SELECT registration_id,identity_key,signed_prekey_id,signed_prekey,signed_prekey_signature,kyber_prekey_id,kyber_prekey,kyber_prekey_signature FROM devices WHERE group_id=? AND device_id=? AND active=true")) {
                query.setString(1, group); query.setInt(2, device);
                try (ResultSet r = query.executeQuery()) {
                    if (!r.next()) { c.rollback(); json(x, 404, error("Urządzenie nie istnieje.")); return; }
                    ObjectNode out = obj().put("deviceId", device).put("registrationId", r.getInt(1)).put("identityKey", b64(r.getBytes(2))).put("signedPreKeyId", r.getInt(3)).put("signedPreKey", b64(r.getBytes(4))).put("signedPreKeySignature", b64(r.getBytes(5))).put("kyberPreKeyId", r.getInt(6)).put("kyberPreKey", b64(r.getBytes(7))).put("kyberPreKeySignature", b64(r.getBytes(8)));
                    try (PreparedStatement pre = c.prepareStatement("SELECT prekey_id,prekey FROM one_time_prekeys WHERE group_id=? AND device_id=? AND consumed_at IS NULL ORDER BY prekey_id LIMIT 1 FOR UPDATE SKIP LOCKED")) {
                        pre.setString(1, group); pre.setInt(2, device);
                        try (ResultSet pr = pre.executeQuery()) {
                            if (pr.next()) { int id = pr.getInt(1); byte[] key = pr.getBytes(2); out.put("preKeyId", id).put("preKey", b64(key)); try (PreparedStatement consume = c.prepareStatement("UPDATE one_time_prekeys SET consumed_at=now() WHERE group_id=? AND device_id=? AND prekey_id=? AND consumed_at IS NULL")) { consume.setString(1, group); consume.setInt(2, device); consume.setInt(3, id); consume.executeUpdate(); } }
                            else out.putNull("preKeyId").putNull("preKey");
                        }
                    }
                    c.commit(); json(x, 200, out);
                }
            } catch (Exception e) { c.rollback(); throw e; }
        } catch (Exception e) { json(x, 503, error("Błąd usługi.")); }
    }

    private static void messages(HttpExchange x) throws IOException {
        if (!method(x, "POST")) return; String token = auth(x); if (token == null) { json(x, 401, error("Brak uwierzytelnienia.")); return; }
        JsonNode b = readJson(x); String group = text(b, "groupId", 128), idem = text(b, "idempotencyKey", 128); int sender = integer(b, "senderDeviceId", 1, 127), recipient = integer(b, "recipientDeviceId", 1, 127); require(group != null && idem != null, "groupId/idempotencyKey");
        byte[] cipher = b64(b, "ciphertext"); if (cipher.length > 256 * 1024) { json(x, 413, error("Koperta jest zbyt duża.")); return; } if (sender == recipient) { json(x, 400, error("Niedozwolony adres docelowy.")); return; }
        try (Connection c = db()) {
            if (!authorized(c, group, sender, token)) { json(x, 403, error("Brak uprawnień.")); return; }
            try (PreparedStatement target = c.prepareStatement("SELECT 1 FROM devices WHERE group_id=? AND device_id=? AND active=true")) { target.setString(1, group); target.setInt(2, recipient); try (ResultSet rs = target.executeQuery()) { if (!rs.next()) { json(x, 404, error("Urządzenie docelowe nie istnieje lub jest odwołane.")); return; } } }
            try (PreparedStatement p = c.prepareStatement("INSERT INTO messages(group_id,recipient_device_id,sender_device_id,idempotency_key,ciphertext) VALUES(?,?,?,?,?) ON CONFLICT(group_id,recipient_device_id,idempotency_key) DO NOTHING")) { p.setString(1, group); p.setInt(2, recipient); p.setInt(3, sender); p.setString(4, idem); p.setBytes(5, cipher); p.executeUpdate(); }
            json(x, 202, obj().put("accepted", true));
        } catch (Exception e) { json(x, 503, error("Błąd zapisu.")); }
    }

    private static void sync(HttpExchange x) throws IOException {
        if (!method(x, "GET")) return; String token = auth(x); if (token == null) { json(x, 401, error("Brak uwierzytelnienia.")); return; }
        String group = q(x, "groupId"), cur = q(x, "cursor"), deviceRaw = q(x, "deviceId"); if (group == null || group.length() > 128 || deviceRaw == null) { json(x, 400, error("Brak parametrów synchronizacji.")); return; }
        int device; try { device = Integer.parseInt(deviceRaw); } catch (Exception e) { json(x, 400, error("Nieprawidłowe urządzenie.")); return; }
        long cursor; try { cursor = cur == null ? 0 : Long.parseLong(cur); if (cursor < 0) throw new NumberFormatException(); } catch (Exception e) { json(x, 400, error("Nieprawidłowy kursor.")); return; }
        try (Connection c = db()) { if (!authorized(c, group, device, token)) { json(x, 403, error("Brak uprawnień.")); return; }
            try (PreparedStatement p = c.prepareStatement("SELECT seq,sender_device_id,idempotency_key,ciphertext,created_at FROM messages WHERE group_id=? AND recipient_device_id=? AND seq>? ORDER BY seq ASC LIMIT 100")) {
                p.setString(1, group); p.setInt(2, device); p.setLong(3, cursor); try (ResultSet r = p.executeQuery()) { ArrayNode a = JSON.createArrayNode(); long next = cursor; while (r.next()) { next = r.getLong(1); a.add(obj().put("seq", next).put("senderDeviceId", r.getInt(2)).put("idempotencyKey", r.getString(3)).put("ciphertext", b64(r.getBytes(4))).put("createdAt", r.getTimestamp(5).toInstant().toString()); } json(x, 200, obj().put("cursor", next).set("messages", a)); }
            }
        } catch (Exception e) { json(x, 503, error("Błąd synchronizacji.")); }
    }

    private static boolean authorized(Connection c, String group, int device, String token) throws Exception {
        byte[] decoded; try { decoded = Base64.getUrlDecoder().decode(token); } catch (IllegalArgumentException e) { return false; } if (decoded.length != 32) return false;
        try (PreparedStatement p = c.prepareStatement("SELECT 1 FROM devices WHERE group_id=? AND device_id=? AND active=true AND auth_token_hash=?")) { p.setString(1, group); p.setInt(2, device); p.setBytes(3, hmac(decoded)); try (ResultSet r = p.executeQuery()) { return r.next(); } }
    }
    private static String auth(HttpExchange x) { String h = x.getRequestHeaders().getFirst("Authorization"); if (h == null || !h.startsWith("Bearer ")) return null; String v = h.substring(7).trim(); return v.isEmpty() ? null : v; }
    private static Connection db() throws SQLException { return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD); }
    private static byte[] hmac(byte[] data) throws Exception { Mac m = Mac.getInstance("HmacSHA256"); m.init(new SecretKeySpec(AUTH_SECRET, "HmacSHA256")); return m.doFinal(data); }
    private static byte[] random(int n) { byte[] b = new byte[n]; RANDOM.nextBytes(b); return b; }
    private static String tokenString(byte[] b) { return Base64.getUrlEncoder().withoutPadding().encodeToString(b); }
    private static String b64(byte[] b) { return Base64.getEncoder().encodeToString(b); }
    private static byte[] b64(JsonNode n, String f) { String v = n.path(f).asText(null); if (v == null) throw new IllegalArgumentException(f); return Base64.getDecoder().decode(v); }
    private static JsonNode readJson(HttpExchange x) throws IOException { byte[] b = x.getRequestBody().readNBytes(MAX_BODY + 1); if (b.length > MAX_BODY) throw new IOException("body"); return JSON.readTree(b); }
    private static String text(JsonNode n, String f, int max) { String s = n.path(f).asText(null); return s != null && s.length() <= max ? s : null; }
    private static int integer(JsonNode n, String f, int min, int max) { int v = n.path(f).asInt(Integer.MIN_VALUE); if (v < min || v > max) throw new IllegalArgumentException(f); return v; }
    private static ObjectNode obj() { return JSON.createObjectNode(); }
    private static ObjectNode error(String s) { return obj().put("error", s); }
    private static boolean method(HttpExchange x, String m) throws IOException { if (!x.getRequestMethod().equals(m)) { x.getResponseHeaders().set("Allow", m); json(x, 405, error("Metoda niedozwolona.")); return false; } return true; }
    private static String q(HttpExchange x, String k) { String query = x.getRequestURI().getRawQuery(); if (query == null) return null; for (String p : query.split("&")) { String[] a = p.split("=", 2); if (a.length == 2 && a[0].equals(k)) return a[1]; } return null; }
    private static void json(HttpExchange x, int status, JsonNode body) throws IOException { byte[] b = JSON.writeValueAsBytes(body); x.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8"); x.getResponseHeaders().set("Cache-Control", "no-store"); x.sendResponseHeaders(status, b.length); try (var o = x.getResponseBody()) { o.write(b); } }
    private static String env(String k, String d) { String v = System.getenv(k); return v == null || v.isBlank() ? d : v; }
    private static void require(boolean ok, String what) { if (!ok) throw new IllegalArgumentException(what); }
}