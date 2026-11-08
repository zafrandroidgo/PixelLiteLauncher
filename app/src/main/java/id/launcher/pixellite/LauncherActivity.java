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
import android.view.MotionEvent;
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

/** Lightweight, dependency-free Pixel-inspired HOME launcher for Android 4.4 / API 19. */
public class LauncherActivity extends Activity {
    private static final int BG = Color.rgb(16, 17, 22);
    private static final int SURFACE = Color.rgb(38, 41, 49);
    private static final int TEXT = Color.rgb(232, 234, 240);
    private static final int MUTED = Color.rgb(174, 179, 190);
    private static final int ACCENT = Color.rgb(168, 199, 250);

    private final Handler handler = new Handler();
    private TextView clockView;
    private TextView dateView;
    private GridView appGrid;
    private LinearLayout homePanel;
    private LinearLayout drawerPanel;
    private AppAdapter adapter;
    private boolean drawerOpen = false;
    private float downY;

    private final Runnable clockTick = new Runnable() {
        @Override public void run() {
            if (clockView != null) updateClock();
            handler.postDelayed(this, 60000L);
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        Window w = getWindow();
        // Ask Android to draw the current system wallpaper behind the launcher.
        w.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            w.setStatusBarColor(BG);
            w.setNavigationBarColor(BG);
        }
        w.getDecorView().setSystemUiVisibility(0);
        buildLayout();
        loadApps();
        showHome();
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

    @Override public void onBackPressed() {
        if (drawerOpen) showHome();
        else super.onBackPressed();
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

    private LinearLayout vertical() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        return l;
    }

    private void buildLayout() {
        LinearLayout root = vertical();
        root.setBackgroundColor(Color.TRANSPARENT);
        root.setPadding(dp(20), dp(12), dp(20), dp(10));

        // HOME: clock, date, search, dock and app-drawer affordance only.
        homePanel = vertical();
        homePanel.setBackgroundColor(Color.TRANSPARENT);
        homePanel.setGravity(Gravity.CENTER_HORIZONTAL);
        homePanel.setPadding(0, dp(12), 0, dp(8));

        View topSpace = new View(this);
        homePanel.addView(topSpace, new LinearLayout.LayoutParams(1, 0, 1f));

        clockView = text("--:--", 58, TEXT);
        clockView.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clockView.setGravity(Gravity.CENTER);
        homePanel.addView(clockView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dateView = text("", 15, MUTED);
        dateView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateParams.topMargin = dp(3);
        homePanel.addView(dateView, dateParams);

        TextView search = text("⌕     Search", 15, MUTED);
        search.setGravity(Gravity.CENTER_VERTICAL);
        search.setPadding(dp(18), 0, dp(18), 0);
        search.setBackground(rounded(SURFACE, 28));
        LinearLayout.LayoutParams searchParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(48));
        searchParams.setMargins(dp(2), dp(28), dp(2), dp(18));
        homePanel.addView(search, searchParams);
        search.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { launchSearch(); }
        });

        View middleSpace = new View(this);
        homePanel.addView(middleSpace, new LinearLayout.LayoutParams(1, 0, 1f));

        LinearLayout dock = new LinearLayout(this);
        dock.setGravity(Gravity.CENTER);
        dock.setOrientation(LinearLayout.HORIZONTAL);
        dock.setPadding(dp(5), dp(8), dp(5), dp(8));
        dock.setBackground(rounded(Color.rgb(29, 31, 38), 24));
        addDockShortcut(dock, "Browser", "com.android.browser", "🌐");
        addDockShortcut(dock, "Camera", "com.sec.android.app.camera", "📷");
        addDockShortcut(dock, "Gallery", "com.sec.android.gallery3d", "▧");
        addDockShortcut(dock, "Settings", "com.android.settings", "⚙");
        LinearLayout.LayoutParams dockParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(66));
        dockParams.bottomMargin = dp(12);
        homePanel.addView(dock, dockParams);

        LinearLayout homeActions = new LinearLayout(this);
        homeActions.setOrientation(LinearLayout.HORIZONTAL);
        homeActions.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams actionsParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        actionsParams.setMargins(dp(8), 0, dp(8), dp(4));
        homePanel.addView(homeActions, actionsParams);

        TextView allApps = text("▲  ALL APPS", 12, ACCENT);
        allApps.setGravity(Gravity.CENTER);
        allApps.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        allApps.setBackground(rounded(SURFACE, 22));
        homeActions.addView(allApps, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        allApps.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showDrawer(); }
        });

        TextView wallpaper = text("WALLPAPER", 12, TEXT);
        wallpaper.setGravity(Gravity.CENTER);
        wallpaper.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        wallpaper.setBackground(rounded(SURFACE, 22));
        LinearLayout.LayoutParams wallpaperParams = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1f);
        wallpaperParams.leftMargin = dp(8);
        homeActions.addView(wallpaper, wallpaperParams);
        wallpaper.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { launchWallpaperPicker(); }
        });

        homePanel.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downY = event.getY();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    float delta = downY - event.getY();
                    if (delta > dp(55)) showDrawer();
                    return true;
                }
                return true;
            }
        });

        // APP DRAWER: hidden until user taps ALL APPS or swipes upward.
        drawerPanel = vertical();
        drawerPanel.setBackgroundColor(BG);
        drawerPanel.setVisibility(View.GONE);

        LinearLayout drawerHeader = new LinearLayout(this);
        drawerHeader.setGravity(Gravity.CENTER_VERTICAL);
        drawerHeader.setOrientation(LinearLayout.HORIZONTAL);
        TextView back = text("‹", 34, TEXT);
        back.setGravity(Gravity.CENTER);
        drawerHeader.addView(back, new LinearLayout.LayoutParams(dp(42), dp(48)));
        back.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showHome(); }
        });
        TextView drawerTitle = text("All apps", 20, TEXT);
        drawerHeader.addView(drawerTitle, new LinearLayout.LayoutParams(0, dp(48), 1f));
        drawerPanel.addView(drawerHeader);

        TextView hint = text("Swipe down or press Back to return", 12, MUTED);
        hint.setPadding(dp(8), 0, 0, dp(8));
        drawerPanel.addView(hint);

        appGrid = new GridView(this);
        appGrid.setNumColumns(GridView.AUTO_FIT);
        appGrid.setColumnWidth(dp(88));
        appGrid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        appGrid.setVerticalSpacing(dp(8));
        appGrid.setHorizontalSpacing(dp(2));
        appGrid.setSelector(android.R.color.transparent);
        appGrid.setCacheColorHint(Color.TRANSPARENT);
        appGrid.setClipToPadding(false);
        drawerPanel.addView(appGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        drawerPanel.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    downY = event.getY();
                    return true;
                }
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if (event.getY() - downY > dp(55)) showHome();
                    return true;
                }
                return true;
            }
        });

        root.addView(homePanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        root.addView(drawerPanel, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void addDockShortcut(LinearLayout dock, String label, String packageName, String fallback) {
        PackageManager pm = getPackageManager();
        Intent launch = pm.getLaunchIntentForPackage(packageName);
        if (launch == null) return;
        LinearLayout item = new LinearLayout(this);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setGravity(Gravity.CENTER);
        TextView glyph = text(fallback, 23, TEXT);
        glyph.setGravity(Gravity.CENTER);
        item.addView(glyph, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(31)));
        TextView name = text(label, 9, MUTED);
        name.setGravity(Gravity.CENTER);
        item.addView(name, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(19)));
        dock.addView(item, new LinearLayout.LayoutParams(0, dp(52), 1f));
        final Intent intent = launch;
        item.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try { startActivity(intent); } catch (Exception ignored) { }
            }
        });
    }

    private void showHome() {
        drawerOpen = false;
        homePanel.setVisibility(View.VISIBLE);
        drawerPanel.setVisibility(View.GONE);
    }

    private void showDrawer() {
        drawerOpen = true;
        homePanel.setVisibility(View.GONE);
        drawerPanel.setVisibility(View.VISIBLE);
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void updateClock() {
        Date now = new Date();
        clockView.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
        dateView.setText(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(now));
    }

    private void launchWallpaperPicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            startActivity(Intent.createChooser(intent, "Choose wallpaper"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "Wallpaper picker is not available on this device",
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Unable to open wallpaper picker",
                    Toast.LENGTH_SHORT).show();
        }
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
        if (appGrid == null) return;
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
            if (convertView == null) {
                cell = new LinearLayout(LauncherActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setGravity(Gravity.CENTER_HORIZONTAL);
                cell.setPadding(dp(3), dp(7), dp(3), dp(7));
                ImageView icon = new ImageView(LauncherActivity.this);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                cell.addView(icon, new LinearLayout.LayoutParams(dp(46), dp(46)));
                TextView title = text("", 11, TEXT);
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
