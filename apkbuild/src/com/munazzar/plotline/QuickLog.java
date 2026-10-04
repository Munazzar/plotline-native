package com.munazzar.plotline;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/* The small box the Threads widget opens: pick a thread (or start a new one), type or speak, pick what kind of
   update it is, save. Saved lines go into the widget queue; the app adds them to the thread when it next opens. */
public class QuickLog extends Activity {
    static final String[][] TAGS = {{"note", "💭"}, {"idea", "💡"}, {"prog", "🚧"}, {"block", "⛔"}, {"done", "✅"}};
    static final int REQ_VOICE = 71;
    final List<String> ids = new ArrayList<>(); final List<String> names = new ArrayList<>();
    Spinner pick; EditText title, text; String kind = "note"; final List<TextView> tagViews = new ArrayList<>();
    boolean dark; int fg, muted, card, accent = 0xFFFFB547, onAccent = 0xFF1B1300;

    int dp(float v) { return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics())); }

    GradientDrawable round(int color, float r, int stroke) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(r)); if (stroke != 0) g.setStroke(dp(1), stroke); return g; }

    TextView btn(String s, boolean pri) {
        TextView b = new TextView(this); b.setText(s); b.setTextSize(15); b.setTypeface(Typeface.DEFAULT_BOLD); b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(10), dp(16), dp(10)); b.setTextColor(pri ? onAccent : fg);
        b.setBackground(pri ? round(accent, 14, 0) : round(Color.TRANSPARENT, 14, muted)); return b;
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        /* a tap on a thread card in the widget: just open that thread in the app */
        String route = getIntent().getStringExtra("route");
        if (route != null && route.length() > 0) {
            Intent o = new Intent(this, MainActivity.class);
            o.setAction("com.munazzar.plotline.ROUTE." + route); o.putExtra("route", route);
            o.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(o); finish(); overridePendingTransition(0, 0); return;
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        W.loadTheme(this);
        if (W.THEMED) { dark = !W.LIGHT; fg = W.TEXT; muted = W.MUTED; card = W.blend(W.BG | 0xFF000000, W.TEXT, 0.04f); accent = W.ACCENT; onAccent = W.ON; }
        else {
        dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        fg = dark ? 0xFFECEAE3 : 0xFF141A2B; muted = dark ? 0xFF8F96AC : 0xFF5D6580; card = dark ? 0xFF171D33 : 0xFFFFFFFF;
        }
        Intent in = getIntent(); String want = in.getStringExtra("thread"); boolean voice = in.getBooleanExtra("voice", false);
        JSONArray ts = W.data(this).optJSONArray("thr");
        if (ts != null) for (int i = 0; i < ts.length(); i++) { JSONObject o = ts.optJSONObject(i); if (o == null) continue; ids.add(o.optString("id")); names.add(o.optString("e", "🧵") + "  " + o.optString("t")); }
        ids.add(""); names.add("＋  New thread");

        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(18), dp(20), dp(16));
        root.setBackground(round(card, 26, 0));
        TextView h = new TextView(this); h.setText("Add to a thread"); h.setTextSize(19); h.setTypeface(Typeface.DEFAULT_BOLD); h.setTextColor(fg); root.addView(h);

        pick = new Spinner(this);
        ArrayAdapter<String> ad = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, names) {
            @Override public View getView(int p, View cv, ViewGroup pa) { TextView t = (TextView) super.getView(p, cv, pa); t.setTextColor(fg); t.setTextSize(15); return t; }
        };
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        pick.setAdapter(ad);
        int sel = want == null ? 0 : (want.isEmpty() ? ids.size() - 1 : Math.max(0, ids.indexOf(want)));
        pick.setSelection(sel);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2); lp.topMargin = dp(10); root.addView(pick, lp);

        title = new EditText(this); title.setHint("Name the thread"); title.setSingleLine(true); title.setTextColor(fg); title.setHintTextColor(muted);
        title.setBackground(round(dark ? 0x14FFFFFF : 0x0A000000, 14, 0)); title.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams tp = new LinearLayout.LayoutParams(-1, -2); tp.topMargin = dp(8); root.addView(title, tp);

        LinearLayout tags = new LinearLayout(this); tags.setOrientation(LinearLayout.HORIZONTAL);
        for (final String[] t : TAGS) {
            final TextView tv = new TextView(this); tv.setText(t[1]); tv.setTextSize(18); tv.setGravity(Gravity.CENTER); tv.setPadding(0, dp(6), 0, dp(6));
            tv.setContentDescription(t[0]);
            tv.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { kind = t[0]; paintTags(); } });
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, -2, 1); p.setMarginEnd(dp(6)); tags.addView(tv, p); tagViews.add(tv);
        }
        LinearLayout.LayoutParams gp = new LinearLayout.LayoutParams(-1, -2); gp.topMargin = dp(12); root.addView(tags, gp); paintTags();

        text = new EditText(this); text.setHint("What’s new? Type, or tap 🎤 to speak"); text.setMinLines(3); text.setMaxLines(8); text.setGravity(Gravity.TOP | Gravity.START);
        text.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        text.setTextColor(fg); text.setHintTextColor(muted); text.setBackground(round(dark ? 0x14FFFFFF : 0x0A000000, 16, 0)); text.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams xp = new LinearLayout.LayoutParams(-1, -2); xp.topMargin = dp(10); root.addView(text, xp);

        LinearLayout bar = new LinearLayout(this); bar.setOrientation(LinearLayout.HORIZONTAL); bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView mic = btn("🎤  Speak", false); mic.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { listen(); } });
        bar.addView(mic, new LinearLayout.LayoutParams(-2, -2));
        View sp = new View(this); bar.addView(sp, new LinearLayout.LayoutParams(0, 1, 1));
        TextView cancel = btn("Cancel", false); cancel.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { finish(); } });
        bar.addView(cancel, new LinearLayout.LayoutParams(-2, -2));
        TextView save = btn("Save", true); save.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { save(); } });
        LinearLayout.LayoutParams sv = new LinearLayout.LayoutParams(-2, -2); sv.setMarginStart(dp(8)); bar.addView(save, sv);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2); bp.topMargin = dp(14); root.addView(bar, bp);

        pick.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) { title.setVisibility(ids.get(pos).isEmpty() ? View.VISIBLE : View.GONE); }
            public void onNothingSelected(android.widget.AdapterView<?> p) { }
        });
        title.setVisibility(ids.get(sel).isEmpty() ? View.VISIBLE : View.GONE);
        setContentView(root);
        Window w = getWindow();
        w.setLayout(Math.min(getResources().getDisplayMetrics().widthPixels - dp(24), dp(520)), WindowManager.LayoutParams.WRAP_CONTENT);
        w.setBackgroundDrawableResource(android.R.color.transparent);
        (ids.get(sel).isEmpty() ? title : text).requestFocus();
        if (voice) listen();
    }

    void paintTags() {
        for (int i = 0; i < tagViews.size(); i++) {
            boolean on = TAGS[i][0].equals(kind);
            tagViews.get(i).setBackground(on ? round((accent & 0x00FFFFFF) | (dark ? 0x33000000 : 0x22000000), 12, accent) : round(Color.TRANSPARENT, 12, dark ? 0x22FFFFFF : 0x22000000));
        }
    }

    void listen() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say your update");
        try { startActivityForResult(i, REQ_VOICE); }
        catch (Exception e) { Toast.makeText(this, "Speech input isn’t available on this phone. Type it instead.", Toast.LENGTH_LONG).show(); }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_VOICE || res != RESULT_OK || data == null) return;
        ArrayList<String> r = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (r == null || r.isEmpty()) return;
        String said = r.get(0).trim(); if (said.isEmpty()) return;
        String cur = text.getText().toString().trim();
        String out = cur.isEmpty() ? Character.toUpperCase(said.charAt(0)) + said.substring(1) : cur + " " + said;
        text.setText(out); text.setSelection(out.length());
        if (ids.get(pick.getSelectedItemPosition()).isEmpty() && title.getText().toString().trim().isEmpty())
            title.setText(out.length() > 50 ? out.substring(0, 50).trim() + "…" : out);
    }

    void save() {
        String x = text.getText().toString().trim(), id = ids.get(pick.getSelectedItemPosition()), t = title.getText().toString().trim();
        long now = System.currentTimeMillis();
        try {
            JSONObject a = new JSONObject().put("x", x).put("kind", kind).put("t", now).put("qid", "w" + Long.toString(now, 36));
            if (id.isEmpty()) {
                if (t.isEmpty()) t = x.length() > 50 ? x.substring(0, 50).trim() + "…" : x;
                if (t.isEmpty()) { Toast.makeText(this, "Name the thread or write something first", Toast.LENGTH_SHORT).show(); return; }
                a.put("k", "thrnew").put("title", t).put("nid", "w" + Long.toString(now, 36) + "n");
            } else {
                if (x.isEmpty()) { Toast.makeText(this, "Write or say something first", Toast.LENGTH_SHORT).show(); return; }
                a.put("k", "thr").put("id", id);
            }
            W.enqueue(this, a);
        } catch (Exception e) { Toast.makeText(this, "Couldn’t save that", Toast.LENGTH_SHORT).show(); return; }
        ThreadsWidget.updateAll(this);
        InputMethodManager im = getSystemService(InputMethodManager.class); if (im != null) im.hideSoftInputFromWindow(text.getWindowToken(), 0);
        Toast.makeText(this, id.isEmpty() ? "Thread started · it’s in Plotline next time you open it" : "Added · it’s in Plotline next time you open it", Toast.LENGTH_SHORT).show();
        finish();
    }
}
