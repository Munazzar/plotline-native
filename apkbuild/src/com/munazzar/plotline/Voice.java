package com.munazzar.plotline;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import java.util.ArrayList;
import org.json.JSONObject;

/*
 * Speech to text for the mic buttons. Prefers the phone's on-device recognizer (Android 12+), so audio never
 * leaves the phone. The phone's regular recognizer (usually Google's, may use the network) is used only when the
 * person allowed it in Settings. Results go to window.__voice(id, kind, text): partial · final · end · err · level.
 */
final class Voice {
    static SpeechRecognizer rec; static String cur = ""; static NVoice nat;

    static boolean onDeviceAvailable(MainActivity a) {
        if (Build.VERSION.SDK_INT < 31) return false;
        try { return (Boolean) SpeechRecognizer.class.getMethod("isOnDeviceRecognitionAvailable", android.content.Context.class).invoke(null, a); }
        catch (Exception e) { return false; }
    }

    static String state(MainActivity a) {
        try {
            JSONObject o = new JSONObject();
            o.put("onDevice", onDeviceAvailable(a));
            o.put("any", SpeechRecognizer.isRecognitionAvailable(a));
            o.put("perm", a.checkSelfPermission("android.permission.RECORD_AUDIO") == android.content.pm.PackageManager.PERMISSION_GRANTED);
            return o.toString();
        } catch (Exception e) { return "{}"; }
    }

    static void send(MainActivity a, final String id, final String kind, final String text) {
        if (nat != null && id.startsWith("n")) { final NVoice n = nat; a.runOnUiThread(new Runnable() { public void run() { if (!kind.equals("level") && !kind.equals("ready")) n.on(kind, text == null ? "" : text); } }); return; }
        a.js("window.__voice&&window.__voice(" + JSONObject.quote(id) + "," + JSONObject.quote(kind) + "," + JSONObject.quote(text == null ? "" : text) + ")");
    }

    static void start(final MainActivity a, final String id, final String lang, final boolean allowCloud) {
        a.runOnUiThread(new Runnable() { public void run() {
            try {
                stopNow();
                if (a.checkSelfPermission("android.permission.RECORD_AUDIO") != android.content.pm.PackageManager.PERMISSION_GRANTED) { send(a, id, "err", "perm"); return; }
                boolean od = onDeviceAvailable(a);
                if (od) rec = (SpeechRecognizer) SpeechRecognizer.class.getMethod("createOnDeviceSpeechRecognizer", android.content.Context.class).invoke(null, a);
                else if (allowCloud && SpeechRecognizer.isRecognitionAvailable(a)) rec = SpeechRecognizer.createSpeechRecognizer(a);
                else { send(a, id, "err", od ? "unavailable" : "no_on_device"); return; }
                cur = id;
                rec.setRecognitionListener(new RecognitionListener() {
                    public void onReadyForSpeech(Bundle b) { send(a, id, "ready", ""); }
                    public void onBeginningOfSpeech() { }
                    public void onRmsChanged(float db) { send(a, id, "level", String.valueOf(Math.round(db))); }
                    public void onBufferReceived(byte[] b) { }
                    public void onEndOfSpeech() { }
                    public void onError(int code) { send(a, id, "err", String.valueOf(code)); release(); }
                    public void onResults(Bundle b) { ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); send(a, id, "final", r == null || r.isEmpty() ? "" : r.get(0)); send(a, id, "end", ""); release(); }
                    public void onPartialResults(Bundle b) { ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION); if (r != null && !r.isEmpty()) send(a, id, "partial", r.get(0)); }
                    public void onEvent(int t, Bundle b) { }
                });
                Intent it = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                it.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                it.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
                it.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
                it.putExtra("android.speech.extra.SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS", 2500L);
                it.putExtra("android.speech.extra.SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS", 2000L);
                if (lang != null && !lang.isEmpty()) it.putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang);
                rec.startListening(it);
            } catch (Exception e) { send(a, id, "err", String.valueOf(e.getMessage())); release(); }
        } });
    }

    static void stop(final MainActivity a) { a.runOnUiThread(new Runnable() { public void run() { try { if (rec != null) rec.stopListening(); } catch (Exception ignored) { } } }); }

    static void stopNow() { try { if (rec != null) { rec.cancel(); rec.destroy(); } } catch (Exception ignored) { } rec = null; }
    static void release() { try { if (rec != null) rec.destroy(); } catch (Exception ignored) { } rec = null; }
}
