package com.iappsan.tvboxlauncher;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Bundle;
import android.os.StatFs;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.text.Collator;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String PREFERENCES = "launcher_preferences";
    private static final String FAVORITES_KEY = "favorites";
    private static final String HIDDEN_APPS_KEY = "hidden_apps";
    private static final String THEME_KEY = "theme";
    private static final String[] FEATURED_APP_NAMES = {
            "youtube", "netflix", "prime video", "disney", "max", "plex", "kodi", "spotify"
    };
    private static final ThemePalette[] THEMES = {
            new ThemePalette("Océano", 0xFF101D2A, 0xFF0B1119, 0xFF193041,
                    0xFF1F534F, 0xFF23645B, 0xFF18232F, 0xFF2B414D,
                    0xFF80E0D3, 0xFF0D2527, 0xFF30414D, 0xFF273745,
                    0xFF274457, 0xFF1F4F4E),
            new ThemePalette("Bosque", 0xFF101F1A, 0xFF09120F, 0xFF19372D,
                    0xFF286248, 0xFF327252, 0xFF182A23, 0xFF29483A,
                    0xFF9BE3B0, 0xFF173224, 0xFF354D41, 0xFF26392D,
                    0xFF264D3A, 0xFF286148),
            new ThemePalette("Atardecer", 0xFF251923, 0xFF120E18, 0xFF44273B,
                    0xFF704052, 0xFF8A4F5C, 0xFF2B202C, 0xFF49303D,
                    0xFFFFBE91, 0xFF3C241B, 0xFF55404A, 0xFF3B2937,
                    0xFF533345, 0xFF71404D),
            new ThemePalette("Grafito", 0xFF1C2025, 0xFF101216, 0xFF303943,
                    0xFF434F5C, 0xFF596674, 0xFF23282F, 0xFF3A424C,
                    0xFFB8D5F5, 0xFF1C2A3A, 0xFF434B56, 0xFF303943,
                    0xFF354452, 0xFF465667)
    };

    private final Handler clockHandler = new Handler(Looper.getMainLooper());
    private final Runnable clockUpdater = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, 60_000);
        }
    };

    private TextView clockText;
    private TextView dateText;
    private ThemePalette currentTheme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        showHome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        clockHandler.removeCallbacks(clockUpdater);
        clockHandler.post(clockUpdater);
        showHome();
    }

    @Override
    protected void onPause() {
        clockHandler.removeCallbacks(clockUpdater);
        super.onPause();
    }

    private void showHome() {
        currentTheme = getSelectedTheme();
        List<ResolveInfo> apps = getLaunchableApps();
        ResolveInfo featuredApp = getFeaturedApp(apps);

        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setGravity(Gravity.CENTER_VERTICAL);
        page.setClipChildren(false);
        page.setClipToPadding(false);
        page.setPadding(dp(48), dp(24), dp(48), dp(24));
        page.setBackground(createBackdrop());

        LinearLayout header = createHeader();
        page.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)));
        addSpace(page, 22);

        View hero = createHeroCard(featuredApp);
        page.addView(hero, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(226)));
        addSpace(page, 26);

        LinearLayout sectionHeader = createAppsSectionHeader(apps.size());
        page.addView(sectionHeader, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(42)));
        addSpace(page, 10);

        View firstAppCard = null;
        if (apps.isEmpty()) {
            TextView empty = createText(getString(R.string.empty_apps), 18, Color.WHITE, false);
            page.addView(empty, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(150)));
        } else {
            HorizontalScrollView appScroll = new HorizontalScrollView(this);
            appScroll.setHorizontalScrollBarEnabled(false);
            appScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
            appScroll.setClipChildren(false);
            appScroll.setClipToPadding(false);

            LinearLayout appRow = new LinearLayout(this);
            appRow.setOrientation(LinearLayout.HORIZONTAL);
            appRow.setClipChildren(false);
            appRow.setClipToPadding(false);
            for (ResolveInfo app : apps) {
                View card = createAppCard(app);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(184), dp(148));
                params.rightMargin = dp(14);
                appRow.addView(card, params);
                if (firstAppCard == null) {
                    firstAppCard = card;
                }
            }
            appScroll.addView(appRow);
            page.addView(appScroll, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(158)));
        }

        TextView         footer = createText(getString(R.string.remote_hint), 13,
                Color.rgb(143, 160, 174), false);
        LinearLayout.LayoutParams footerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(20));
        footerParams.topMargin = dp(18);
        page.addView(footer, footerParams);

        setContentView(page);
        View initialFocus = featuredApp != null ? hero : firstAppCard;
        if (featuredApp != null && firstAppCard != null) {
            hero.setNextFocusDownId(firstAppCard.getId());
            firstAppCard.setNextFocusUpId(hero.getId());
        }
        if (initialFocus != null) {
            page.post(initialFocus::requestFocus);
        }
        updateClock();
    }

    private LinearLayout createHeader() {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView logo = new ImageView(this);
        logo.setImageResource(R.drawable.ic_launcher);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        header.addView(logo, new LinearLayout.LayoutParams(dp(40), dp(40)));

        LinearLayout brand = new LinearLayout(this);
        brand.setOrientation(LinearLayout.VERTICAL);
        brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = createText(getString(R.string.app_name), 21, Color.WHITE, true);
        TextView tagline = createText(getString(R.string.brand_tagline), 11,
                Color.rgb(143, 160, 174), true);
        brand.addView(title);
        brand.addView(tagline);
        LinearLayout.LayoutParams brandParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT);
        brandParams.leftMargin = dp(12);
        header.addView(brand, brandParams);

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));

        LinearLayout time = new LinearLayout(this);
        time.setOrientation(LinearLayout.VERTICAL);
        time.setGravity(Gravity.CENTER_VERTICAL | Gravity.END);
        clockText = createText("", 22, Color.WHITE, true);
        dateText = createText("", 12, Color.rgb(166, 181, 193), false);
        time.addView(clockText);
        time.addView(dateText);
        header.addView(time);

        LinearLayout settings = new LinearLayout(this);
        settings.setOrientation(LinearLayout.HORIZONTAL);
        settings.setGravity(Gravity.CENTER);
        settings.setFocusable(true);
        settings.setFocusableInTouchMode(true);
        settings.setClickable(true);
        settings.setPadding(dp(16), dp(10), dp(16), dp(10));
        settings.setBackground(createSettingsBackground(false));
        settings.setContentDescription(getString(R.string.settings));

        ImageView settingsIcon = new ImageView(this);
        settingsIcon.setImageResource(android.R.drawable.ic_menu_manage);
        settingsIcon.setColorFilter(Color.rgb(211, 224, 231));
        settings.addView(settingsIcon, new LinearLayout.LayoutParams(dp(18), dp(18)));
        TextView settingsLabel = createText(getString(R.string.settings), 14, Color.WHITE, true);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.leftMargin = dp(8);
        settings.addView(settingsLabel, labelParams);

        LinearLayout.LayoutParams settingsParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(44));
        settingsParams.leftMargin = dp(28);
        header.addView(settings, settingsParams);
        settings.setOnFocusChangeListener((view, focused) ->
                view.setBackground(createSettingsBackground(focused)));
        settings.setOnClickListener(view -> showLauncherSettings());
        return header;
    }

    private View createHeroCard(ResolveInfo app) {
        LinearLayout hero = new LinearLayout(this);
        hero.setId(View.generateViewId());
        hero.setOrientation(LinearLayout.HORIZONTAL);
        hero.setGravity(Gravity.CENTER_VERTICAL);
        hero.setClipChildren(false);
        hero.setPadding(dp(32), dp(24), dp(28), dp(24));
        hero.setBackground(createHeroBackground(false));

        LinearLayout copy = new LinearLayout(this);
        copy.setOrientation(LinearLayout.VERTICAL);
        copy.setGravity(Gravity.CENTER_VERTICAL);

        TextView eyebrow = createText(getString(R.string.hero_eyebrow), 12,
                currentTheme.accent, true);
        copy.addView(eyebrow);

        TextView title = createText(getString(R.string.hero_title), 32, Color.WHITE, true);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        titleParams.topMargin = dp(8);
        copy.addView(title, titleParams);

        TextView subtitle = createText(getString(R.string.hero_subtitle), 16,
                Color.rgb(211, 225, 230), false);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.topMargin = dp(8);
        copy.addView(subtitle, subtitleParams);

        String appName = app == null ? null : app.loadLabel(getPackageManager()).toString();
        TextView action = createText(
                appName == null
                        ? getString(R.string.hero_empty_action)
                        : getString(R.string.open_app, appName.toUpperCase(Locale.getDefault())),
                13, currentTheme.textOnAccent, true);
        action.setGravity(Gravity.CENTER);
        action.setPadding(dp(16), dp(10), dp(16), dp(10));
        action.setBackground(createActionPill());
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, dp(38));
        actionParams.topMargin = dp(18);
        copy.addView(action, actionParams);

        hero.addView(copy, new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.MATCH_PARENT, 1));

        FrameLayout featureBadge = createFeatureBadge(app);
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(dp(202), dp(174));
        badgeParams.leftMargin = dp(24);
        hero.addView(featureBadge, badgeParams);

        if (app != null) {
            hero.setFocusable(true);
            hero.setFocusableInTouchMode(true);
            hero.setClickable(true);
            hero.setContentDescription(getString(R.string.open_app, appName));
            hero.setOnFocusChangeListener((view, focused) -> {
                view.setBackground(createHeroBackground(focused));
                view.animate().scaleX(focused ? 1.015f : 1f)
                        .scaleY(focused ? 1.015f : 1f).setDuration(140).start();
            });
            hero.setOnClickListener(view -> launchApp(app));
        }
        return hero;
    }

    private FrameLayout createFeatureBadge(ResolveInfo app) {
        FrameLayout badge = new FrameLayout(this);
        badge.setBackground(createFeatureBadgeBackground());

        View glow = new View(this);
        GradientDrawable glowShape = new GradientDrawable();
        glowShape.setShape(GradientDrawable.OVAL);
        glowShape.setColor(currentTheme.heroMiddle);
        glow.setBackground(glowShape);
        FrameLayout.LayoutParams glowParams = new FrameLayout.LayoutParams(dp(138), dp(138),
                Gravity.TOP | Gravity.END);
        glowParams.rightMargin = dp(-36);
        glowParams.topMargin = dp(-48);
        badge.addView(glow, glowParams);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        ImageView icon = new ImageView(this);
        if (app != null) {
            icon.setImageDrawable(app.loadIcon(getPackageManager()));
        } else {
            icon.setImageResource(R.drawable.ic_launcher);
        }
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        content.addView(icon, new LinearLayout.LayoutParams(dp(68), dp(68)));

        TextView name = createText(
                app == null ? getString(R.string.app_name) : app.loadLabel(getPackageManager()).toString(),
                16, Color.WHITE, true);
        name.setGravity(Gravity.CENTER);
        name.setMaxLines(1);
        name.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        nameParams.topMargin = dp(10);
        content.addView(name, nameParams);
        TextView caption = createText(getString(R.string.featured_label), 10,
                currentTheme.accent, true);
        caption.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams captionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        captionParams.topMargin = dp(3);
        content.addView(caption, captionParams);

        FrameLayout.LayoutParams contentParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        badge.addView(content, contentParams);
        return badge;
    }

    private LinearLayout createAppsSectionHeader(int appCount) {
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = createText(getString(R.string.apps_title), 21, Color.WHITE, true);
        header.addView(title);

        TextView count = createText(getString(R.string.apps_ready, appCount), 13,
                Color.rgb(143, 160, 174), false);
        LinearLayout.LayoutParams countParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        countParams.leftMargin = dp(14);
        header.addView(count, countParams);

        View spacer = new View(this);
        header.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1));
        TextView hint = createText(getString(R.string.apps_hint), 13,
                Color.rgb(143, 160, 174), false);
        header.addView(hint);
        return header;
    }

    private View createAppCard(ResolveInfo app) {
        LinearLayout card = new LinearLayout(this);
        card.setId(View.generateViewId());
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setFocusable(true);
        card.setFocusableInTouchMode(true);
        card.setClickable(true);
        card.setPadding(dp(12), dp(12), dp(12), dp(10));
        card.setBackground(createAppCardBackground(false));
        card.setElevation(dp(2));

        FrameLayout iconFrame = new FrameLayout(this);
        iconFrame.setBackground(createIconBackground());
        ImageView icon = new ImageView(this);
        icon.setImageDrawable(app.loadIcon(getPackageManager()));
        icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams iconParams = new FrameLayout.LayoutParams(dp(44), dp(44),
                Gravity.CENTER);
        iconFrame.addView(icon, iconParams);
        card.addView(iconFrame, new LinearLayout.LayoutParams(dp(66), dp(66)));

        TextView label = createText(app.loadLabel(getPackageManager()).toString(),
                15, Color.WHITE, true);
        label.setGravity(Gravity.CENTER);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams labelParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        labelParams.topMargin = dp(10);
        card.addView(label, labelParams);

        card.setContentDescription(app.loadLabel(getPackageManager()));
        card.setOnFocusChangeListener((view, focused) -> {
            view.setBackground(createAppCardBackground(focused));
            view.setElevation(dp(focused ? 12 : 2));
            view.animate().scaleX(focused ? 1.06f : 1f)
                    .scaleY(focused ? 1.06f : 1f).setDuration(140).start();
        });
        card.setOnClickListener(view -> launchApp(app));
        card.setOnLongClickListener(view -> {
            showAppOptions(app);
            return true;
        });
        return card;
    }

    private List<ResolveInfo> getLaunchableApps() {
        Set<String> hiddenApps = getPreferences().getStringSet(HIDDEN_APPS_KEY, Collections.emptySet());
        List<ResolveInfo> apps = getAllLaunchableApps();
        Iterator<ResolveInfo> iterator = apps.iterator();
        while (iterator.hasNext()) {
            ResolveInfo app = iterator.next();
            if (hiddenApps.contains(app.activityInfo.packageName)) {
                iterator.remove();
            }
        }
        sortApps(apps);
        return apps;
    }

    private List<ResolveInfo> getAllLaunchableApps() {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> apps = new ArrayList<>();
        addAppsForCategory(packageManager, Intent.CATEGORY_LAUNCHER, apps);
        addAppsForCategory(packageManager, Intent.CATEGORY_LEANBACK_LAUNCHER, apps);

        String ownPackage = getPackageName();
        Iterator<ResolveInfo> iterator = apps.iterator();
        while (iterator.hasNext()) {
            ResolveInfo app = iterator.next();
            if (app.activityInfo == null || ownPackage.equals(app.activityInfo.packageName)) {
                iterator.remove();
            }
        }
        return apps;
    }

    private void sortApps(List<ResolveInfo> apps) {
        Set<String> favorites = getPreferences().getStringSet(FAVORITES_KEY, Collections.emptySet());
        PackageManager packageManager = getPackageManager();
        Collator collator = Collator.getInstance(Locale.getDefault());
        Collections.sort(apps, (left, right) -> {
            int leftRank = favorites.contains(left.activityInfo.packageName) ? 0 : 1;
            int rightRank = favorites.contains(right.activityInfo.packageName) ? 0 : 1;
            int favoriteOrder = Integer.compare(leftRank, rightRank);
            return favoriteOrder != 0 ? favoriteOrder : collator.compare(
                    left.loadLabel(packageManager).toString(),
                    right.loadLabel(packageManager).toString());
        });
    }

    private ResolveInfo getFeaturedApp(List<ResolveInfo> apps) {
        PackageManager packageManager = getPackageManager();
        Set<String> favorites = getPreferences().getStringSet(FAVORITES_KEY, Collections.emptySet());
        for (ResolveInfo app : apps) {
            if (favorites.contains(app.activityInfo.packageName)) {
                return app;
            }
        }
        for (String preferredName : FEATURED_APP_NAMES) {
            for (ResolveInfo app : apps) {
                String label = app.loadLabel(packageManager).toString().toLowerCase(Locale.ROOT);
                if (label.contains(preferredName)) {
                    return app;
                }
            }
        }
        return apps.isEmpty() ? null : apps.get(0);
    }

    private void addAppsForCategory(
            PackageManager packageManager, String category, List<ResolveInfo> apps) {
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(category);
        for (ResolveInfo app : packageManager.queryIntentActivities(query, 0)) {
            boolean alreadyIncluded = false;
            for (ResolveInfo existing : apps) {
                if (existing.activityInfo != null && app.activityInfo != null
                        && existing.activityInfo.packageName.equals(app.activityInfo.packageName)
                        && existing.activityInfo.name.equals(app.activityInfo.name)) {
                    alreadyIncluded = true;
                    break;
                }
            }
            if (!alreadyIncluded) {
                apps.add(app);
            }
        }
    }

    private void launchApp(ResolveInfo app) {
        if (app.activityInfo == null) {
            Toast.makeText(this, R.string.launch_error, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setClassName(app.activityInfo.packageName, app.activityInfo.name);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.launch_error, Toast.LENGTH_SHORT).show();
        }
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences(PREFERENCES, MODE_PRIVATE);
    }

    private ThemePalette getSelectedTheme() {
        String themeName = getPreferences().getString(THEME_KEY, THEMES[0].name);
        for (ThemePalette theme : THEMES) {
            if (theme.name.equals(themeName)) {
                return theme;
            }
        }
        return THEMES[0];
    }

    private void showLauncherSettings() {
        String[] options = {
                getString(R.string.wifi_settings),
                getString(R.string.bluetooth_settings),
                getString(R.string.display_settings),
                getString(R.string.device_information),
                getString(R.string.appearance_settings),
                getString(R.string.hidden_apps),
                getString(R.string.android_settings)
        };
        new AlertDialog.Builder(this)
                .setTitle(R.string.settings)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openSystemSettings(Settings.ACTION_WIFI_SETTINGS);
                            break;
                        case 1:
                            openSystemSettings(Settings.ACTION_BLUETOOTH_SETTINGS);
                            break;
                        case 2:
                            openSystemSettings(Settings.ACTION_DISPLAY_SETTINGS);
                            break;
                        case 3:
                            showDeviceInformation();
                            break;
                        case 4:
                            showThemeChooser();
                            break;
                        case 5:
                            showHiddenApps();
                            break;
                        case 6:
                            openSystemSettings(Settings.ACTION_SETTINGS);
                            break;
                        default:
                            break;
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void openSystemSettings(String action) {
        try {
            startActivity(new Intent(action));
        } catch (ActivityNotFoundException exception) {
            Toast.makeText(this, R.string.launch_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void showThemeChooser() {
        String[] names = new String[THEMES.length];
        int selected = 0;
        for (int i = 0; i < THEMES.length; i++) {
            names[i] = THEMES[i].name;
            if (THEMES[i].name.equals(currentTheme.name)) {
                selected = i;
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_theme)
                .setSingleChoiceItems(names, selected, (dialog, which) -> {
                    getPreferences().edit().putString(THEME_KEY, THEMES[which].name).apply();
                    dialog.dismiss();
                    showHome();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showDeviceInformation() {
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(memoryInfo);

        StatFs storage = new StatFs(Environment.getDataDirectory().getPath());
        long totalStorage = storage.getTotalBytes();
        long freeStorage = storage.getAvailableBytes();
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        String deviceName = manufacturer.equalsIgnoreCase(model)
                ? model : manufacturer + " " + model;
        String info = getString(R.string.device_info_text,
                deviceName,
                Build.VERSION.RELEASE,
                Build.VERSION.SDK_INT,
                width,
                height,
                Formatter.formatFileSize(this, freeStorage),
                Formatter.formatFileSize(this, totalStorage),
                Formatter.formatFileSize(this, memoryInfo.availMem),
                Formatter.formatFileSize(this, memoryInfo.totalMem));
        new AlertDialog.Builder(this)
                .setTitle(R.string.device_information)
                .setMessage(info)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showHiddenApps() {
        Set<String> hiddenPackages = getPreferences().getStringSet(
                HIDDEN_APPS_KEY, Collections.emptySet());
        if (hiddenPackages.isEmpty()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.hidden_apps)
                    .setMessage(R.string.no_hidden_apps)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        List<ResolveInfo> hiddenApps = getAllLaunchableApps();
        List<ResolveInfo> uniqueHiddenApps = new ArrayList<>();
        Set<String> includedPackages = new HashSet<>();
        for (ResolveInfo app : hiddenApps) {
            String packageName = app.activityInfo.packageName;
            if (hiddenPackages.contains(packageName) && includedPackages.add(packageName)) {
                uniqueHiddenApps.add(app);
            }
        }
        if (uniqueHiddenApps.isEmpty()) {
            getPreferences().edit().remove(HIDDEN_APPS_KEY).apply();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.hidden_apps)
                    .setMessage(R.string.no_hidden_apps)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        sortApps(uniqueHiddenApps);
        CharSequence[] labels = new CharSequence[uniqueHiddenApps.size()];
        for (int i = 0; i < uniqueHiddenApps.size(); i++) {
            labels[i] = uniqueHiddenApps.get(i).loadLabel(getPackageManager());
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_hidden_app)
                .setItems(labels, (dialog, which) -> confirmRestoreApp(uniqueHiddenApps.get(which)))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void confirmRestoreApp(ResolveInfo app) {
        String label = app.loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore_hidden_app)
                .setMessage(getString(R.string.confirm_restore_app, label))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.restore, (dialog, which) -> {
                    Set<String> hidden = new HashSet<>(getPreferences().getStringSet(
                            HIDDEN_APPS_KEY, Collections.emptySet()));
                    hidden.remove(app.activityInfo.packageName);
                    getPreferences().edit().putStringSet(HIDDEN_APPS_KEY, hidden).apply();
                    showHome();
                })
                .show();
    }

    private void showAppOptions(ResolveInfo app) {
        String packageName = app.activityInfo.packageName;
        String label = app.loadLabel(getPackageManager()).toString();
        boolean isFavorite = getPreferences().getStringSet(
                FAVORITES_KEY, Collections.emptySet()).contains(packageName);
        String[] options = {
                getString(isFavorite ? R.string.remove_favorite : R.string.add_favorite),
                getString(R.string.hide_app),
                getString(R.string.uninstall_app)
        };
        new AlertDialog.Builder(this)
                .setTitle(label)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        toggleFavorite(app);
                    } else if (which == 1) {
                        confirmHideApp(app);
                    } else if (which == 2) {
                        confirmUninstallApp(app);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void toggleFavorite(ResolveInfo app) {
        Set<String> favorites = new HashSet<>(getPreferences().getStringSet(
                FAVORITES_KEY, Collections.emptySet()));
        String packageName = app.activityInfo.packageName;
        boolean added;
        if (favorites.remove(packageName)) {
            added = false;
        } else {
            favorites.add(packageName);
            added = true;
        }
        getPreferences().edit().putStringSet(FAVORITES_KEY, favorites).apply();
        showHome();
        Toast.makeText(this, added ? R.string.favorite_added : R.string.favorite_removed,
                Toast.LENGTH_SHORT).show();
    }

    private void confirmHideApp(ResolveInfo app) {
        String label = app.loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this)
                .setTitle(R.string.hide_app)
                .setMessage(getString(R.string.confirm_hide_app, label))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.hide_app, (dialog, which) -> {
                    Set<String> hidden = new HashSet<>(getPreferences().getStringSet(
                            HIDDEN_APPS_KEY, Collections.emptySet()));
                    hidden.add(app.activityInfo.packageName);
                    getPreferences().edit().putStringSet(HIDDEN_APPS_KEY, hidden).apply();
                    showHome();
                })
                .show();
    }

    private void confirmUninstallApp(ResolveInfo app) {
        String label = app.loadLabel(getPackageManager()).toString();
        new AlertDialog.Builder(this)
                .setTitle(R.string.uninstall_app)
                .setMessage(getString(R.string.confirm_uninstall_app, label))
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.uninstall_app, (dialog, which) -> {
                    Intent uninstallIntent = new Intent(Intent.ACTION_DELETE,
                            Uri.parse("package:" + app.activityInfo.packageName));
                    try {
                        startActivity(uninstallIntent);
                    } catch (ActivityNotFoundException exception) {
                        Toast.makeText(this, R.string.uninstall_unavailable, Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void updateClock() {
        if (clockText == null || dateText == null) {
            return;
        }
        Date now = new Date();
        clockText.setText(DateFormat.getTimeFormat(this).format(now));
        dateText.setText(new SimpleDateFormat("EEEE, d 'de' MMMM", new Locale("es", "ES"))
                .format(now));
    }

    private TextView createText(String text, int sizeSp, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextColor(color);
        view.setTextSize(sizeSp);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        return view;
    }

    private void addSpace(LinearLayout parent, int heightDp) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(heightDp)));
    }

    private GradientDrawable createBackdrop() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{currentTheme.backgroundTop, currentTheme.background, currentTheme.background});
        drawable.setGradientType(GradientDrawable.LINEAR_GRADIENT);
        return drawable;
    }

    private GradientDrawable createHeroBackground(boolean focused) {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{currentTheme.heroStart, currentTheme.heroMiddle, currentTheme.heroEnd});
        drawable.setCornerRadius(dp(24));
        drawable.setStroke(dp(focused ? 2 : 1),
                focused ? currentTheme.accent : currentTheme.border);
        return drawable;
    }

    private GradientDrawable createFeatureBadgeBackground() {
        GradientDrawable drawable = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{currentTheme.badgeStart, currentTheme.badgeEnd});
        drawable.setCornerRadius(dp(20));
        drawable.setStroke(dp(1), currentTheme.border);
        return drawable;
    }

    private GradientDrawable createActionPill() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(currentTheme.accent);
        drawable.setCornerRadius(dp(20));
        return drawable;
    }

    private GradientDrawable createSettingsBackground(boolean focused) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(focused ? currentTheme.cardFocused : currentTheme.card);
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(focused ? 2 : 1),
                focused ? currentTheme.accent : currentTheme.border);
        return drawable;
    }

    private GradientDrawable createAppCardBackground(boolean focused) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(focused ? currentTheme.cardFocused : currentTheme.card);
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(focused ? 2 : 1),
                focused ? currentTheme.accent : currentTheme.border);
        return drawable;
    }

    private GradientDrawable createIconBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(currentTheme.iconBackground);
        drawable.setCornerRadius(dp(20));
        return drawable;
    }

    private static final class ThemePalette {
        final String name;
        final int backgroundTop;
        final int background;
        final int heroStart;
        final int heroMiddle;
        final int heroEnd;
        final int card;
        final int cardFocused;
        final int accent;
        final int textOnAccent;
        final int border;
        final int iconBackground;
        final int badgeStart;
        final int badgeEnd;

        ThemePalette(String name, int backgroundTop, int background, int heroStart,
                int heroMiddle, int heroEnd, int card, int cardFocused, int accent,
                int textOnAccent, int border, int iconBackground, int badgeStart, int badgeEnd) {
            this.name = name;
            this.backgroundTop = backgroundTop;
            this.background = background;
            this.heroStart = heroStart;
            this.heroMiddle = heroMiddle;
            this.heroEnd = heroEnd;
            this.card = card;
            this.cardFocused = cardFocused;
            this.accent = accent;
            this.textOnAccent = textOnAccent;
            this.border = border;
            this.iconBackground = iconBackground;
            this.badgeStart = badgeStart;
            this.badgeEnd = badgeEnd;
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
