package id.launcher.pixellite;

import android.app.Activity;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** A deliberately small, dependency-free HOME launcher for Android 4.4 / API 19. */
public class LauncherActivity extends Activity {
    private static final int BG = Color.rgb(16, 17, 22);
    private static final int TEXT = Color.rgb(232, 234, 240);
    private static final int MUTED = Color.rgb(174, 179, 190);
    private static final int ACCENT = Color.rgb(168, 199, 250);

    private final Handler handler = new Handler();
    private TextView clockView;
    private TextView dateView;
    private GridView appGrid;
    private AppAdapter adapter;
    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            updateClock();
            handler.postDelayed(this, 60000L);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            w.setStatusBarColor(BG);
            w.setNavigationBarColor(BG);
        }
        w.getDecorView().setSystemUiVisibility(0);
        buildLayout();
        loadApps();
    }

    @Override protected void onResume() {
        super.onResume();
        updateClock();
        handler.removeCallbacks(clockTick);
        handler.postDelayed(clockTick, 60000L);
        if (adapter != null) loadApps();
    }

    @Override protected void onPause() {
        handler.removeCallbacks(clockTick);
        super.onPause();
    }

    private int dp(float value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private TextView text(String value, float size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setIncludeFontPadding(true);
        return t;
    }

    private void buildLayout() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setPadding(dp(20), dp(12), dp(20), dp(8));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setGravity(Gravity.CENTER_HORIZONTAL);
        header.setPadding(0, dp(8), 0, dp(12));

        clockView = text("--:--", 42, TEXT);
        clockView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clockView.setGravity(Gravity.CENTER);
        header.addView(clockView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dateView = text("", 14, MUTED);
        dateView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateParams.topMargin = dp(1);
        header.addView(dateView, dateParams);
        root.addView(header);

        TextView search = text("⌕     Search", 15, MUTED);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(18), 0, dp(18), 0);
        search.setBackground(rounded(Color.rgb(38, 41, 49), 28));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        searchParams.setMargins(dp(2), dp(2), dp(2), dp(14));
        root.addView(search, searchParams);
        search.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { launchSearch(); }
        });

        TextView label = text("ALL APPS", 11, ACCENT);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28));
        labelParams.leftMargin = dp(4);
        root.addView(label, labelParams);

        appGrid = new GridView(this);
        appGrid.setNumColumns(GridView.AUTO_FIT);
        appGrid.setColumnWidth(dp(88));
        appGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        appGrid.setVerticalSpacing(dp(8));
        appGrid.setHorizontalSpacing(dp(2));
        appGrid.setSelector(android.R.color.transparent);
        appGrid.setCacheColorHint(Color.TRANSPARENT);
        appGrid.setClipToPadding(false);
        root.addView(appGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
    }

    private void updateClock() {
        Date now = new Date();
        clockView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        dateView.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now));
    }

    private void launchSearch() {
        try {
            Intent i = new Intent(Intent.ACTION_WEB_SEARCH);
            i.putExtra(SearchManager.QUERY, "");
            startActivity(i);
        } catch (ActivityNotFoundException e) {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://www.google.com")));
            } catch (Exception ignored) {
                Toast.makeText(this, "No search or browser app found", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> found = pm.queryIntentActivities(main, 0);
        final ArrayList<AppEntry> apps = new ArrayList<AppEntry>();
        for (ResolveInfo ri : found) {
            if (ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            if (getPackageName().equals(pkg)) continue;
            AppEntry entry = new AppEntry();
            entry.label = ri.loadLabel(pm).toString();
            entry.icon = ri.loadIcon(pm);
            entry.intent = new Intent(Intent.ACTION_MAIN);
            entry.intent.addCategory(Intent.CATEGORY_LAUNCHER);
            entry.intent.setClassName(pkg, ri.activityInfo.name);
            entry.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            apps.add(entry);
        }
        Collections.sort(apps, new Comparator<AppEntry>() {
            @Override public int compare(AppEntry a, AppEntry b) {
                return a.label.toLowerCase(Locale.getDefault()).compareTo(
                        b.label.toLowerCase(Locale.getDefault()));
            }
        });
        adapter = new AppAdapter(apps);
        appGrid.setAdapter(adapter);
    }

    private class AppEntry {
        String label;
        android.graphics.drawable.Drawable icon;
        Intent intent;
    }

    private class AppAdapter extends BaseAdapter {
        private final ArrayList<AppEntry> items;
        AppAdapter(ArrayList<AppEntry> data) { items = data; }
        @Override public int getCount() { return items.size(); }
        @Override public Object getItem(int position) { return items.get(position); }
        @Override public long getItemId(int position) { return position; }

        @Override public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout cell;
            ImageView icon;
            TextView title;
            if (convertView == null) {
                cell = new LinearLayout(LauncherActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(dp(3), dp(7), dp(3), dp(7));
                icon = new ImageView(LauncherActivity.this);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                cell.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
                title = text("", 11, TEXT);
                title.setGravity(Gravity.CENTER);
                title.setMaxLines(2);
                title.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
                titleParams.topMargin = dp(5);
                cell.addView(title, titleParams);
                cell.setTag(new ViewHolder(icon, title));
            } else {
                cell = (LinearLayout) convertView;
            }
            ViewHolder holder = (ViewHolder) cell.getTag();
            final AppEntry item = items.get(position);
            holder.icon.setImageDrawable(item.icon);
            holder.title.setText(item.label);
            cell.setContentDescription(item.label);
            cell.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    try { startActivity(item.intent); }
                    catch (Exception e) {
                        Toast.makeText(LauncherActivity.this, "Unable to open " + item.label,
                                Toast.LENGTH_SHORT).show();
                    }
                }
            });
            return cell;
        }
    }

    private static class ViewHolder {
        final ImageView icon;
        final TextView title;
        ViewHolder(ImageView i, TextView t) { icon = i; title = t; }
    }
}
