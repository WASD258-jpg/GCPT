package cn.gcpt.app;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.os.Message;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.PathInterpolator;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Local-only timetable shell. No account data or timetable content leaves the device. */
public class MainActivity extends Activity {
    private static final String HOME_URL = "https://home.gcp.edu.cn/";
    private static final String COURSE_URL =
            "https://jwcjw.gcp.edu.cn/jwglxt/kbcx/xskbcx_cxXskbcxIndex.html?gnmkdm=N253508&layout=default";
    private static final String JWC_HOME_URL = "https://jwcjw.gcp.edu.cn/";
    private static final String LMS_URL = "https://lms.gcp.edu.cn/";
    private static final String VERSION = "1.0.3";
    private static final String HOT_WATER_URL = "https://mh60.gcp.edu.cn/";
    private static final String WECHAT_UA =
            "Mozilla/5.0 (Linux; Android 15; PLC110) AppleWebKit/537.36 "
                    + "(KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36 "
                    + "MicroMessenger/8.0.50.2701(0x28003252) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN";
    private static final String CAS_MOBILE_URL =
            "http://xgxt.gcp.edu.cn/sms/api/cascsxy/app/noToken/casLogin/0001/m";
    private static final String HOURS_PAGE_URL =
            "http://xgxt.gcp.edu.cn/m/laborHistory?applicationCode";
    private static final String WEEK_QUERY_URL =
            "https://jwcjw.gcp.edu.cn/jwglxt/kbcx/xskbcxZccx_cxXskbcxIndex.html?gnmkdm=N2154&layout=default";
    private static final int TAB_HOME = 0, TAB_WEEK = 1, TAB_STUDY = 2,
            TAB_BROWSER = 3, TAB_STUDENT = 4, TAB_LIFE = 5, TAB_SETTINGS = 6;
    private static final int MAX_WEEK = 30;
    private static final int REQUEST_QR = 7001;

    private SharedPreferences prefs;
    private String oneCardBalance = "";
    private String oneCardUser = "";
    private boolean prefillOneCard;
    private boolean oneCardCapture;
    private boolean oneCardReturnToLife;
    private boolean balanceSyncing;
    private boolean balanceNeedsLogin;
    private int balancePollCount;
    private int balancePollMax = 7;
    private long balanceLastSync;
    private CourseData data = new CourseData();
    private int tab = TAB_HOME, returnTab = TAB_HOME;
    private int viewWeek = 1;
    private LocalDate termStart;
    private LocalDate lastToday;
    private boolean autoWeek = true, showTeacher = true;
    private boolean updatingSwitch;

    private FrameLayout content;
    private ScrollView homeScreen, studyScreen, settingsScreen, lifeScreen;
    private LinearLayout weekScreen, previewScreen, browserScreen;
    private final TextView[] tabViews = new TextView[7];
    private LinearLayout headerBar, bottomNav;
    private LineIconView headerMenu, headerBack, headerInfo;
    private TextView headerTitle;
    private final LinearLayout[] navItems = new LinearLayout[4];
    private final LineIconView[] navIcons = new LineIconView[4];
    private final TextView[] navLabels = new TextView[4];
    private boolean scheduleWeekMode;
    private int selectedDay;
    private View currentScreen;
    private int screenAnimGeneration;
    private boolean studentBrowserMode;
    private LinearLayout browserControls, browserSecondary, studentBar;

    private WebView web;
    private TextView browserState, browserAddress;
    private ProgressBar browserProgress;
    private int pageGeneration;
    private final Set<String> cleartextApproved = new HashSet<String>();
    private boolean cleartextPromptShowing;
    private boolean studentNoticeShown;
    private HoursData hoursData;
    private boolean hoursLoading;
    private String hoursError;
    private int hoursStage;
    private int hoursRequestId;
    private int syncStage;
    private int syncRequestId;

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        prefs = getPreferences(0);
        oneCardBalance = prefs.getString("oneCardBalance", "");
        oneCardUser = "";
        getWindow().setStatusBarColor(Ui.BG_TOP);
        getWindow().setNavigationBarColor(Ui.BG_TOP);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
        readSettings();
        loadData();
        if (autoWeek && termStart != null) {
            int current = currentWeek();
            if (current >= 1 && current <= MAX_WEEK) viewWeek = current;
        }
        lastToday = LocalDate.now();
        buildShell();
        buildBrowserScreen();
        if (saved != null) {
            viewWeek = Math.max(1, Math.min(MAX_WEEK, saved.getInt("viewWeek", viewWeek)));
            int restored = saved.getInt("tab", TAB_HOME);
            tab = (restored == TAB_HOME || restored == TAB_WEEK
                    || restored == TAB_LIFE || restored == TAB_SETTINGS)
                    ? restored : TAB_HOME;
        }
        showTab(tab);
        maybeAutoSync();
        showFirstRunNotice();
    }

    private void showFirstRunNotice() {
        if (prefs.getBoolean("noticeAccepted", false)) return;
        LinearLayout content = Ui.column(this);
        content.setPadding(dp(20), dp(16), dp(20), dp(4));

        content.addView(Ui.bold(this, "广城通 " + VERSION, 20, Ui.TEXT));
        content.addView(Ui.text(this,
                "非学校官方应用 · 开源项目（GPL-3.0）· 仅 GitHub 分发",
                10.5f, Ui.TEXT_SOFT), topMargin(4));
        TextView intro = Ui.text(this,
                "本项目开源，使用风险由你自行承担；开发者不提供担保。"
                        + "进入前请先阅读《隐私与数据须知》。",
                12, Ui.TEXT);
        intro.setLineSpacing(dp(2), 1f);
        content.addView(intro, topMargin(12));

        TextView link = Ui.action(this, "查看《隐私与数据须知》", false);
        content.addView(link, topMargin(12));
        content.addView(Ui.text(this,
                "打开须知后，下方「同意并进入」才会启用。",
                10, Ui.TEXT_FAINT), topMargin(8));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(content)
                .setCancelable(false)
                .setPositiveButton("同意并进入", null)
                .setNegativeButton("退出", (d, w) -> finish())
                .create();
        dialog.show();
        android.widget.Button positive = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positive.setEnabled(false);
        link.setOnClickListener(v -> showDataNotice(() -> positive.setEnabled(true)));
        positive.setOnClickListener(v -> {
            prefs.edit().putBoolean("noticeAccepted", true).apply();
            dialog.dismiss();
        });
    }

    private void showDataNotice(Runnable onRead) {
        new AlertDialog.Builder(this)
                .setTitle("隐私与数据须知")
                .setMessage("广城通是本地开源客户端。\n\n"
                        + "不收集、不上传、不运营服务器；没有账号、注册、统计、广告或崩溃上报。\n\n"
                        + "账号、密码、课表、余额、图书馆二维码只保存在本机。密码使用 Android Keystore 加密；"
                        + "账号、课表、余额为应用私有明文；清除数据或卸载即删除。\n\n"
                        + "学校、微信、图书馆等第三方会按各自政策处理你的请求；请使用本人账号并自行保护设备。\n\n"
                        + "本项目以 GPL-3.0 开源，使用风险由你自行承担；开发者不提供担保。")
                .setCancelable(false)
                .setPositiveButton("已阅读", (d, w) -> {
                    if (onRead != null) onRead.run();
                })
                .show();
    }

    // ---------------------------------------------------------------- settings & data

    private void readSettings() {
        termStart = TermCalendar.parse(prefs.getString("termStart", ""));
        autoWeek = prefs.getBoolean("autoWeek", true);
        showTeacher = prefs.getBoolean("showTeacher", true);
        viewWeek = Math.max(1, Math.min(MAX_WEEK, prefs.getInt("selectedWeek", 1)));
    }

    private void loadData() {
        data = new CourseData();
        String json = prefs.getString("courseJson", "");
        if (!json.isEmpty()) {
            try {
                data.load(new JSONObject(json));
            } catch (Exception ignored) {
                data.clear();
            }
        }
        data.rawText = prefs.getString("courseText", "");
        data.lastSync = prefs.getString("lastSync", "");
    }

    private int currentWeek() {
        if (termStart == null) return 0;
        return TermCalendar.weekOf(termStart, LocalDate.now());
    }

    private int homeWeek() {
        int current = currentWeek();
        if (autoWeek && current >= 1 && current <= MAX_WEEK) return current;
        return viewWeek;
    }

    private String termLabel() {
        String term = data.term == null ? "" : data.term.trim();
        if (term.isEmpty()) return "学期待读取";
        int slash = term.indexOf('/');
        if (slash > 0) {
            String year = term.substring(0, slash).trim();
            String number = term.substring(slash + 1).trim();
            return year + " 学年 · 第 " + number + " 学期";
        }
        return term;
    }

    // ---------------------------------------------------------------- shell

    private void buildShell() {
        FrameLayout root = new FrameLayout(this);
        root.setBackground(Ui.background(this));
        setContentView(root);

        LinearLayout shell = Ui.column(this);
        root.addView(shell, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        headerBar = buildHeader();
        shell.addView(headerBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(58)));

        content = new FrameLayout(this);
        shell.addView(content, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        homeScreen = new ScrollView(this);
        homeScreen.setBackgroundColor(Color.TRANSPARENT);
        homeScreen.setFillViewport(true);
        homeScreen.setVerticalScrollBarEnabled(false);
        homeScreen.setClipToPadding(false);
        content.addView(homeScreen, full());

        weekScreen = Ui.column(this);
        weekScreen.setBackgroundColor(Color.TRANSPARENT);
        content.addView(weekScreen, full());

        studyScreen = new ScrollView(this);
        studyScreen.setBackgroundColor(Color.TRANSPARENT);
        studyScreen.setFillViewport(true);
        studyScreen.setVerticalScrollBarEnabled(false);
        studyScreen.setClipToPadding(false);
        content.addView(studyScreen, full());

        settingsScreen = new ScrollView(this);
        settingsScreen.setBackgroundColor(Color.TRANSPARENT);
        settingsScreen.setFillViewport(true);
        settingsScreen.setVerticalScrollBarEnabled(false);
        settingsScreen.setClipToPadding(false);
        content.addView(settingsScreen, full());

        lifeScreen = new ScrollView(this);
        lifeScreen.setBackgroundColor(Color.TRANSPARENT);
        lifeScreen.setFillViewport(true);
        lifeScreen.setVerticalScrollBarEnabled(false);
        lifeScreen.setClipToPadding(false);
        content.addView(lifeScreen, full());

        previewScreen = Ui.column(this);
        previewScreen.setBackgroundColor(Color.TRANSPARENT);
        content.addView(previewScreen, full());

        bottomNav = buildBottomNav();
        shell.addView(bottomNav, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(70)));

        homeScreen.setVisibility(View.GONE);
        weekScreen.setVisibility(View.GONE);
        studyScreen.setVisibility(View.GONE);
        settingsScreen.setVisibility(View.GONE);
        lifeScreen.setVisibility(View.GONE);
        previewScreen.setVisibility(View.GONE);
    }

    private LinearLayout buildHeader() {
        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Color.TRANSPARENT);
        bar.setPadding(dp(18), dp(6), dp(16), dp(2));

        headerMenu = new LineIconView(this, LineIconView.MENU, Ui.TEXT);
        headerMenu.setOnClickListener(v -> showSideMenu());
        LinearLayout.LayoutParams menuLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        bar.addView(headerMenu, menuLp);

        headerBack = new LineIconView(this, LineIconView.CHEVRON, Ui.TEXT);
        headerBack.setVisibility(View.GONE);
        headerBack.setOnClickListener(v -> showTab(TAB_HOME));
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(38), dp(38));
        bar.addView(headerBack, backLp);

        headerTitle = Ui.heading(this, "广城通", 18, Ui.TEXT);
        LinearLayout.LayoutParams titleLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleLp.leftMargin = dp(8);
        bar.addView(headerTitle, titleLp);

        bar.addView(Ui.flexibleGap(this));

        headerInfo = new LineIconView(this, LineIconView.INFO, Ui.TEXT_SOFT);
        headerInfo.setBackground(Ui.round(0x40FFFFFF, 18, this));
        headerInfo.setOnClickListener(v -> showDataNotice(null));
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        bar.addView(headerInfo, infoLp);
        return bar;
    }

    private LinearLayout buildBottomNav() {
        LinearLayout nav = Ui.row(this);
        nav.setPadding(dp(12), dp(4), dp(12), dp(4));
        nav.setBackground(Ui.roundStroke(0xCCF8F5ED, 0, 0x33FFFFFF, 1, this));
        String[] labels = {"首页", "课表", "生活", "设置"};
        int[] icons = {LineIconView.HOME, LineIconView.CALENDAR, LineIconView.CUP,
                LineIconView.GEAR};
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            LinearLayout item = Ui.column(this);
            item.setGravity(Gravity.CENTER);
            item.setPadding(dp(4), dp(6), dp(4), dp(4));
            item.setClickable(true);
            item.setFocusable(true);
            item.setBackground(Ui.ripple(0x00000000, 18, 0x14276757, this));
            item.setOnClickListener(v -> showTab(index == 0 ? TAB_HOME
                    : index == 1 ? TAB_WEEK : index == 2 ? TAB_LIFE : TAB_SETTINGS));

            LineIconView icon = new LineIconView(this, icons[i], Ui.TEXT_FAINT);
            item.addView(icon, new LinearLayout.LayoutParams(dp(23), dp(23)));
            TextView label = Ui.medium(this, labels[i], 10.5f, Ui.TEXT_FAINT);
            LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            labelLp.topMargin = dp(2);
            item.addView(label, labelLp);

            nav.addView(item, new LinearLayout.LayoutParams(0, dp(58), 1));
            navItems[i] = item;
            navIcons[i] = icon;
            navLabels[i] = label;
        }
        return nav;
    }

    private void updateHeader(int which) {
        boolean sub = which == TAB_STUDY || which == TAB_BROWSER || which == TAB_STUDENT;
        headerMenu.setVisibility(sub ? View.GONE : View.VISIBLE);
        headerBack.setVisibility(sub ? View.VISIBLE : View.GONE);
        headerTitle.setText(sub
                ? (which == TAB_STUDY ? "学业与学时"
                : which == TAB_STUDENT ? "学工系统" : "学校门户")
                : "广城通");
        bottomNav.setVisibility(sub ? View.GONE : View.VISIBLE);
    }

    private void showSideMenu() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFF8F5EC);
        scroll.setVerticalScrollBarEnabled(false);
        LinearLayout panel = Ui.column(this);
        panel.setPadding(dp(22), dp(32), dp(22), dp(24));
        scroll.addView(panel, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout head = Ui.row(this);
        TextView monogram = Ui.heading(this, "广", 18, Ui.ACCENT);
        monogram.setGravity(Gravity.CENTER);
        monogram.setBackground(Ui.round(0x14276757, 22, this));
        head.addView(monogram, new LinearLayout.LayoutParams(dp(44), dp(44)));
        LinearLayout brand = Ui.column(this);
        brand.setPadding(dp(10), 0, 0, 0);
        brand.addView(Ui.heading(this, "广城通", 22, Ui.TEXT));
        brand.addView(Ui.text(this, VERSION + " · 本地开源客户端", 9.5f, Ui.TEXT_FAINT),
                topMargin(2));
        head.addView(brand, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView close = Ui.medium(this, "×", 26, Ui.TEXT_SOFT);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.ripple(0x00000000, 18, 0x14276757, this));
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close, new LinearLayout.LayoutParams(dp(40), dp(40)));
        panel.addView(head);

        panel.addView(menuSection("导航"), topMargin(24));
        panel.addView(menuRow(LineIconView.HOME, "首页", "今天安排",
                tab == TAB_HOME, () -> {
                    dialog.dismiss();
                    showTab(TAB_HOME);
                }), topMargin(2));
        panel.addView(menuRow(LineIconView.CALENDAR, "课表", "这一周的节奏",
                tab == TAB_WEEK, () -> {
                    dialog.dismiss();
                    showTab(TAB_WEEK);
                }), topMargin(2));
        panel.addView(menuRow(LineIconView.CUP, "生活", "校园卡与图书馆",
                tab == TAB_LIFE, () -> {
                    dialog.dismiss();
                    showTab(TAB_LIFE);
                }), topMargin(2));
        panel.addView(menuRow(LineIconView.GEAR, "设置", "偏好与凭证",
                tab == TAB_SETTINGS, () -> {
                    dialog.dismiss();
                    showTab(TAB_SETTINGS);
                }), topMargin(2));

        panel.addView(menuSection("校园入口"), topMargin(20));
        panel.addView(menuRow(LineIconView.HOME, "学校门户", "综合服务中心与统一认证", false, () -> {
            dialog.dismiss();
            openServiceWeb(HOME_URL);
        }), topMargin(2));
        panel.addView(menuRow(LineIconView.DOC, "教务系统", "课表与考试", false, () -> {
            dialog.dismiss();
            openServiceWeb(JWC_HOME_URL);
        }), topMargin(2));
        panel.addView(menuRow(LineIconView.PERSON, "学工系统", "学时与事务", false, () -> {
            dialog.dismiss();
            openStudentAffairs();
        }), topMargin(2));
        panel.addView(menuRow(LineIconView.BUILDING, "网上办事大厅", "流程与证明", false, () -> {
            dialog.dismiss();
            openServiceWeb("https://ehall2.gcp.edu.cn/taskcenter/workflow/index");
        }), topMargin(2));
        panel.addView(menuRow(LineIconView.CROSS, "网上医务室", "预约与问诊", false, () -> {
            dialog.dismiss();
            openServiceWeb("https://yws.gcp.edu.cn/m/");
        }), topMargin(2));
        panel.addView(menuRow(LineIconView.BUILDING, "宿舍报修", "后勤报修 · 水电门窗", false, () -> {
            dialog.dismiss();
            openServiceWeb(LMS_URL);
        }), topMargin(2));

        panel.addView(menuSection("说明"), topMargin(20));
        panel.addView(menuRow(LineIconView.INFO, "数据与隐私须知", "本地数据边界", false, () -> {
            dialog.dismiss();
            showDataNotice(null);
        }), topMargin(2));
        panel.addView(menuRow(LineIconView.BOOK, "关于广城通", "版本与开源许可", false, () -> {
            dialog.dismiss();
            showAbout();
        }), topMargin(2));

        panel.addView(Ui.text(this, "清除数据请到 设置 → 隐私与数据", 9.5f, Ui.TEXT_FAINT),
                topMargin(22));
        dialog.setContentView(scroll);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(0x55000000));
            window.setGravity(Gravity.LEFT);
            window.setLayout(dp(312), ViewGroup.LayoutParams.MATCH_PARENT);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setDimAmount(0.35f);
        }
        dialog.show();
        scroll.setAlpha(0f);
        scroll.setTranslationX(-dp(28));
        scroll.animate().alpha(1f).translationX(0f).setDuration(300)
                .setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f)).start();
    }

    private TextView menuSection(String label) {
        TextView section = Ui.medium(this, label, 9.5f, Ui.TEXT_FAINT);
        section.setLetterSpacing(0.14f);
        return section;
    }

    private LinearLayout menuRow(int icon, String title, String subtitle,
                                 boolean selected, Runnable action) {
        LinearLayout row = Ui.row(this);
        row.setPadding(dp(10), dp(9), dp(10), dp(9));
        row.setClickable(true);
        row.setFocusable(true);
        row.setBackground(selected
                ? Ui.ripple(0x14276757, 14, 0x14276757, this)
                : Ui.ripple(0x00000000, 14, 0x14276757, this));
        row.addView(new LineIconView(this, icon, selected ? Ui.ACCENT : Ui.TEXT_SOFT),
                new LinearLayout.LayoutParams(dp(19), dp(19)));
        LinearLayout block = Ui.column(this);
        block.setPadding(dp(11), 0, 0, 0);
        TextView titleView = Ui.medium(this, title, 13, selected ? Ui.ACCENT : Ui.TEXT);
        block.addView(titleView);
        TextView subtitleView = Ui.text(this, subtitle, 9.5f, Ui.TEXT_FAINT);
        block.addView(subtitleView, topMargin(2));
        row.addView(block, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(new LineIconView(this, LineIconView.CHEVRON_RIGHT, Ui.TEXT_FAINT),
                new LinearLayout.LayoutParams(dp(14), dp(14)));
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("关于广城通")
                .setMessage("广城通 " + VERSION + "\n\n"
                        + "非学校官方应用，开源项目（GPL-3.0），仅 GitHub 分发。\n\n"
                        + "数据只保存在本机；不收集、不上传、没有账号或统计。\n\n"
                        + "使用风险由你自行承担；开发者不提供担保。")
                .setPositiveButton("知道了", null)
                .show();
    }

    private LinearLayout buildTopBar() {
        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Ui.BG);
        bar.setPadding(dp(20), 0, dp(8), 0);

        LinearLayout brand = Ui.column(this);
        brand.setPadding(0, dp(8), dp(10), dp(6));
        brand.addView(Ui.bold(this, "广城通", 17, Ui.TEXT));
        brand.addView(Ui.text(this, VERSION, 9, Ui.TEXT_FAINT));
        bar.addView(brand);

        bar.addView(Ui.flexibleGap(this));

        String[] labels = {"首页", "课表", "学业", "教务", "学工", "生活", "设置"};
        for (int i = 0; i < labels.length; i++) {
            final int target = i;
            TextView t = Ui.medium(this, labels[i], 10.5f, Ui.TEXT_FAINT);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(5), 0, dp(5), 0);
            t.setClickable(true);
            t.setFocusable(true);
            t.setOnClickListener(v -> showTab(target));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
            lp.leftMargin = dp(3);
            bar.addView(t, lp);
            tabViews[i] = t;
        }
        return bar;
    }

    private FrameLayout.LayoutParams full() {
        return new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private void showTab(int which) {
        setStudentMode(false);
        if (which != TAB_BROWSER) oneCardReturnToLife = false;
        tab = which;
        applyOrientationForTab(which);
        if (which == TAB_HOME) renderHome();
        else if (which == TAB_WEEK) renderWeek();
        else if (which == TAB_STUDY) {
            renderStudy();
            if (hoursData == null && !hoursLoading) fetchHours();
        }
        else if (which == TAB_STUDENT) openStudentAffairs();
        else if (which == TAB_LIFE) {
            if (!balanceSyncing) startBalanceSync();
            renderLife();
        }
        else if (which == TAB_SETTINGS) renderSettings();
        if (which == TAB_BROWSER && web != null && web.getUrl() != null
                && (web.getUrl().contains("xgxt.gcp.edu.cn")
                || web.getUrl().contains("zffw.gcp.edu.cn"))) {
            web.loadUrl(HOME_URL);
        }
        if (which == TAB_BROWSER && web != null && web.getUrl() != null) {
            applyOrientationForPage(web.getUrl());
        }
        boolean browserVisible = which == TAB_BROWSER || studentBrowserMode;
        View target;
        if (which == TAB_HOME) target = homeScreen;
        else if (which == TAB_WEEK) target = weekScreen;
        else if (which == TAB_STUDY && !studentBrowserMode) target = studyScreen;
        else if (which == TAB_LIFE) target = lifeScreen;
        else if (which == TAB_SETTINGS) target = settingsScreen;
        else if (browserVisible) target = browserScreen;
        else target = homeScreen;
        View[] screens = {homeScreen, weekScreen, studyScreen, lifeScreen,
                settingsScreen, browserScreen};
        for (View screen : screens) {
            if (screen != target && screen != currentScreen) {
                screen.setVisibility(View.GONE);
                screen.setAlpha(1f);
                screen.setTranslationY(0f);
            }
        }
        previewScreen.setVisibility(View.GONE);
        if (web != null) web.setVisibility(browserVisible ? View.VISIBLE : View.GONE);
        swapScreen(target);
        updateHeader(which);
        updateTabs();
    }

    /** 280ms crossfade + slight rise, matching demo C's page transition. */
    private void swapScreen(final View target) {
        if (target == null) return;
        final View old = currentScreen;
        currentScreen = target;
        if (old == target) {
            target.setVisibility(View.VISIBLE);
            target.setAlpha(1f);
            target.setTranslationY(0f);
            return;
        }
        final int generation = ++screenAnimGeneration;
        PathInterpolator curve = new PathInterpolator(0.22f, 1f, 0.36f, 1f);
        if (old != null) {
            old.animate().alpha(0f).translationY(-dp(5))
                    .setDuration(170).setInterpolator(curve)
                    .withEndAction(() -> {
                        if (generation != screenAnimGeneration) return;
                        old.setVisibility(View.GONE);
                        old.setAlpha(1f);
                        old.setTranslationY(0f);
                    }).start();
        }
        target.setAlpha(0f);
        target.setTranslationY(dp(8));
        target.setVisibility(View.VISIBLE);
        target.animate().alpha(1f).translationY(0f)
                .setDuration(280).setInterpolator(curve).start();
    }

    private void applyOrientationForTab(int which) {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
    }

    private void applyOrientationForPage(String url) {
        if (tab != TAB_BROWSER || studentBrowserMode || syncStage > 0) return;
        Uri u = Uri.parse(url);
        String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.US);
        String path = u.getPath() == null ? "" : u.getPath();
        if ("auth.gcp.edu.cn".equals(host) || path.contains("sso/login")
                || "home.gcp.edu.cn".equals(host) || "jwcjw.gcp.edu.cn".equals(host)
                || "lms.gcp.edu.cn".equals(host)) {
            // School portal, SSO and 正方 pages all use the phone/portrait layout.
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private void showPreviewScreen() {
        returnTab = tab;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        renderPreview();
        homeScreen.setVisibility(View.GONE);
        weekScreen.setVisibility(View.GONE);
        studyScreen.setVisibility(View.GONE);
        settingsScreen.setVisibility(View.GONE);
        browserScreen.setVisibility(View.GONE);
        previewScreen.setVisibility(View.VISIBLE);
        currentScreen = previewScreen;
    }

    private void updateTabs() {
        int active = tab == TAB_HOME ? 0 : tab == TAB_WEEK ? 1
                : tab == TAB_LIFE ? 2 : tab == TAB_SETTINGS ? 3 : -1;
        for (int i = 0; i < navItems.length; i++) {
            final LineIconView icon = navIcons[i];
            final TextView label = navLabels[i];
            boolean selected = i == active;
            int from = label.getCurrentTextColor();
            int to = selected ? Ui.ACCENT : Ui.TEXT_FAINT;
            if (from != to) {
                ValueAnimator anim = ValueAnimator.ofObject(new ArgbEvaluator(), from, to);
                anim.setDuration(180);
                anim.addUpdateListener(a -> {
                    int color = (int) a.getAnimatedValue();
                    label.setTextColor(color);
                    icon.setColor(color);
                });
                anim.start();
            }
            navItems[i].setBackground(selected
                    ? Ui.ripple(0x18276757, 18, 0x14276757, this)
                    : Ui.ripple(0x00000000, 18, 0x14276757, this));
            navItems[i].animate().scaleX(selected ? 1.02f : 1f)
                    .scaleY(selected ? 1.02f : 1f).setDuration(180).start();
        }
    }

    private int dp(float value) {
        return Ui.dp(this, value);
    }

    private int sidePadding(int maxContentDp) {
        int width = getResources().getConfiguration().screenWidthDp;
        return Math.max(16, (width - maxContentDp) / 2);
    }

    // ---------------------------------------------------------------- home

    private void renderHome() {
        homeScreen.removeAllViews();
        LinearLayout page = Ui.column(this);
        int side = sidePadding(760);
        page.setPadding(dp(side), dp(2), dp(side), dp(30));
        homeScreen.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LocalDate today = LocalDate.now();
        int week = homeWeek();
        int todayDay = today.getDayOfWeek().getValue();
        page.addView(Ui.text(this,
                TermCalendar.weekTitle(week) + " · " + TermCalendar.shortMonthDay(today)
                        + " " + TermCalendar.weekday(today),
                10.5f, Ui.TEXT_SOFT));

        TextView hero = Ui.heading(this, "把今天，安排妥当。", 30, Ui.TEXT);
        page.addView(hero, topMargin(6));

        page.addView(sectionHeader("今天的课程", "查看课表 →", () -> showTab(TAB_WEEK)),
                topMargin(18));
        page.addView(todayCard(todayDay, week), topMargin(10));

        LinearLayout overviewHead = Ui.row(this);
        overviewHead.addView(Ui.bold(this, "本周概览", 15, Ui.TEXT));
        overviewHead.addView(Ui.flexibleGap(this));
        String overview = data.countInWeek(week) + " 条排课"
                + (data.hasPending() ? " · " + data.pending.size() + " 项待确认" : "");
        overviewHead.addView(Ui.text(this, overview, 10.5f, Ui.TEXT_FAINT));
        page.addView(overviewHead, topMargin(20));

        page.addView(weekDots(week), topMargin(10));

        LinearLayout stats = Ui.row(this);
        LinearLayout.LayoutParams statLp = new LinearLayout.LayoutParams(0, dp(92), 1);
        stats.addView(statCard("学时",
                hoursData == null ? "—" : formatHoursShort(hoursData.total()), "学时明细",
                LineIconView.CLOCK_HOURS, () -> showTab(TAB_STUDY)), statLp);
        LinearLayout.LayoutParams stat2Lp = new LinearLayout.LayoutParams(0, dp(92), 1);
        stat2Lp.leftMargin = dp(10);
        stats.addView(statCard("校园卡余额",
                oneCardBalance.isEmpty() ? "—" : "¥" + oneCardBalance, "一卡通凭证",
                LineIconView.WALLET, () -> showTab(TAB_LIFE)), stat2Lp);
        page.addView(stats, topMargin(10));

        int pending = data.pending.size() + (balanceNeedsLogin ? 1 : 0) + (hoursData == null ? 1 : 0);
        if (pending > 0) {
            page.addView(pendingCard(pending), topMargin(10));
        }

        page.addView(Ui.bold(this, "校园入口", 13.5f, Ui.TEXT), topMargin(20));
        LinearLayout quick1 = Ui.row(this);
        quick1.addView(quickCard("教务系统", "课表与考试", LineIconView.DOC,
                () -> openServiceWeb(JWC_HOME_URL)), weightLp(1, 0));
        quick1.addView(quickCard("学工系统", "学时与事务", LineIconView.PERSON,
                this::openStudentAffairs), weightLp(1, 10));
        page.addView(quick1, topMargin(8));
        LinearLayout quick2 = Ui.row(this);
        quick2.addView(quickCard("网上办事大厅", "流程与证明", LineIconView.BUILDING,
                () -> openServiceWeb("https://ehall2.gcp.edu.cn/taskcenter/workflow/index")),
                weightLp(1, 0));
        quick2.addView(quickCard("网上医务室", "预约与问诊", LineIconView.CROSS,
                () -> openServiceWeb("https://yws.gcp.edu.cn/m/")), weightLp(1, 10));
        page.addView(quick2, topMargin(8));
        page.addView(quickCard("学校门户", "综合服务中心与统一认证", LineIconView.HOME,
                () -> openServiceWeb(HOME_URL)), topMargin(8));
        page.addView(quickCard("宿舍报修", "后勤报修 · 水电门窗", LineIconView.BUILDING,
                () -> openServiceWeb(LMS_URL)), topMargin(8));

        if (termStart == null) {
            page.addView(buildWeekNudge(), topMargin(14));
        }

        TextView footer = Ui.text(this, "非学校官方应用 · 数据仅保存在本机 · v" + VERSION,
                9.5f, Ui.TEXT_FAINT);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, topMargin(22));
    }

    private View sectionHeader(String title, String action, Runnable onClick) {
        LinearLayout row = Ui.row(this);
        row.addView(Ui.bold(this, title, 15, Ui.TEXT));
        row.addView(Ui.flexibleGap(this));
        TextView link = Ui.ghost(this, action, 11);
        link.setOnClickListener(v -> onClick.run());
        row.addView(link);
        return row;
    }

    private View todayCard(int day, int week) {
        List<CourseParser.Course> courses = data.coursesOn(day, week);
        LinearLayout card = Ui.card(this);
        card.setClickable(true);
        card.setFocusable(true);
        if (courses.isEmpty()) {
            if (data.isEmpty()) {
                card.addView(Ui.medium(this, "还没有课表", 16, Ui.TEXT));
                card.addView(Ui.text(this, "连接学校门户同步课表后，这里会显示今天的课。",
                        10.5f, Ui.TEXT_SOFT), topMargin(4));
                TextView connect = Ui.action(this, "连接门户", true);
                connect.setOnClickListener(v -> connectPortal());
                card.addView(connect, topMargin(10));
            } else {
                card.addView(Ui.medium(this, "今天没有课", 18, Ui.TEXT));
                card.addView(Ui.text(this, "本周还有 " + data.countInWeek(week)
                        + " 条排课，去课表看看。", 10.5f, Ui.TEXT_SOFT), topMargin(4));
            }
            return card;
        }
        CourseParser.Course course = courses.get(0);
        LinearLayout top = Ui.row(this);
        top.addView(Ui.text(this, TermCalendar.monthDay(LocalDate.now()) + " · "
                + TermCalendar.weekday(LocalDate.now()), 10.5f, Ui.TEXT_SOFT));
        top.addView(Ui.flexibleGap(this));
        TextView timeTag = Ui.medium(this, "● " + dayPart(course.start), 10,
                WeekGrid.accentColor(course.name));
        top.addView(timeTag);
        card.addView(top);

        String name = course.name.replace("【调】", "").replace("[调]", "").trim();
        card.addView(Ui.bold(this, name.isEmpty() ? "未命名课程" : name, 20, Ui.TEXT),
                topMargin(8));
        String range = PeriodTimes.range(course.start, course.end);
        card.addView(Ui.medium(this, range + " · 第 " + course.start + "–" + course.end + " 节",
                11, Ui.TEXT_SOFT), topMargin(6));
        String place = (course.room == null || course.room.isEmpty() ? "地点待确认" : course.room)
                + (showTeacher && course.teacher != null && !course.teacher.isEmpty()
                ? " · " + course.teacher : "");
        card.addView(Ui.text(this, place, 10.5f, Ui.TEXT_FAINT), topMargin(4));
        card.setOnClickListener(v -> CourseDetailDialog.show(this, course,
                data.overlaps(course, week)));
        if (courses.size() > 1) {
            card.addView(Ui.text(this, "今天还有 " + (courses.size() - 1) + " 门课",
                    10, Ui.ACCENT), topMargin(8));
        }
        return card;
    }

    private View weekDots(int week) {
        LinearLayout row = Ui.row(this);
        int today = LocalDate.now().getDayOfWeek().getValue();
        boolean current = currentWeek() == week;
        for (int i = 1; i <= 7; i++) {
            final int day = i;
            LinearLayout col = Ui.column(this);
            col.setGravity(Gravity.CENTER);
            col.setPadding(0, dp(4), 0, dp(4));
            boolean active = current && day == today;
            TextView label = Ui.text(this, TermCalendar.DAYS[i - 1], 9.5f,
                    active ? Ui.ACCENT : Ui.TEXT_FAINT);
            label.setGravity(Gravity.CENTER);
            col.addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            View dot = new View(this);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(dp(6), dp(6));
            dotLp.topMargin = dp(5);
            dot.setBackground(Ui.round(active ? Ui.ACCENT : 0x33233B38, 3, this));
            col.addView(dot, dotLp);
            String date = "";
            if (termStart != null) {
                LocalDate d = TermCalendar.weekStart(termStart, week).plusDays(i - 1L);
                date = String.valueOf(d.getDayOfMonth());
            }
            TextView dateView = Ui.text(this, date, 9.5f,
                    active ? Ui.ACCENT : Ui.TEXT_FAINT);
            dateView.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dateLp.topMargin = dp(3);
            col.addView(dateView, dateLp);
            row.addView(col, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        return row;
    }

    private View statCard(String label, String value, String sub, int icon, Runnable action) {
        LinearLayout card = Ui.glassCard(this, 20, 14, 12);
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout top = Ui.row(this);
        top.addView(Ui.text(this, label, 10.5f, Ui.TEXT_SOFT));
        top.addView(Ui.flexibleGap(this));
        LineIconView ic = new LineIconView(this, icon, Ui.TEXT_FAINT);
        top.addView(ic, new LinearLayout.LayoutParams(dp(16), dp(16)));
        card.addView(top);
        TextView valueView = Ui.heading(this, value, 22, Ui.TEXT);
        card.addView(valueView, topMargin(5));
        card.addView(Ui.text(this, sub, 9.5f, Ui.TEXT_FAINT), topMargin(2));
        card.setOnClickListener(v -> action.run());
        return card;
    }

    private View pendingCard(int count) {
        LinearLayout row = Ui.row(this);
        row.setBackground(Ui.roundStroke(0x66FFFFFF, 18, Ui.CARD_BORDER, 1, this));
        row.setPadding(dp(14), dp(10), dp(14), dp(10));
        row.setClickable(true);
        row.setFocusable(true);
        TextView badge = Ui.medium(this, String.valueOf(count), 11, Ui.ACCENT);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(Ui.round(Ui.ACCENT_SOFT, 14, this));
        row.addView(badge, new LinearLayout.LayoutParams(dp(28), dp(28)));
        LinearLayout block = Ui.column(this);
        block.setPadding(dp(10), 0, 0, 0);
        block.addView(Ui.medium(this, "还有 " + count + " 件小事，待确认", 12.5f, Ui.TEXT));
        block.addView(Ui.text(this, "学时明细 · 一卡通凭证 · 课表待确认", 9.5f, Ui.TEXT_FAINT),
                topMargin(2));
        row.addView(block, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(new LineIconView(this, LineIconView.CHEVRON_RIGHT, Ui.TEXT_FAINT),
                new LinearLayout.LayoutParams(dp(18), dp(18)));
        row.setOnClickListener(v -> {
            if (hoursData == null) showTab(TAB_STUDY);
            else if (balanceNeedsLogin) showTab(TAB_LIFE);
            else if (data.hasPending()) showPendingDialog(data.pending);
        });
        return row;
    }

    private View quickCard(String title, String subtitle, int icon, Runnable action) {
        LinearLayout card = Ui.glassCard(this, 18, 13, 11);
        card.setClickable(true);
        card.setFocusable(true);
        card.setOnClickListener(v -> action.run());
        LinearLayout row = Ui.row(this);
        LinearLayout badge = Ui.column(this);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(Ui.round(0x66FFFFFF, 12, this));
        badge.addView(new LineIconView(this, icon, Ui.ACCENT),
                new LinearLayout.LayoutParams(dp(17), dp(17)));
        row.addView(badge, new LinearLayout.LayoutParams(dp(34), dp(34)));
        LinearLayout text = Ui.column(this);
        text.setPadding(dp(9), 0, 0, 0);
        text.addView(Ui.medium(this, title, 12, Ui.TEXT));
        text.addView(Ui.text(this, subtitle, 9.5f, Ui.TEXT_FAINT), topMargin(2));
        row.addView(text, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        row.addView(new LineIconView(this, LineIconView.ARROW, Ui.TEXT_FAINT),
                new LinearLayout.LayoutParams(dp(15), dp(15)));
        card.addView(row);
        return card;
    }

    private LinearLayout.LayoutParams weightLp(float weight, int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, weight);
        lp.leftMargin = dp(leftMargin);
        return lp;
    }

    private static String dayPart(int start) {
        if (start <= 4) return "上午课程";
        if (start <= 8) return "下午课程";
        return "晚间课程";
    }

    private static String formatHoursShort(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) {
            return String.format(Locale.US, "%.0f", value);
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private void startBalanceSync() {
        if (web == null) return;
        balanceSyncing = true;
        balanceNeedsLogin = false;
        balancePollCount = 0;
        balancePollMax = 7;
        oneCardCapture = true;
        web.getSettings().setUserAgentString(
                "Mozilla/5.0 (Linux; Android 15; PLC110) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36");
        web.loadUrl("https://zffw.gcp.edu.cn/");
        for (int i = 1; i <= 7; i++) {
            web.postDelayed(this::captureOneCardBalance, 1500L * i);
        }
    }

    private void renderLife() {
        lifeScreen.removeAllViews();
        LinearLayout page = Ui.column(this);
        int side = sidePadding(760);
        page.setPadding(dp(side), dp(2), dp(side), dp(30));
        lifeScreen.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        page.addView(Ui.heading(this, "校园里的小日常", 26, Ui.TEXT));
        page.addView(Ui.text(this, "常用服务，就在手边。", 11, Ui.TEXT_FAINT), topMargin(4));

        LinearLayout balanceCard = Ui.card(this);
        LinearLayout balanceTop = Ui.row(this);
        balanceTop.addView(Ui.medium(this, "校园一卡通", 12, Ui.TEXT));
        balanceTop.addView(Ui.flexibleGap(this));
        balanceTop.addView(Ui.text(this, "余额", 10.5f, Ui.TEXT_FAINT));
        balanceCard.addView(balanceTop);
        LinearLayout balanceValue = Ui.row(this);
        balanceValue.addView(Ui.heading(this,
                oneCardBalance.isEmpty() ? "—" : "¥" + oneCardBalance, 34, Ui.TEXT));
        String status;
        if (balanceSyncing) status = "正在同步…";
        else if (balanceNeedsLogin) status = "登录已过期";
        else if (oneCardBalance.isEmpty()) status = "进入页面自动同步";
        else status = "上次同步 " + lastSyncText();
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        statusLp.leftMargin = dp(8);
        balanceValue.addView(Ui.text(this, status, 9.5f, Ui.TEXT_FAINT), statusLp);
        balanceCard.addView(balanceValue, topMargin(2));
        balanceCard.addView(Ui.divider(this), topMargin(10));
        LinearLayout manage = Ui.row(this);
        manage.setPadding(0, dp(8), 0, 0);
        manage.addView(Ui.text(this, "当前数据 · 人民币", 9.5f, Ui.TEXT_FAINT));
        manage.addView(Ui.flexibleGap(this));
        TextView manageLink = Ui.medium(this,
                balanceNeedsLogin ? "登录一卡通 →" : "管理一卡通凭证 →", 10.5f, Ui.ACCENT);
        manageLink.setOnClickListener(v -> openOneCardZffw());
        manage.addView(manageLink);
        balanceCard.addView(manage);
        page.addView(balanceCard, topMargin(16));

        page.addView(Ui.bold(this, "宿舍生活", 13, Ui.TEXT), topMargin(20));
        LinearLayout hot = Ui.glassCard(this, 20, 14, 12);
        hot.setClickable(true);
        hot.setFocusable(true);
        LinearLayout hotRow = Ui.row(this);
        LinearLayout waterBadge = Ui.column(this);
        waterBadge.setGravity(Gravity.CENTER);
        waterBadge.setBackground(Ui.round(0x66FFFFFF, 14, this));
        waterBadge.addView(new LineIconView(this, LineIconView.DROP, Ui.ACCENT),
                new LinearLayout.LayoutParams(dp(20), dp(20)));
        hotRow.addView(waterBadge, new LinearLayout.LayoutParams(dp(40), dp(40)));
        LinearLayout hotText = Ui.column(this);
        hotText.setPadding(dp(10), 0, 0, 0);
        hotText.addView(Ui.medium(this, "热水服务", 13, Ui.TEXT));
        hotText.addView(Ui.text(this, "广州城市职业学院宿舍热水服务", 9.5f, Ui.TEXT_FAINT),
                topMargin(2));
        hotRow.addView(hotText, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        hotRow.addView(new LineIconView(this, LineIconView.CHEVRON_RIGHT, Ui.TEXT_FAINT),
                new LinearLayout.LayoutParams(dp(18), dp(18)));
        hot.addView(hotRow);
        hot.setOnClickListener(v -> openHotWaterService());
        page.addView(hot, topMargin(8));

        page.addView(Ui.bold(this, "阅读与学习", 13, Ui.TEXT), topMargin(20));
        LinearLayout library = Ui.card(this);
        LinearLayout libTop = Ui.row(this);
        libTop.addView(Ui.medium(this, "图书馆", 13, Ui.TEXT));
        libTop.addView(Ui.flexibleGap(this));
        libTop.addView(Ui.text(this, "把好书带回日常", 9.5f, Ui.TEXT_FAINT));
        library.addView(libTop);
        LinearLayout libActions = Ui.row(this);
        libActions.setPadding(0, dp(10), 0, 0);
        libActions.addView(libraryAction("馆藏检索", LineIconView.SEARCH, () -> openLibrary()),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        libActions.addView(libraryAction("借阅 / 续借", LineIconView.BOOK,
                () -> openLibraryOpac()), new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        libActions.addView(libraryAction("电子证", LineIconView.QR, () -> {
            if (qrFile().exists()) showQrImage();
            else showEcardHelp();
        }), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        library.addView(libActions);
        LinearLayout ecardRow = Ui.row(this);
        ecardRow.setPadding(0, dp(8), 0, 0);
        TextView ecardNote = Ui.text(this, "电子证可能长期不变，请勿公开分享", 9.5f, Ui.TEXT_FAINT);
        ecardRow.addView(ecardNote, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView ecardHelp = Ui.medium(this, "如何保存 →", 10, Ui.ACCENT);
        ecardHelp.setOnClickListener(v -> showEcardHelp());
        ecardRow.addView(ecardHelp);
        library.addView(ecardRow);
        TextView ecardPick = Ui.medium(this, "导入 / 更换电子证截图", 10, Ui.ACCENT);
        ecardPick.setOnClickListener(v -> pickQrImage());
        library.addView(ecardPick, topMargin(5));
        page.addView(library, topMargin(8));

        page.addView(Ui.bold(this, "校园服务", 13, Ui.TEXT), topMargin(20));
        LinearLayout quick1 = Ui.row(this);
        quick1.addView(quickCard("教务系统", "课表与考试", LineIconView.DOC,
                () -> openServiceWeb(JWC_HOME_URL)), weightLp(1, 0));
        quick1.addView(quickCard("学工系统", "学时与事务", LineIconView.PERSON,
                this::openStudentAffairs), weightLp(1, 10));
        page.addView(quick1, topMargin(8));
        LinearLayout quick2 = Ui.row(this);
        quick2.addView(quickCard("网上办事大厅", "流程与证明", LineIconView.BUILDING,
                () -> openServiceWeb("https://ehall2.gcp.edu.cn/taskcenter/workflow/index")),
                weightLp(1, 0));
        quick2.addView(quickCard("网上医务室", "预约与问诊", LineIconView.CROSS,
                () -> openServiceWeb("https://yws.gcp.edu.cn/m/")), weightLp(1, 10));
        page.addView(quick2, topMargin(8));
        page.addView(quickCard("学校门户", "综合服务中心与统一认证", LineIconView.HOME,
                () -> openServiceWeb(HOME_URL)), topMargin(8));
        page.addView(quickCard("宿舍报修", "后勤报修 · 水电门窗", LineIconView.BUILDING,
                () -> openServiceWeb(LMS_URL)), topMargin(8));

        TextView footer = Ui.text(this,
                "进入本页自动同步；密码加密保存，验证码手动。非学校官方应用。",
                9.5f, Ui.TEXT_FAINT);
        footer.setGravity(Gravity.CENTER);
        page.addView(footer, topMargin(20));
    }

    private View libraryAction(String label, int icon, Runnable action) {
        LinearLayout col = Ui.column(this);
        col.setGravity(Gravity.CENTER);
        col.setClickable(true);
        col.setFocusable(true);
        col.setPadding(0, dp(4), 0, dp(4));
        col.addView(new LineIconView(this, icon, Ui.ACCENT),
                new LinearLayout.LayoutParams(dp(20), dp(20)));
        TextView text = Ui.text(this, label, 10, Ui.TEXT_SOFT);
        text.setGravity(Gravity.CENTER);
        col.addView(text, topMargin(5));
        col.setOnClickListener(v -> action.run());
        return col;
    }

    private String lastSyncText() {
        if (balanceLastSync <= 0) return "尚未同步";
        return new java.text.SimpleDateFormat("HH:mm", Locale.CHINA)
                .format(new java.util.Date(balanceLastSync));
    }

    private void openOneCardZffw() {
        showTab(TAB_BROWSER);
        oneCardReturnToLife = true;
        oneCardCapture = true;
        prefillOneCard = true;
        balanceSyncing = true;
        balanceNeedsLogin = false;
        balancePollCount = 0;
        balancePollMax = 400;
        browserState.setText("正在打开校内一卡通；登录后会自动保存电子账户余额，App 不保存密码。");
        web.getSettings().setUserAgentString(
                "Mozilla/5.0 (Linux; Android 15; PLC110) AppleWebKit/537.36 "
                        + "(KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36");
        web.loadUrl("https://zffw.gcp.edu.cn/");
        for (int i = 1; i <= 400; i++) {
            web.postDelayed(this::captureOneCardBalance, 1500L * i);
        }
    }

    private void captureOneCardBalance() {
        if (!oneCardCapture) return;
        balancePollCount++;
        String js = "(function(){try{var t=document.body?document.body.innerText:'';"
                + "var m=t.match(/电子账户[\\s\\S]{0,40}?(\\d+(?:\\.\\d+)?)/);"
                + "return m?m[1]:'';}catch(e){return '';}})()";
        web.evaluateJavascript(js, value -> {
            if (value == null) return;
            String b = value.replace("\"", "").trim();
            if (!b.isEmpty() && !"null".equals(b)) {
                oneCardBalance = b;
                oneCardCapture = false;
                prefillOneCard = false;
                balanceSyncing = false;
                balanceNeedsLogin = false;
                balanceLastSync = System.currentTimeMillis();
                prefs.edit().putString("oneCardBalance", b).apply();
                web.loadUrl(HOME_URL);
                if (oneCardReturnToLife && tab == TAB_BROWSER) {
                    oneCardReturnToLife = false;
                    showTab(TAB_LIFE);
                } else if (tab == TAB_LIFE) {
                    renderLife();
                } else {
                    Toast.makeText(this, "电子账户余额已更新", Toast.LENGTH_SHORT).show();
                }
            } else if (balancePollCount >= balancePollMax) {
                oneCardCapture = false;
                balanceSyncing = false;
                balanceNeedsLogin = true;
                if (tab == TAB_LIFE) {
                    renderLife();
                } else {
                    Toast.makeText(this, "余额同步超时；登录完成后回生活页可刷新",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private static final String ONE_CARD_KEY_ALIAS = "gct_one_card_credential";
    private static final String SCHOOL_KEY_ALIAS = "gct_school_credential";

    private SecretKey keyFor(String prefix) throws Exception {
        return key("oneCard".equals(prefix) ? ONE_CARD_KEY_ALIAS : SCHOOL_KEY_ALIAS);
    }

    private SecretKey key(String alias) throws Exception {
        KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if (!ks.containsAlias(alias)) {
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            kg.init(new KeyGenParameterSpec.Builder(alias,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build());
            kg.generateKey();
        }
        return (SecretKey) ks.getKey(alias, null);
    }

    private String encryptPass(String prefix, String plain) {
        try {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, keyFor(prefix));
            byte[] iv = c.getIV();
            byte[] ct = c.doFinal(plain.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "v1:" + Base64.encodeToString(iv, Base64.NO_WRAP)
                    + ":" + Base64.encodeToString(ct, Base64.NO_WRAP);
        } catch (Exception e) {
            return "";
        }
    }

    private String decryptPass(String prefix, String stored) {
        try {
            String dec = decryptWith(keyFor(prefix), stored);
            if (!dec.isEmpty()) return dec;
            if (!"oneCard".equals(prefix)) {
                dec = decryptWith(key(ONE_CARD_KEY_ALIAS), stored);
                if (!dec.isEmpty()) {
                    String re = encryptPass(prefix, dec);
                    if (!re.isEmpty()) prefs.edit().putString(prefix + "PassEnc", re).apply();
                    return dec;
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    private String decryptWith(SecretKey key, String stored) {
        try {
            String[] parts = stored == null ? new String[0] : stored.split(":", 3);
            if (parts.length != 3 || !"v1".equals(parts[0])) return "";
            byte[] iv = Base64.decode(parts[1], Base64.NO_WRAP);
            byte[] ct = Base64.decode(parts[2], Base64.NO_WRAP);
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, iv));
            return new String(c.doFinal(ct), java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private String credentialUser(String prefix) {
        if ("oneCard".equals(prefix) && !oneCardUser.isEmpty()) return oneCardUser;
        String stored = prefs.getString(prefix + "User", "");
        if (stored.startsWith("enc:")) {
            String dec = decryptPass(prefix, stored.substring(4));
            if (!dec.isEmpty()) return dec;
            return "";
        }
        return stored;
    }

    private String credentialPass(String prefix) {
        return decryptPass(prefix, prefs.getString(prefix + "PassEnc", ""));
    }

    private void saveCredential(String prefix, String user, String pass) {
        String enc = prefs.getString(prefix + "PassEnc", "");
        if (pass.isEmpty()) {
            if (enc.isEmpty()) {
                Toast.makeText(this, "请填写密码", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            enc = encryptPass(prefix, pass);
            if (enc.isEmpty()) {
                Toast.makeText(this, "加密失败，未保存", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        String encUser = "enc:" + encryptPass(prefix, user);
        if (encUser.length() <= 4) {
            Toast.makeText(this, "账号加密失败，未保存", Toast.LENGTH_SHORT).show();
            return;
        }
        prefs.edit().putString(prefix + "User", encUser)
                .putString(prefix + "PassEnc", enc).apply();
        if ("oneCard".equals(prefix)) oneCardUser = user;
        Toast.makeText(this, "凭证已保存（验证码仍需手动）", Toast.LENGTH_LONG).show();
        renderSettings();
    }

    private void clearCredential(String prefix) {
        prefs.edit().remove(prefix + "User").remove(prefix + "PassEnc").apply();
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            ks.deleteEntry("oneCard".equals(prefix) ? ONE_CARD_KEY_ALIAS : SCHOOL_KEY_ALIAS);
        } catch (Exception ignored) {
        }
        if ("oneCard".equals(prefix)) oneCardUser = "";
        Toast.makeText(this, "凭证已清除", Toast.LENGTH_SHORT).show();
        renderSettings();
    }

    private void prefillOneCardCredentials() {
        if (!prefillOneCard) return;
        String user = credentialUser("oneCard");
        String pass = credentialPass("oneCard");
        if (user.isEmpty() || pass.isEmpty()) return;
        String js = "(function(){function set(el,v){if(!el)return false;"
                + "var s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;"
                + "s.call(el,v);el.dispatchEvent(new Event('input',{bubbles:true}));"
                + "el.dispatchEvent(new Event('change',{bubbles:true}));return true;}"
                + "var u=document.querySelector('input[placeholder*=\"账号\"]')"
                + "||document.querySelectorAll('input')[0];"
                + "var p=document.querySelector('input[type=password]')"
                + "||document.querySelector('input[placeholder*=\"密码\"]');"
                + "set(u," + jsQuote(user) + ");set(p," + jsQuote(pass) + ");"
                + "return 1;})()";
        web.evaluateJavascript(js, null);
    }

    private static String jsQuote(String value) {
        return JSONObject.quote(value)
                .replace("\u2028", "\\u2028")
                .replace("\u2029", "\\u2029");
    }

    private void prefillSchoolCredentials() {
        prefillSchoolInto(web);
    }

    private void prefillSchoolInto(WebView target) {
        String user = credentialUser("school");
        String pass = credentialPass("school");
        if (user.isEmpty() || pass.isEmpty()) return;
        String js = "(function(){function set(el,v){if(!el)return false;"
                + "var s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;"
                + "s.call(el,v);el.dispatchEvent(new Event('input',{bubbles:true}));"
                + "el.dispatchEvent(new Event('change',{bubbles:true}));return true;}"
                + "var u=null,p=null,ins=document.querySelectorAll('input');"
                + "for(var i=0;i<ins.length;i++){var el=ins[i],t=(el.type||'').toLowerCase();"
                + "if(t==='password'&&!p){p=el;}"
                + "else if(!u&&(t==='text'||t==='tel'||t==='email'||t==='')&&el.offsetParent!==null){u=el;}}"
                + "set(u," + jsQuote(user) + ");set(p," + jsQuote(pass) + ");"
                + "return 1;})()";
        target.evaluateJavascript(js, null);
    }

    private LinearLayout buildCredentialCard(String title, String prefix,
                                             String accountHint, String note) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.medium(this, title, 14, Ui.TEXT));
        card.addView(Ui.text(this, note, 10.5f, Ui.TEXT_SOFT), topMargin(4));

        EditText user = new EditText(this);
        user.setHint(accountHint);
        user.setSingleLine(true);
        user.setText(credentialUser(prefix));
        user.setTextSize(13.5f);
        user.setTextColor(Ui.TEXT);
        user.setHintTextColor(Ui.TEXT_FAINT);
        user.setInputType(InputType.TYPE_CLASS_TEXT);
        user.setPadding(dp(10), 0, dp(10), 0);
        user.setBackground(Ui.roundStroke(0xFFF6F7F9, 10, Ui.LINE, 1, this));
        card.addView(user, editLp());

        String stored = prefs.getString(prefix + "PassEnc", "");
        EditText pass = new EditText(this);
        pass.setHint(stored.isEmpty() ? "登录密码" : "已保存，留空不修改");
        pass.setSingleLine(true);
        pass.setTextSize(13.5f);
        pass.setTextColor(Ui.TEXT);
        pass.setHintTextColor(Ui.TEXT_FAINT);
        pass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        pass.setPadding(dp(10), 0, dp(10), 0);
        pass.setBackground(Ui.roundStroke(0xFFF6F7F9, 10, Ui.LINE, 1, this));
        card.addView(pass, editLp());

        LinearLayout actions = Ui.row(this);
        card.addView(actions, topMargin(8));
        TextView save = Ui.action(this, "保存凭证", true);
        save.setOnClickListener(v -> {
            String u = user.getText().toString().trim();
            if (u.isEmpty()) {
                Toast.makeText(this, "请填写账号", Toast.LENGTH_SHORT).show();
                return;
            }
            saveCredential(prefix, u, pass.getText().toString());
        });
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(44), 1));

        if (!stored.isEmpty()) {
            TextView clear = Ui.ghost(this, "清除凭证", 11);
            clear.setOnClickListener(v -> clearCredential(prefix));
            card.addView(clear, topMargin(6));
        }
        return card;
    }

    private LinearLayout.LayoutParams editLp() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(44));
        lp.topMargin = dp(6);
        return lp;
    }

    private void openHotWaterService() {
        resolveHotWaterScheme(HOT_WATER_URL);
    }

    private void resolveHotWaterScheme(final String link) {
        Toast.makeText(this, "正在获取热水入口…", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            String scheme = null;
            try {
                String html = fetchHotWaterPage(link);
                scheme = findWeixinScheme(html);
                if (scheme == null) {
                    String shortUrl = findWxmpUrl(html);
                    if (shortUrl != null) {
                        scheme = findWeixinScheme(fetchHotWaterPage(shortUrl));
                    }
                }
            } catch (Exception ignored) {
            }
            final String found = scheme;
            runOnUiThread(() -> {
                if (found != null) {
                    launchHotWaterScheme(found);
                } else {
                    Toast.makeText(this, "没有取到当前热水链接，请稍后重试",
                            Toast.LENGTH_LONG).show();
                }
            });
        }).start();
    }

    private void launchHotWaterScheme(String scheme) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(scheme));
            intent.setPackage("com.tencent.mm");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "未安装微信或链接失效", Toast.LENGTH_LONG).show();
        }
    }

    private String fetchHotWaterPage(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(8000);
        connection.setReadTimeout(10000);
        connection.setInstanceFollowRedirects(true);
        connection.setRequestProperty("User-Agent", WECHAT_UA);
        connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
        try (InputStream in = connection.getInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            int total = 0;
            while ((read = in.read(buffer)) > 0) {
                total += read;
                if (total > 1024 * 1024) break;
                out.write(buffer, 0, read);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }
    }

    private String findWeixinScheme(String html) {
        if (html == null) return null;
        Matcher matcher = Pattern.compile(
                "url_scheme\\s*[:=]\\s*['\"](weixin://[^'\"]+)['\"]").matcher(html);
        if (matcher.find()) return matcher.group(1);
        String key = "weixin://dl/business/?t=";
        int last = -1;
        int from = 0;
        while (true) {
            int index = html.indexOf(key, from);
            if (index < 0) break;
            last = index;
            from = index + 1;
        }
        if (last < 0) return null;
        int end = last + key.length();
        while (end < html.length()) {
            char c = html.charAt(end);
            if (!(Character.isLetterOrDigit(c) || c == '_' || c == '-')) break;
            end++;
        }
        return end > last + key.length() ? html.substring(last, end) : null;
    }

    private String findWxmpUrl(String html) {
        if (html == null) return null;
        Matcher matcher = Pattern.compile("https://wxmpurl\\.cn/[A-Za-z0-9_-]+").matcher(html);
        return matcher.find() ? matcher.group() : null;
    }

    private void openServiceWeb(String url) {
        showTab(TAB_BROWSER);
        browserState.setText("正在打开校园服务…");
        web.loadUrl(url);
    }

    private void openLibrary() {
        openLibraryWeb("https://lib.gcp.edu.cn/");
    }

    private void showEcardHelp() {
        new AlertDialog.Builder(this)
                .setTitle("保存电子证二维码")
                .setMessage("1. 微信打开学校/图书馆电子证页面，截屏或保存二维码。\n\n"
                        + "2. 回到广城通 → 生活 → 图书馆 → 导入 / 更换电子证截图。\n\n"
                        + "3. 选择截图；应用只把图片保存在本机，不上传。进馆时点「借阅证 / 电子证」全屏亮码。\n\n"
                        + "电子证二维码可能是固定不变的，也可能由学校轮换；请勿公开分享，换证后重新导入。"
                        + "卸载应用或清除数据会删除本机图片。")
                .setPositiveButton("选择截图", (d, w) -> pickQrImage())
                .setNegativeButton("稍后", null)
                .show();
    }

    private void openLibraryCard() {
        if (qrFile().exists()) {
            showQrImage();
            return;
        }
        openLibraryWeb("https://opac.gzlib.org.cn/opac/special/toGzcsOpac");
    }

    private void openLibraryOpac() {
        openLibraryWeb("https://opac.gzlib.org.cn/opac/special/toGzcsOpac");
    }

    private File qrFile() {
        return new File(getFilesDir(), "library_card_qr.png");
    }

    private void pickQrImage() {
        Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
        pick.setType("image/*");
        startActivityForResult(Intent.createChooser(pick, "选择电子证截图"), REQUEST_QR);
    }

    private void showQrImage() {
        showImageFile(qrFile(), "还没有保存电子证二维码");
    }

    private void showImageFile(File file, String emptyHint) {
        if (!file.exists()) {
            Toast.makeText(this, emptyHint, Toast.LENGTH_SHORT).show();
            return;
        }
        android.graphics.BitmapFactory.Options options =
                new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(file.getAbsolutePath(), options);
        int sample = 1;
        while (options.outWidth / sample > 2048 || options.outHeight / sample > 2048) {
            sample *= 2;
        }
        options.inJustDecodeBounds = false;
        options.inSampleSize = sample;
        showImageBitmap(BitmapFactory.decodeFile(file.getAbsolutePath(), options));
    }

    private void showImageBitmap(android.graphics.Bitmap bitmap) {
        if (bitmap == null) {
            Toast.makeText(this, "二维码读取失败", Toast.LENGTH_SHORT).show();
            return;
        }
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout box = new FrameLayout(this);
        box.setBackgroundColor(Color.WHITE);
        ImageView image = new ImageView(this);
        image.setImageBitmap(bitmap);
        image.setScaleType(ImageView.ScaleType.FIT_CENTER);
        image.setAdjustViewBounds(true);
        box.addView(image, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView close = Ui.text(this, "关闭", 12, Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.round(0xCC1F2A37, 18, this));
        close.setOnClickListener(v -> dialog.dismiss());
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(dp(64), dp(36),
                Gravity.TOP | Gravity.END);
        closeLp.topMargin = dp(14);
        closeLp.rightMargin = dp(14);
        box.addView(close, closeLp);
        dialog.setContentView(box);
        dialog.show();
        box.setAlpha(0f);
        box.animate().alpha(1f).setDuration(280)
                .setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f)).start();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_QR || resultCode != RESULT_OK
                || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String scheme = uri.getScheme();
        if (!"content".equalsIgnoreCase(scheme) && !"file".equalsIgnoreCase(scheme)) {
            Toast.makeText(this, "不支持的图片来源", Toast.LENGTH_SHORT).show();
            return;
        }
        String mime = getContentResolver().getType(uri);
        if (mime == null || !mime.startsWith("image/")) {
            Toast.makeText(this, "请选择图片文件", Toast.LENGTH_SHORT).show();
            return;
        }
        File tmp = new File(getFilesDir(), "library_card_qr.tmp");
        try (InputStream in = getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(tmp)) {
            if (in == null) throw new IllegalStateException("无法读取图片");
            byte[] buffer = new byte[8192];
            int read;
            long total = 0;
            while ((read = in.read(buffer)) > 0) {
                total += read;
                if (total > 12L * 1024 * 1024) {
                    throw new IllegalStateException("图片太大");
                }
                out.write(buffer, 0, read);
            }
            if (tmp.length() == 0) throw new IllegalStateException("图片为空");
        } catch (Exception e) {
            tmp.delete();
            Toast.makeText(this, "保存失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        if (qrFile().exists() && !qrFile().delete()) {
            tmp.delete();
            Toast.makeText(this, "替换失败，请重试", Toast.LENGTH_LONG).show();
            return;
        }
        if (!tmp.renameTo(qrFile())) {
            tmp.delete();
            Toast.makeText(this, "保存失败，请重试", Toast.LENGTH_LONG).show();
            return;
        }
        Toast.makeText(this, "电子证二维码已保存", Toast.LENGTH_SHORT).show();
        showQrImage();
    }

    private void openLibraryWeb(String startUrl) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        FrameLayout box = new FrameLayout(this);
        WebView lib = new WebView(this);
        WebSettings s = lib.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSaveFormData(false);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setGeolocationEnabled(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        WebView.setWebContentsDebuggingEnabled(false);
        CookieManager.getInstance().setAcceptThirdPartyCookies(lib, false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 15; PLC110) AppleWebKit/537.36 "
                + "(KHTML, like Gecko) Chrome/129.0.0.0 Mobile Safari/537.36 "
                + "MicroMessenger/8.0.50.2701(0x28003252) WeChat/arm64 Weixin NetType/WIFI Language/zh_CN");
        lib.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest request) {
                return !isAllowedLibraryUrl(request.getUrl());
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                Uri parsed = Uri.parse(url);
                if ("https".equalsIgnoreCase(parsed.getScheme())
                        && "auth.gcp.edu.cn".equals(parsed.getHost())) {
                    v.postDelayed(() -> prefillSchoolInto(v), 1200);
                }
            }
        });
        lib.loadUrl(startUrl);
        box.addView(lib, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView close = Ui.text(this, "关闭", 12, Color.WHITE);
        close.setGravity(Gravity.CENTER);
        close.setBackground(Ui.round(0xCC1F2A37, 18, this));
        close.setOnClickListener(v -> dialog.dismiss());
        FrameLayout.LayoutParams closeLp = new FrameLayout.LayoutParams(dp(64), dp(36),
                Gravity.TOP | Gravity.END);
        closeLp.topMargin = dp(14);
        closeLp.rightMargin = dp(14);
        box.addView(close, closeLp);
        dialog.setContentView(box);
        dialog.setOnDismissListener(d -> lib.destroy());
        dialog.show();
    }

    private LinearLayout.LayoutParams topMargin(int value) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(value);
        return lp;
    }

    private View buildHero() {
        LinearLayout head = Ui.column(this);

        if (data.isEmpty()) {
            head.addView(Ui.medium(this, "广城通 · 校园版 " + VERSION, 10.5f, Ui.TEAL));
            head.addView(Ui.bold(this, "还没有课表", 32, Ui.TEXT), topMargin(8));
            TextView desc = Ui.text(this,
                    "连接学校门户，登录后进入个人课表读取。课程、周次与待确认项只保存在本机。",
                    12, Ui.TEXT_SOFT);
            desc.setLineSpacing(dp(3), 1f);
            head.addView(desc, topMargin(8));
            return head;
        }

        head.addView(Ui.text(this, termLabel(), 11, Ui.TEXT_FAINT));
        LinearLayout weekRow = Ui.row(this);
        weekRow.addView(Ui.bold(this, TermCalendar.weekTitle(homeWeek()), 42, Ui.TEXT));
        TextView mark = Ui.medium(this,
                autoWeek && currentWeek() >= 1 && currentWeek() <= MAX_WEEK ? "本周" : "教学周",
                10.5f, Ui.TEXT_FAINT);
        LinearLayout.LayoutParams markLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        markLp.leftMargin = dp(10);
        markLp.topMargin = dp(20);
        weekRow.addView(mark, markLp);
        head.addView(weekRow, topMargin(2));

        LocalDate today = LocalDate.now();
        String date = TermCalendar.monthDay(today) + " · " + TermCalendar.weekday(today);
        if (termStart != null) date += " ｜ " + TermCalendar.shortRange(termStart, homeWeek());
        head.addView(Ui.text(this, date, 12, Ui.TEXT_SOFT), topMargin(4));

        String summary = "本周 " + data.countInWeek(homeWeek()) + " 条排课";
        if (data.hasPending()) summary += " · " + data.pending.size() + " 项待确认";
        head.addView(Ui.text(this, summary, 11, Ui.TEXT_FAINT), topMargin(5));
        if (!data.lastSync.isEmpty()) {
            head.addView(Ui.text(this, "更新 " + data.lastSync, 9.5f, Ui.TEXT_FAINT), topMargin(2));
        }

        LinearLayout actions = Ui.row(this);
        head.addView(actions, topMargin(12));
        TextView open = Ui.action(this, "查看课表", true);
        open.setOnClickListener(v -> showTab(TAB_WEEK));
        actions.addView(open);
        TextView update = Ui.action(this, "更新", false);
        update.setOnClickListener(v -> showTab(TAB_BROWSER));
        LinearLayout.LayoutParams updateLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        updateLp.leftMargin = dp(8);
        actions.addView(update, updateLp);

        TextView student = Ui.ghost(this, "学工", 12);
        student.setOnClickListener(v -> openStudentAffairs());
        LinearLayout.LayoutParams studentLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(40));
        studentLp.leftMargin = dp(4);
        actions.addView(student, studentLp);
        return head;
    }

    private View buildTodayCard() {
        LinearLayout card = Ui.card(this);
        LocalDate today = LocalDate.now();
        int week = homeWeek();
        int day = today.getDayOfWeek().getValue();
        List<CourseParser.Course> list = data.coursesOn(day, week);

        LinearLayout title = Ui.row(this);
        title.addView(Ui.medium(this, "今天", 15, Ui.TEXT));
        TextView date = Ui.text(this, TermCalendar.weekday(today) + " " + TermCalendar.shortMonthDay(today),
                10.5f, Ui.TEXT_FAINT);
        LinearLayout.LayoutParams dateLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateLp.leftMargin = dp(8);
        title.addView(date, dateLp);
        title.addView(Ui.flexibleGap(this));
        if (!list.isEmpty()) title.addView(countPill(list.size() + " 节"));
        card.addView(title);

        LinearLayout listBox = Ui.column(this);
        card.addView(listBox, topMargin(8));

        if (data.isEmpty()) {
            listBox.addView(Ui.text(this, "读取课表后，这里会显示今天的课程。", 12, Ui.TEXT_SOFT));
        } else if (termStart != null && currentWeek() <= 0) {
            listBox.addView(Ui.text(this, "学期尚未开始。", 13.5f, Ui.TEXT_SOFT));
            listBox.addView(Ui.text(this, "第 1 周从 " + TermCalendar.fullDate(termStart) + " 开始。", 11, Ui.TEXT_FAINT));
        } else if (termStart != null && currentWeek() > MAX_WEEK) {
            listBox.addView(Ui.text(this, "已超过 30 个教学周。", 13.5f, Ui.TEXT_SOFT));
        } else if (list.isEmpty()) {
            listBox.addView(Ui.medium(this, "今天没有排课", 14, Ui.TEXT_SOFT));
            listBox.addView(Ui.text(this, "本周共 " + data.countInWeek(week) + " 条排课，可在课表中切换周次。",
                    11, Ui.TEXT_FAINT), topMargin(4));
        } else {
            for (int i = 0; i < list.size(); i++) {
                listBox.addView(courseRow(list.get(i), week));
                if (i < list.size() - 1) listBox.addView(Ui.divider(this));
            }
        }

        if (termStart == null && !data.isEmpty()) {
            TextView manual = Ui.text(this, "当前按手动选择的第 " + viewWeek + " 周显示", 9.5f, Ui.TEXT_FAINT);
            card.addView(manual, topMargin(7));
        }

        if (!data.isEmpty()) {
            LinearLayout.LayoutParams overviewDividerLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
            overviewDividerLp.topMargin = dp(11);
            card.addView(Ui.divider(this), overviewDividerLp);
            LinearLayout overviewHead = Ui.row(this);
            overviewHead.addView(Ui.medium(this, "本周概览", 11.5f, Ui.TEXT_SOFT));
            overviewHead.addView(Ui.flexibleGap(this));
            overviewHead.addView(Ui.text(this, TermCalendar.weekTitle(week), 9.5f, Ui.TEXT_FAINT));
            card.addView(overviewHead, topMargin(9));

            LinearLayout daysRow = Ui.row(this);
            int todayDay = today.getDayOfWeek().getValue();
            for (int d = 1; d <= 7; d++) {
                boolean isToday = d == todayDay && (termStart == null || currentWeek() == week);
                LinearLayout cell = Ui.column(this);
                cell.setGravity(Gravity.CENTER);
                cell.setPadding(dp(2), dp(5), dp(2), dp(5));
                cell.setBackground(Ui.round(isToday ? Ui.TEAL_SOFT : 0x00000000, 10, this));
                cell.addView(Ui.text(this, TermCalendar.DAYS[d - 1].substring(1), 9,
                        isToday ? Ui.TEAL : Ui.TEXT_FAINT));
                int count = data.countOn(d, week);
                cell.addView(Ui.medium(this, count == 0 ? "–" : String.valueOf(count), 13,
                        count == 0 ? Ui.TEXT_FAINT : isToday ? Ui.TEAL : Ui.TEXT), topMargin(2));
                cell.setOnClickListener(v -> showTab(TAB_WEEK));
                LinearLayout.LayoutParams cellLp = new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
                cellLp.setMargins(dp(1), 0, dp(1), 0);
                daysRow.addView(cell, cellLp);
            }
            card.addView(daysRow, topMargin(4));
        }
        return card;
    }

    private TextView countPill(String text) {
        TextView t = Ui.medium(this, text, 10, Ui.TEAL);
        t.setPadding(dp(9), dp(3), dp(9), dp(3));
        t.setBackground(Ui.round(Ui.TEAL_SOFT, 20, this));
        return t;
    }

    private View courseRow(final CourseParser.Course course, final int week) {
        LinearLayout row = Ui.row(this);
        row.setPadding(dp(2), dp(8), dp(2), dp(8));
        row.setBackground(Ui.ripple(0x00000000, 10, 0x1014395C, this));
        row.setClickable(true);
        row.setFocusable(true);

        View bar = new View(this);
        bar.setBackground(Ui.round(WeekGrid.accentColor(course.name), 2, this));
        row.addView(bar, new LinearLayout.LayoutParams(dp(4), dp(34)));

        LinearLayout info = Ui.column(this);
        LinearLayout.LayoutParams infoLp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        infoLp.leftMargin = dp(10);
        row.addView(info, infoLp);

        TextView name = Ui.medium(this, course.name, 13, Ui.TEXT);
        name.setSingleLine(true);
        name.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(name);

        String room = course.room == null || course.room.isEmpty() ? "地点待确认" : course.room;
        StringBuilder sub = new StringBuilder("第 ").append(course.start).append("–").append(course.end)
                .append(" 节");
        String timeRange = PeriodTimes.range(course.start, course.end);
        if (!timeRange.isEmpty()) sub.append(" · ").append(timeRange);
        sub.append(" · ").append(room);
        if (showTeacher && course.teacher != null && !course.teacher.isEmpty()) {
            sub.append(" · ").append(course.teacher);
        }
        TextView meta = Ui.text(this, sub.toString(), 10.5f, Ui.TEXT_SOFT);
        meta.setSingleLine(true);
        meta.setEllipsize(TextUtils.TruncateAt.END);
        info.addView(meta, topMargin(3));

        final boolean conflict = data.overlaps(course, week);
        if (conflict) {
            TextView badge = Ui.medium(this, "重叠", 9.5f, Ui.DANGER);
            badge.setPadding(dp(7), dp(2), dp(7), dp(2));
            badge.setBackground(Ui.round(0xFFFFE9E7, 10, this));
            row.addView(badge);
        } else {
            row.addView(Ui.text(this, "›", 17, Ui.TEXT_FAINT));
        }
        row.setOnClickListener(v -> CourseDetailDialog.show(this, course, conflict));
        return row;
    }

    private View buildWeekNudge() {
        LinearLayout card = Ui.tintedCard(this, Ui.INFO_BG, 16);
        LinearLayout top = Ui.row(this);
        top.addView(Ui.medium(this, "让周次自动对号", 13.5f, Ui.NAVY));
        top.addView(Ui.flexibleGap(this));
        TextView go = Ui.ghost(this, "设置", 12);
        go.setOnClickListener(v -> pickTermStart());
        top.addView(go);
        card.addView(top);
        TextView desc = Ui.text(this,
                "填写第 1 周的周一，应用会自动定位当前教学周，并在课表中标出今天。",
                11, Ui.TEXT_SOFT);
        desc.setLineSpacing(dp(2), 1f);
        card.addView(desc, topMargin(5));
        card.setOnClickListener(v -> pickTermStart());
        return card;
    }

    private View buildPendingCard() {
        LinearLayout card = Ui.tintedCard(this, Ui.WARN_BG, 16);
        LinearLayout top = Ui.row(this);
        top.addView(Ui.medium(this, data.pending.size() + " 项待确认", 13.5f, Ui.WARN_TEXT));
        top.addView(Ui.flexibleGap(this));
        TextView go = Ui.ghost(this, "查看", 12);
        go.setOnClickListener(v -> showPendingDialog(data.pending));
        top.addView(go);
        card.addView(top);
        card.addView(Ui.text(this, "其它课程未提供明确的星期或节次，因此没有排入周课表。", 11, 0xFF7A6339),
                topMargin(5));
        card.setOnClickListener(v -> showPendingDialog(data.pending));
        return card;
    }

    // ---------------------------------------------------------------- week

    private void renderWeek() {
        weekScreen.removeAllViews();
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        weekScreen.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout page = Ui.column(this);
        int side = sidePadding(760);
        page.setPadding(dp(side), dp(2), dp(side), dp(30));
        scroll.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        boolean current = currentWeek() >= 1 && currentWeek() == viewWeek;
        if (selectedDay < 1 || selectedDay > 7) {
            selectedDay = current ? LocalDate.now().getDayOfWeek().getValue() : 1;
        }

        page.addView(Ui.text(this, termLabel(), 10.5f, Ui.TEXT_SOFT));
        LinearLayout heroRow = Ui.row(this);
        heroRow.addView(Ui.heading(this, "这一周的节奏", 26, Ui.TEXT),
                new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        TextView weekPill = Ui.medium(this, TermCalendar.weekTitle(viewWeek), 10.5f, Ui.ACCENT);
        weekPill.setPadding(dp(11), dp(6), dp(11), dp(6));
        weekPill.setBackground(Ui.round(Ui.ACCENT_SOFT, 12, this));
        weekPill.setOnClickListener(v -> showWeekPicker());
        heroRow.addView(weekPill);
        page.addView(heroRow, topMargin(5));

        page.addView(weekSelector(viewWeek), topMargin(14));

        LocalDate selectedDate = dateFor(viewWeek, selectedDay);
        int count = data.coursesOn(selectedDay, viewWeek).size();
        LinearLayout summary = Ui.row(this);
        String dateLine = selectedDate == null
                ? TermCalendar.DAYS[selectedDay - 1]
                : TermCalendar.shortMonthDay(selectedDate) + " " + TermCalendar.weekday(selectedDate);
        summary.addView(Ui.text(this, dateLine + " · " + count + " 已知课程", 10.5f, Ui.TEXT_SOFT));
        summary.addView(Ui.flexibleGap(this));
        TextView dayChip = Ui.chip(this, "日程", !scheduleWeekMode);
        dayChip.setOnClickListener(v -> {
            if (!scheduleWeekMode) return;
            scheduleWeekMode = false;
            renderWeek();
        });
        summary.addView(dayChip);
        LinearLayout.LayoutParams weekChipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        weekChipLp.leftMargin = dp(5);
        TextView weekChip = Ui.chip(this, "周览", scheduleWeekMode);
        weekChip.setOnClickListener(v -> {
            if (scheduleWeekMode) return;
            scheduleWeekMode = true;
            renderWeek();
        });
        summary.addView(weekChip, weekChipLp);
        page.addView(summary, topMargin(12));

        if (data.isEmpty()) {
            LinearLayout empty = Ui.card(this);
            empty.addView(Ui.medium(this, "尚无结构化课表", 15, Ui.TEXT));
            empty.addView(Ui.text(this, "连接教务并同步后，这里会按日显示 11 节时间轨道。",
                    10.5f, Ui.TEXT_SOFT), topMargin(4));
            TextView connect = Ui.action(this, "连接教务", true);
            connect.setOnClickListener(v -> showTab(TAB_BROWSER));
            empty.addView(connect, topMargin(10));
            page.addView(empty, topMargin(8));
        } else if (scheduleWeekMode) {
            page.addView(weekGridCard(viewWeek), topMargin(8));
        } else {
            page.addView(dayTimeline(viewWeek, selectedDay), topMargin(8));
        }

        if (termStart == null) page.addView(buildWeekNudge(), topMargin(14));
        TextView hint = Ui.text(this, "点课程查看详情 · 周览可左右/上下滑动", 9.5f, Ui.TEXT_FAINT);
        hint.setGravity(Gravity.CENTER);
        page.addView(hint, topMargin(14));
    }

    private View weekSelector(int week) {
        LinearLayout row = Ui.row(this);
        int today = LocalDate.now().getDayOfWeek().getValue();
        boolean current = currentWeek() == week;
        for (int i = 1; i <= 7; i++) {
            final int day = i;
            boolean selected = day == selectedDay;
            boolean isToday = current && day == today;
            LinearLayout col = Ui.column(this);
            col.setGravity(Gravity.CENTER);
            col.setPadding(dp(2), dp(7), dp(2), dp(7));
            col.setClickable(true);
            col.setFocusable(true);
            col.setBackground(Ui.ripple(selected ? 0x66FFFFFF : 0x00000000, 14, 0x14276757, this));
            if (selected) {
                // A soft white pill marks the selected day; the accent marks today.
                col.setBackground(Ui.rippleDrawable(
                        Ui.roundStroke(0xB8FFFFFF, 14, Ui.CARD_BORDER, 1, this), 0x14276757));
            }
            TextView label = Ui.text(this, TermCalendar.DAYS[i - 1], 9.5f,
                    isToday ? Ui.ACCENT : Ui.TEXT_FAINT);
            label.setGravity(Gravity.CENTER);
            col.addView(label, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            LocalDate date = dateFor(week, day);
            TextView number = Ui.medium(this, date == null ? "–" : String.valueOf(date.getDayOfMonth()),
                    15, isToday ? Ui.ACCENT : Ui.TEXT);
            number.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams numberLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            numberLp.topMargin = dp(2);
            col.addView(number, numberLp);
            row.addView(col, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1));
            col.setOnClickListener(v -> {
                selectedDay = day;
                renderWeek();
            });
        }
        return row;
    }

    private View dayTimeline(int week, int day) {
        final int rowH = dp(46);
        FrameLayout timeline = new FrameLayout(this);
        timeline.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, rowH * 11));

        LinearLayout background = Ui.column(this);
        timeline.addView(background, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        for (int p = 1; p <= 11; p++) {
            LinearLayout row = Ui.row(this);
            row.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, rowH - 1));
            LinearLayout timeCell = Ui.column(this);
            timeCell.setGravity(Gravity.CENTER);
            TextView time = Ui.text(this, PeriodTimes.start(p), 10, Ui.TEXT_SOFT);
            time.setGravity(Gravity.CENTER);
            timeCell.addView(time, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TextView section = Ui.text(this, "第" + p + "节", 8.5f, Ui.TEXT_FAINT);
            section.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            sectionLp.topMargin = dp(1);
            timeCell.addView(section, sectionLp);
            row.addView(timeCell, new LinearLayout.LayoutParams(dp(62),
                    ViewGroup.LayoutParams.MATCH_PARENT));
            View spacer = new View(this);
            row.addView(spacer, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
            if (p == 5 || p == 9) {
                TextView part = Ui.text(this, p == 5 ? "下午" : "晚间", 9.5f, Ui.TEXT_FAINT);
                LinearLayout.LayoutParams partLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                partLp.rightMargin = dp(10);
                row.addView(part, partLp);
            }
            background.addView(row);
            View line = new View(this);
            line.setBackgroundColor(Ui.LINE_SOFT);
            background.addView(line, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 1));
        }

        for (CourseParser.Course course : data.coursesOn(day, week)) {
            int span = Math.max(1, Math.min(11, course.end) - course.start + 1);
            LinearLayout card = timelineCourseCard(course, span);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, rowH * span - dp(4));
            lp.leftMargin = dp(64);
            lp.rightMargin = dp(6);
            lp.topMargin = (course.start - 1) * rowH + dp(2);
            timeline.addView(card, lp);
        }
        return timeline;
    }

    private LinearLayout timelineCourseCard(CourseParser.Course course, int span) {
        int accent = WeekGrid.accentColor(course.name);
        LinearLayout card = Ui.column(this);
        card.setBackground(Ui.roundStroke(0xC7E8F0EA, 16, 0xE6FFFFFF, 1, this));
        card.setElevation(dp(4));
        card.setClickable(true);
        card.setFocusable(true);
        LinearLayout row = Ui.row(this);
        View rail = new View(this);
        rail.setBackgroundColor(accent);
        row.addView(rail, new LinearLayout.LayoutParams(dp(3),
                ViewGroup.LayoutParams.MATCH_PARENT));
        LinearLayout content = Ui.column(this);
        content.setPadding(dp(11), dp(8), dp(10), dp(8));
        if (span >= 2) {
            String range = PeriodTimes.range(course.start, course.end).replace("–", " — ");
            content.addView(Ui.text(this, range + " · 第 " + course.start + "–" + course.end + " 节",
                    9.5f, Ui.TEXT_SOFT));
        }
        String name = course.name.replace("【调】", "").replace("[调]", "").trim();
        String displayName = name.isEmpty() ? "未命名课程" : name;
        displayName = displayName.replaceAll(
                "(?<=[A-Za-z0-9])(?=[\\u4e00-\\u9fa5])", " ");
        if (displayName.length() > 9) {
            displayName = displayName.substring(0, 8) + "\n" + displayName.substring(8);
        }
        TextView nameView = Ui.bold(this, displayName,
                span >= 3 ? 17 : 14.5f, Ui.TEXT);
        nameView.setMaxWidth(dp(160));
        nameView.setMaxLines(2);
        nameView.setEllipsize(TextUtils.TruncateAt.END);
        nameView.setLineSpacing(dp(1), 1f);
        content.addView(nameView, topMargin(3));
        if (span >= 3) {
            content.addView(Ui.flexibleGap(this));
            String room = course.room == null || course.room.isEmpty() ? "地点待确认" : course.room;
            content.addView(Ui.text(this, room, 9.5f, Ui.TEXT_FAINT));
            if (showTeacher && course.teacher != null && !course.teacher.isEmpty()) {
                content.addView(Ui.text(this, course.teacher, 9.5f, Ui.TEXT_FAINT),
                        topMargin(1));
            }
        }
        LinearLayout bottom = Ui.row(this);
        bottom.addView(Ui.flexibleGap(this));
        LineIconView arrow = new LineIconView(this, LineIconView.ARROW, Ui.ACCENT);
        arrow.setBackground(Ui.round(0x66FFFFFF, 14, this));
        bottom.addView(arrow, new LinearLayout.LayoutParams(dp(26), dp(26)));
        content.addView(bottom, topMargin(4));
        row.addView(content, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.MATCH_PARENT, 1));
        card.addView(row, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        card.setOnClickListener(v -> CourseDetailDialog.show(this, course,
                data.overlaps(course, viewWeek)));
        return card;
    }

    private View weekGridCard(int week) {
        FrameLayout card = new FrameLayout(this);
        card.setBackground(Ui.roundStroke(Ui.CARD, 20, Ui.CARD_BORDER, 1, this));
        card.setClipToOutline(true);
        card.setElevation(dp(4));
        card.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(500)));
        int todayDay = currentWeek() == week ? LocalDate.now().getDayOfWeek().getValue() : 0;
        WeekGrid grid = new WeekGrid(this, data, week, termStart, showTeacher, todayDay,
                course -> CourseDetailDialog.show(this, course, data.overlaps(course, week)));
        card.addView(grid, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return card;
    }

    private LocalDate dateFor(int week, int day) {
        if (termStart == null) return null;
        return TermCalendar.weekStart(termStart, week).plusDays(day - 1L);
    }

    private void changeWeek(int delta) {
        changeWeekTo(Math.max(1, Math.min(MAX_WEEK, viewWeek + delta)));
    }

    private void changeWeekTo(int week) {
        viewWeek = Math.max(1, Math.min(MAX_WEEK, week));
        prefs.edit().putInt("selectedWeek", viewWeek).apply();
        renderWeek();
    }

    private void showWeekPicker() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(600));

        LinearLayout root = Ui.column(this);
        root.setBackground(Ui.round(Ui.CARD, 20, this));
        root.setClipToOutline(true);
        root.setPadding(dp(18), dp(16), dp(18), dp(12));

        LinearLayout head = Ui.row(this);
        head.addView(Ui.bold(this, "选择教学周", 18, Ui.TEXT));
        head.addView(Ui.flexibleGap(this));
        TextView close = Ui.ghost(this, "关闭", 12);
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close);
        root.addView(head);
        root.addView(Ui.text(this, termStart == null
                ? "尚未设置开学日期；周次为手动选择，不代表当前周。"
                : "第 1 周从 " + TermCalendar.fullDate(termStart) + " 开始；日期为参考范围。",
                10, Ui.TEXT_FAINT), topMargin(4));

        ScrollView sv = new MaxHeightScrollView(this, dp(380));
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        svLp.topMargin = dp(10);
        root.addView(sv, svLp);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(5);
        sv.addView(grid, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int current = currentWeek();
        for (int week = 1; week <= MAX_WEEK; week++) {
            final int target = week;
            boolean selected = week == viewWeek;
            boolean isCurrent = week == current;
            LinearLayout cell = Ui.column(this);
            cell.setGravity(Gravity.CENTER);
            cell.setPadding(dp(4), dp(6), dp(4), dp(6));
            int background = selected ? Ui.NAVY : isCurrent ? Ui.TEAL_SOFT : 0xFFF2F6FB;
            cell.setBackground(Ui.ripple(background, 12, selected ? 0x33FFFFFF : 0x2214395C, this));
            cell.setClickable(true);
            cell.setFocusable(true);
            TextView number = Ui.medium(this, TermCalendar.compactWeek(week), 12,
                    selected ? Color.WHITE : isCurrent ? Ui.TEAL : Ui.TEXT_SOFT);
            cell.addView(number);
            String sub;
            if (termStart != null) sub = TermCalendar.shortRange(termStart, week);
            else sub = isCurrent ? "本周" : "";
            if (!sub.isEmpty()) {
                TextView date = Ui.text(this, sub, 8.5f,
                        selected ? 0xCCFFFFFF : isCurrent ? 0xFF5A9E89 : Ui.TEXT_FAINT);
                cell.addView(date, topMargin(1));
            }
            GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
            lp.width = 0;
            lp.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
            lp.setMargins(dp(2), dp(2), dp(2), dp(2));
            grid.addView(cell, lp);
            cell.setOnClickListener(v -> {
                dialog.dismiss();
                changeWeekTo(target);
            });
        }

        LinearLayout footer = Ui.row(this);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLp.topMargin = dp(10);
        root.addView(footer, footerLp);
        if (current >= 1 && current <= MAX_WEEK) {
            TextView back = Ui.action(this, "回到本周", false);
            back.setOnClickListener(v -> {
                dialog.dismiss();
                changeWeekTo(current);
            });
            footer.addView(back);
        }
        footer.addView(Ui.flexibleGap(this));
        TextView done = Ui.action(this, "完成", true);
        done.setOnClickListener(v -> dialog.dismiss());
        footer.addView(done);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.42f);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setGravity(Gravity.CENTER);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
        root.setAlpha(0f);
        root.setScaleX(0.96f);
        root.setScaleY(0.96f);
        root.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(300)
                .setInterpolator(new PathInterpolator(0.22f, 1f, 0.36f, 1f)).start();
    }

    // ---------------------------------------------------------------- hours

    private void renderStudy() {
        studyScreen.removeAllViews();
        LinearLayout page = Ui.column(this);
        int side = sidePadding(860);
        page.setPadding(dp(side), dp(18), dp(side), dp(28));
        studyScreen.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout head = Ui.row(this);
        head.addView(Ui.bold(this, "学业", 24, Ui.TEXT));
        head.addView(Ui.flexibleGap(this));
        if (hoursData != null) {
            TextView refresh = Ui.ghost(this, "刷新", 12);
            refresh.setOnClickListener(v -> fetchHours());
            head.addView(refresh);
        }
        page.addView(head);
        page.addView(Ui.text(this, "学分 · 学时", 11, Ui.TEXT_FAINT), topMargin(3));

        LinearLayout metrics = Ui.card(this);
        LinearLayout metricsRow = Ui.row(this);
        LinearLayout creditCol = Ui.column(this);
        creditCol.addView(Ui.text(this, "学分", 10, Ui.TEXT_FAINT));
        creditCol.addView(Ui.bold(this, "—", 22, Ui.TEXT_FAINT), topMargin(2));
        creditCol.addView(Ui.text(this, "待接入", 9, Ui.TEXT_FAINT));
        metricsRow.addView(creditCol, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout hoursCol = Ui.column(this);
        hoursCol.addView(Ui.text(this, "学时", 10, Ui.TEXT_FAINT));
        String total = hoursData == null ? "—"
                : formatHoursShort(hoursData.total()) + " 学时";
        hoursCol.addView(Ui.bold(this, total, 22,
                hoursData == null ? Ui.TEXT_FAINT : Ui.TEAL), topMargin(2));
        String breakdown = hoursData == null ? "尚未读取"
                : "心理 " + formatHours(hoursData.psyTotal())
                + " · 劳动 " + formatHours(hoursData.laborTotal())
                + " · 创业 " + formatHours(hoursData.ieTotal());
        hoursCol.addView(Ui.text(this, breakdown, 9, Ui.TEXT_FAINT));
        metricsRow.addView(hoursCol, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        metrics.addView(metricsRow);
        page.addView(metrics, topMargin(8));
        page.addView(Ui.text(this, "学分数据后续从教务学业情况接入。",
                9, Ui.TEXT_FAINT), topMargin(4));
        page.addView(Ui.medium(this, "学时明细", 13, Ui.TEXT), topMargin(12));

        if (hoursLoading && hoursData == null) {
            LinearLayout loading = Ui.card(this);
            loading.addView(Ui.medium(this, "正在读取学时…", 14, Ui.NAVY));
            loading.addView(Ui.text(this,
                    "通过学校登录态访问学工移动端；如果出现 HTTP 明文提示，请确认后再继续。",
                    11, Ui.TEXT_SOFT), topMargin(6));
            page.addView(loading, topMargin(14));
            return;
        }

        if (hoursError != null) {
            LinearLayout warn = Ui.tintedCard(this, Ui.WARN_BG, 14);
            TextView message = Ui.text(this, hoursError, 11.5f, Ui.WARN_TEXT);
            message.setLineSpacing(dp(2), 1f);
            warn.addView(message);
            LinearLayout actions = Ui.row(this);
            TextView retry = Ui.action(this, "重新读取", true);
            retry.setOnClickListener(v -> fetchHours());
            actions.addView(retry);
            TextView login = Ui.action(this, "打开学校登录", false);
            login.setOnClickListener(v -> showTab(TAB_BROWSER));
            LinearLayout.LayoutParams loginLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, dp(44));
            loginLp.leftMargin = dp(8);
            actions.addView(login, loginLp);
            warn.addView(actions, topMargin(10));
            page.addView(warn, topMargin(14));
            if (hoursData == null) return;
        }

        if (hoursData == null) {
            LinearLayout intro = Ui.card(this);
            intro.addView(Ui.medium(this, "读取你的学时", 15, Ui.TEXT));
            TextView desc = Ui.text(this,
                    "使用你在学校门户的登录态，读取心理活动、劳动实践和创新创业学时。"
                            + "不读取密码，也不上传数据。",
                    11.5f, Ui.TEXT_SOFT);
            desc.setLineSpacing(dp(2), 1f);
            intro.addView(desc, topMargin(6));
            TextView go = Ui.action(this, "读取学时", true);
            go.setOnClickListener(v -> fetchHours());
            intro.addView(go, topMargin(12));
            intro.addView(Ui.text(this,
                    "如果提示需要登录，请先到「教务」完成学校门户登录。",
                    9.5f, Ui.TEXT_FAINT), topMargin(8));
            page.addView(intro, topMargin(14));
            return;
        }

        page.addView(hoursCard("心理活动学时", hoursData.psy, hoursData.psyTotal(),
                0xFF7B5EA7, 0xFFF0E8FB), topMargin(14));
        page.addView(hoursCard("劳动实践学时", hoursData.labor, hoursData.laborTotal(),
                0xFF2E8B6B, 0xFFE1F3EA), topMargin(12));
        page.addView(hoursCard("创新创业学时", hoursData.ie, hoursData.ieTotal(),
                0xFFC07A2C, 0xFFFFEFDD), topMargin(12));

        if (hoursLoading) {
            page.addView(Ui.text(this, "正在刷新…", 10, Ui.TEXT_FAINT), topMargin(10));
        }
        TextView foot = Ui.text(this,
                "数据来源：学校学工系统 · 仅在本机显示，不上传", 9.5f, Ui.TEXT_FAINT);
        foot.setGravity(Gravity.CENTER);
        page.addView(foot, topMargin(16));
    }

    private LinearLayout hoursCard(String title, List<HoursData.Item> items, double total,
                                   int color, int light) {
        LinearLayout card = Ui.card(this);
        card.setPadding(0, 0, 0, 0);

        LinearLayout header = Ui.column(this);
        header.setBackgroundColor(light);
        header.setPadding(dp(18), dp(14), dp(18), dp(13));
        LinearLayout titleRow = Ui.row(this);
        View dot = new View(this);
        dot.setBackground(Ui.round(color, 3, this));
        titleRow.addView(dot, new LinearLayout.LayoutParams(dp(6), dp(6)));
        TextView name = Ui.medium(this, title, 14, color);
        LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        nameLp.leftMargin = dp(8);
        titleRow.addView(name, nameLp);
        titleRow.addView(Ui.flexibleGap(this));
        titleRow.addView(Ui.bold(this, formatHours(total) + " 学时", 20, color));
        header.addView(titleRow);
        card.addView(header);

        LinearLayout body = Ui.column(this);
        body.setPadding(dp(18), dp(7), dp(18), dp(11));
        if (items.isEmpty()) {
            body.addView(Ui.text(this, "暂无记录", 11.5f, Ui.TEXT_FAINT), topMargin(6));
        } else {
            for (int i = 0; i < items.size(); i++) {
                HoursData.Item item = items.get(i);
                LinearLayout row = Ui.row(this);
                row.setPadding(0, dp(7), 0, dp(7));
                TextView activity = Ui.text(this, item.name, 12, Ui.TEXT);
                activity.setMaxLines(2);
                activity.setEllipsize(TextUtils.TruncateAt.END);
                row.addView(activity, new LinearLayout.LayoutParams(
                        0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
                TextView value = Ui.medium(this, "+" + formatHours(item.hours), 12, color);
                LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                valueLp.leftMargin = dp(10);
                row.addView(value, valueLp);
                body.addView(row);
                if (i < items.size() - 1) body.addView(Ui.divider(this));
            }
        }
        card.addView(body);
        return card;
    }

    private String formatHours(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.001) {
            return String.valueOf((long) Math.rint(value));
        }
        return String.format(Locale.CHINA, "%.1f", value);
    }

    private void fetchHours() {
        if (hoursLoading) return;
        if (!xgxtCleartextOk()) {
            promptXgxtCleartext(this::fetchHours);
            return;
        }
        cancelSync();
        hoursLoading = true;
        hoursError = null;
        hoursStage = 1;
        final int request = ++hoursRequestId;
        renderStudy();
        web.postDelayed(() -> {
            if (request == hoursRequestId && hoursLoading && hoursStage > 0) {
                failHours("读取超时。请确认已登录学校门户后再试。");
            }
        }, 35000);
        web.loadUrl(CAS_MOBILE_URL);
    }

    private void failHours(String message) {
        hoursLoading = false;
        hoursStage = 0;
        hoursError = message;
        hoursRequestId++;
        renderStudy();
    }

    private void runHoursScript(final int attempt) {
        if (hoursStage != 3) return;
        String script;
        try {
            script = readAsset("extract-hours.js");
        } catch (Exception e) {
            failHours("学时读取资源不可用。");
            return;
        }
        web.evaluateJavascript(script, result -> {
            if (hoursStage != 3) return;
            String payload = decodeJs(result);
            try {
                JSONObject obj = new JSONObject(payload);
                if (obj.has("error")) {
                    String error = obj.optString("error");
                    if (("auth".equals(error) || "student".equals(error)) && attempt < 2) {
                        web.postDelayed(() -> runHoursScript(attempt + 1), 1200);
                        return;
                    }
                    if ("auth".equals(error)) {
                        failHours("学校登录已过期，请先去「教务」重新登录。");
                    } else if ("student".equals(error)) {
                        failHours("暂时拿不到学号，请先打开个人课表页再读取。");
                    } else {
                        failHours("读取学时失败：" + error + "。请稍后重试。");
                    }
                    return;
                }
                HoursData parsed = HoursData.from(obj);
                hoursData = parsed;
                hoursLoading = false;
                hoursStage = 0;
                hoursError = null;
                renderStudy();
                Toast.makeText(this, "学时已更新", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                failHours("学时数据格式无法识别，请稍后重试。");
            }
        });
    }

    private String readAsset(String name) throws Exception {
        try (InputStream input = getAssets().open(name);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
            return output.toString("UTF-8");
        }
    }

    private String decodeJs(String value) {
        try {
            Object parsed = new org.json.JSONTokener(value).nextValue();
            return parsed instanceof String ? (String) parsed : "";
        } catch (Exception e) {
            return "";
        }
    }

    // ---------------------------------------------------------------- daily sync

    private void maybeAutoSync() {
        if (data.isEmpty() || syncStage > 0 || hoursStage > 0 || studentBrowserMode) return;
        String today = LocalDate.now().toString();
        if (today.equals(prefs.getString("lastAutoSyncDate", ""))) return;
        web.postDelayed(() -> {
            if (syncStage > 0 || hoursStage > 0 || studentBrowserMode) return;
            String date = LocalDate.now().toString();
            if (date.equals(prefs.getString("lastAutoSyncDate", ""))) return;
            startDailySync(false);
        }, 2200);
    }

    private void startDailySync(boolean manual) {
        if (data.isEmpty()) {
            if (manual) Toast.makeText(this, "请先手动读取一次课表，再启用自动同步", Toast.LENGTH_SHORT).show();
            return;
        }
        if (syncStage > 0) {
            if (manual) Toast.makeText(this, "正在同步，请稍候", Toast.LENGTH_SHORT).show();
            return;
        }
        if (hoursStage > 0) {
            if (manual) Toast.makeText(this, "正在读取学时，请稍后再同步", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!manual && tab == TAB_BROWSER) return;
        syncStage = 1;
        final int request = ++syncRequestId;
        if (tab == TAB_BROWSER) browserScreen.setVisibility(View.VISIBLE);
        browserState.setText("正在从教务同步校历与课表…");
        web.postDelayed(() -> {
            if (request == syncRequestId && syncStage > 0) {
                failSync("同步超时，请稍后在设置中手动同步。");
            }
        }, 60000);
        web.loadUrl(WEEK_QUERY_URL);
    }

    private void cancelSync() {
        if (syncStage > 0) {
            syncStage = 0;
            syncRequestId++;
            if (tab != TAB_BROWSER) browserScreen.setVisibility(View.GONE);
        }
    }

    private void runCalendarScript(final int attempt) {
        if (syncStage != 2) return;
        String script;
        try {
            script = readAsset("extract-calendar.js");
        } catch (Exception e) {
            syncStage = 3;
            web.loadUrl(COURSE_URL);
            return;
        }
        web.evaluateJavascript(script, result -> {
            if (syncStage != 2) return;
            boolean ok = false;
            try {
                JSONObject obj = new JSONObject(decodeJs(result));
                int week = obj.optInt("week");
                LocalDate start = TermCalendar.parse(obj.optString("start"));
                if (week >= 1 && week <= MAX_WEEK && start != null) {
                    LocalDate startMonday = TermCalendar.mondayOf(start.minusWeeks(week - 1L));
                    termStart = startMonday;
                    autoWeek = true;
                    viewWeek = week;
                    prefs.edit().putString("termStart", TermCalendar.iso(startMonday))
                            .putBoolean("autoWeek", true)
                            .putInt("selectedWeek", week)
                            .apply();
                    ok = true;
                }
            } catch (Exception ignored) {
            }
            if (!ok && attempt < 3) {
                web.postDelayed(() -> runCalendarScript(attempt + 1), 900);
                return;
            }
            syncStage = 3;
            browserState.setText("校历已读取，正在同步课表…");
            web.loadUrl(COURSE_URL);
        });
    }

    private void runAutoCourseScript(final int attempt) {
        if (syncStage != 4) return;
        String script;
        try {
            script = readAsset("extract-course.js");
        } catch (Exception e) {
            failSync("课表适配资源不可用，旧课表保留。");
            return;
        }
        web.evaluateJavascript(script, result -> {
            if (syncStage != 4) return;
            try {
                if (result == null || "null".equals(result)) {
                    if (attempt < 2) {
                        web.postDelayed(() -> runAutoCourseScript(attempt + 1), 1200);
                    } else {
                        failSync("课表页面未就绪，旧课表保留。");
                    }
                    return;
                }
                JSONObject obj = new JSONObject(result);
                if (obj.has("error")) {
                    if ("notready".equals(obj.optString("error")) && attempt < 3) {
                        web.postDelayed(() -> runAutoCourseScript(attempt + 1), 1200);
                    } else {
                        failSync("课表页面未就绪，旧课表保留。");
                    }
                    return;
                }
                CourseData candidate = new CourseData();
                candidate.load(obj);
                candidate.term = obj.optString("term", "待确认");
                if (candidate.courses.isEmpty()) {
                    failSync("自动同步没有识别到排课，旧课表保留。");
                    return;
                }
                if (!data.term.isEmpty() && !candidate.term.isEmpty()
                        && !data.term.equals(candidate.term)) {
                    failSync("检测到学期变化，已停止自动替换，请手动读取确认。");
                    return;
                }
                if (!data.courses.isEmpty()
                        && candidate.courses.size() < Math.max(1, data.courses.size() / 2)) {
                    failSync("检测到课表条目明显减少，已停止自动替换，请手动确认。");
                    return;
                }
                String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                        .format(new Date());
                if (candidate.signature().equals(data.signature())) {
                    data.lastSync = stamp;
                    prefs.edit().putString("lastSync", stamp).apply();
                    finishSync("今日课表已是最新");
                    return;
                }
                String raw = buildRawText(candidate, obj, stamp);
                data = candidate;
                data.rawText = raw;
                data.lastSync = stamp;
                prefs.edit()
                        .putString("courseJson", obj.toString())
                        .putString("courseText", raw)
                        .putString("lastSync", stamp)
                        .apply();
                finishSync("今日课表已自动更新（" + data.courses.size() + " 条）");
            } catch (Exception e) {
                failSync("课表数据无法识别，旧课表保留。");
            }
        });
    }

    private void finishSync(String note) {
        syncStage = 0;
        syncRequestId++;
        if (tab != TAB_BROWSER) browserScreen.setVisibility(View.GONE);
        prefs.edit()
                .putString("lastAutoSyncDate", LocalDate.now().toString())
                .putString("lastAutoSyncNote", note)
                .apply();
        refreshVisible();
        Toast.makeText(this, note, Toast.LENGTH_LONG).show();
    }

    private void failSync(String note) {
        syncStage = 0;
        syncRequestId++;
        if (tab != TAB_BROWSER) browserScreen.setVisibility(View.GONE);
        prefs.edit()
                .putString("lastAutoSyncDate", LocalDate.now().toString())
                .putString("lastAutoSyncNote", note)
                .apply();
        refreshVisible();
        Toast.makeText(this, note, Toast.LENGTH_LONG).show();
    }

    // ---------------------------------------------------------------- settings

    private void renderSettings() {
        settingsScreen.removeAllViews();
        LinearLayout page = Ui.column(this);
        int side = sidePadding(760);
        page.setPadding(dp(side), dp(2), dp(side), dp(30));
        settingsScreen.addView(page, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView kicker = Ui.medium(this, "YOUR CAMPUS, YOUR WAY", 9.5f, Ui.ACCENT);
        kicker.setLetterSpacing(0.16f);
        page.addView(kicker);
        page.addView(Ui.heading(this, "按你的习惯来", 26, Ui.TEXT), topMargin(4));

        page.addView(Ui.bold(this, "课表偏好", 12.5f, Ui.TEXT_SOFT), topMargin(18));
        LinearLayout prefsCard = Ui.card(this);
        LinearLayout dateRow = Ui.row(this);
        dateRow.setPadding(0, dp(4), 0, dp(10));
        dateRow.setClickable(true);
        dateRow.setFocusable(true);
        LinearLayout dateText = Ui.column(this);
        dateText.addView(Ui.medium(this, "开学日期", 12.5f, Ui.TEXT));
        String dateValue = termStart == null ? "未设置 · 点击选择"
                : termStart.getYear() + "/" + String.format(Locale.US, "%02d/%02d",
                termStart.getMonthValue(), termStart.getDayOfMonth());
        dateText.addView(Ui.text(this, dateValue, 10, Ui.TEXT_FAINT), topMargin(3));
        dateRow.addView(dateText, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        dateRow.addView(new LineIconView(this, LineIconView.CALENDAR, Ui.TEXT_SOFT),
                new LinearLayout.LayoutParams(dp(18), dp(18)));
        dateRow.setOnClickListener(v -> pickTermStart());
        prefsCard.addView(dateRow);
        prefsCard.addView(Ui.divider(this));

        prefsCard.addView(switchRow("自动跟随当前周",
                termStart == null ? "设置开学日期后按当前日期定位" : "以 " + TermCalendar.fullDate(termStart) + " 为第 1 周",
                autoWeek && termStart != null, (button, checked) -> {
                    if (updatingSwitch) return;
                    if (checked && termStart == null) {
                        updatingSwitch = true;
                        button.setChecked(false);
                        updatingSwitch = false;
                        Toast.makeText(this, "请先设置开学日期", Toast.LENGTH_SHORT).show();
                        pickTermStart();
                        return;
                    }
                    autoWeek = checked;
                    prefs.edit().putBoolean("autoWeek", checked).apply();
                    if (checked) {
                        int current = currentWeek();
                        if (current >= 1 && current <= MAX_WEEK) {
                            viewWeek = current;
                            prefs.edit().putInt("selectedWeek", current).apply();
                        }
                    }
                    refreshVisible();
                }));
        prefsCard.addView(Ui.divider(this));
        prefsCard.addView(switchRow("显示教师", "课程块较窄时会自动省略", showTeacher, (button, checked) -> {
            showTeacher = checked;
            prefs.edit().putBoolean("showTeacher", checked).apply();
            refreshVisible();
        }));
        TextView syncNow = Ui.ghost(this, "从教务同步校历与课表", 11);
        syncNow.setOnClickListener(v -> startDailySync(true));
        prefsCard.addView(syncNow, topMargin(6));
        page.addView(prefsCard, topMargin(8));

        page.addView(Ui.bold(this, "本机凭证", 12.5f, Ui.TEXT_SOFT), topMargin(18));
        LinearLayout credentials = Ui.card(this);
        credentials.addView(settingsRow("学校凭证",
                credentialUser("school").isEmpty() ? "未配置" : "已配置",
                () -> showCredentialEditor("school")));
        credentials.addView(Ui.divider(this));
        credentials.addView(settingsRow("一卡通凭证",
                credentialUser("oneCard").isEmpty() ? "待确认" : "已保存",
                () -> showCredentialEditor("oneCard")));
        page.addView(credentials, topMargin(8));

        page.addView(Ui.bold(this, "隐私与数据", 12.5f, Ui.TEXT_SOFT), topMargin(18));
        LinearLayout privacy = Ui.card(this);
        privacy.addView(settingsRow("隐私与数据须知", "", () -> showDataNotice(null)));
        privacy.addView(Ui.divider(this));
        privacy.addView(settingsRow("首次启动须知", "", () -> showDataNotice(null)));
        privacy.addView(Ui.divider(this));
        LinearLayout clearRow = settingsRow("清除数据", "", this::confirmClear);
        ((TextView) clearRow.getChildAt(0)).setTextColor(Ui.DANGER);
        privacy.addView(clearRow);
        page.addView(privacy, topMargin(8));

        TextView quit = Ui.ghost(this, "退出应用", 11);
        quit.setTextColor(Ui.TEXT_FAINT);
        quit.setOnClickListener(v -> finish());
        page.addView(quit, topMargin(16));

        TextView footer = Ui.text(this, "广城通 " + VERSION + " · 非学校官方应用 · GPL-3.0 · 数据仅保存在本机",
                9, Ui.TEXT_FAINT);
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(dp(2), 1f);
        page.addView(footer, topMargin(8));
    }

    private LinearLayout settingsRow(String title, String value, Runnable action) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, dp(12), 0, dp(12));
        row.setClickable(true);
        row.setFocusable(true);
        row.addView(Ui.medium(this, title, 12.5f, Ui.TEXT));
        row.addView(Ui.flexibleGap(this));
        if (value != null && !value.isEmpty()) {
            LinearLayout.LayoutParams valueLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            valueLp.rightMargin = dp(6);
            row.addView(Ui.text(this, value, 10, Ui.TEXT_FAINT), valueLp);
        }
        row.addView(new LineIconView(this, LineIconView.CHEVRON_RIGHT, Ui.TEXT_FAINT),
                new LinearLayout.LayoutParams(dp(15), dp(15)));
        row.setOnClickListener(v -> action.run());
        return row;
    }

    private void showCredentialEditor(String prefix) {
        String title = "school".equals(prefix) ? "学校统一身份认证" : "一卡通查询";
        String note = "school".equals(prefix)
                ? "门户、教务、学工共用。密码用 Android Keystore 加密，不备份；登录页自动填，验证码手动。"
                : "一卡通手机页使用。密码用 Android Keystore 加密，不备份；登录页自动填，验证码手动。";
        LinearLayout card = buildCredentialCard(title, prefix, "学号 / 工号", note);
        card.setBackground(Ui.round(Ui.CARD, 24, this));
        new AlertDialog.Builder(this)
                .setView(card)
                .setPositiveButton("关闭", null)
                .show();
    }
    private View labelValue(String label, String value) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, dp(8), 0, dp(8));
        TextView l = Ui.text(this, label, 11, Ui.TEXT_FAINT);
        row.addView(l, new LinearLayout.LayoutParams(dp(78), ViewGroup.LayoutParams.WRAP_CONTENT));
        TextView v = Ui.medium(this, value, 12.5f, Ui.TEXT);
        v.setLineSpacing(dp(1), 1f);
        row.addView(v, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        return row;
    }

    private View switchRow(String title, String desc, boolean checked,
                           CompoundButton.OnCheckedChangeListener listener) {
        LinearLayout row = Ui.row(this);
        row.setPadding(0, dp(8), 0, dp(8));
        LinearLayout block = Ui.column(this);
        block.addView(Ui.medium(this, title, 13, Ui.TEXT));
        if (desc != null && !desc.isEmpty()) {
            block.addView(Ui.text(this, desc, 10, Ui.TEXT_FAINT), topMargin(2));
        }
        row.addView(block, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        Switch toggle = new Switch(this);
        toggle.setChecked(checked);
        toggle.setShowText(false);
        toggle.setThumbTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{Ui.TEAL, 0xFFC9D5E1}));
        toggle.setTrackTintList(new ColorStateList(
                new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{0xFF8CCFBD, 0xFFE3EAF2}));
        toggle.setOnCheckedChangeListener(listener);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(10);
        row.addView(toggle, lp);
        return row;
    }

    private void refreshVisible() {
        if (tab == TAB_SETTINGS) renderSettings();
        else if (tab == TAB_HOME) renderHome();
        else if (tab == TAB_WEEK) renderWeek();
        else if (tab == TAB_LIFE) renderLife();
    }

    private void pickTermStart() {
        LocalDate today = LocalDate.now();
        LocalDate initial = termStart != null
                ? termStart
                : TermCalendar.mondayOf(today).minusWeeks(Math.max(0, homeWeek() - 1));
        DatePickerDialog dialog = new DatePickerDialog(this, (view, year, month, day) -> {
            LocalDate picked = TermCalendar.mondayOf(LocalDate.of(year, month + 1, day));
            termStart = picked;
            autoWeek = true;
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("termStart", TermCalendar.iso(picked));
            editor.putBoolean("autoWeek", true);
            int current = currentWeek();
            if (current >= 1 && current <= MAX_WEEK) {
                viewWeek = current;
                editor.putInt("selectedWeek", current);
            }
            editor.apply();
            String message = current >= 1 && current <= MAX_WEEK
                    ? "已保存：今天为第 " + current + " 周"
                    : "已保存开学日期";
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
            refreshVisible();
        }, initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth());
        dialog.setTitle("选择第 1 周的周一");
        dialog.show();
    }

    // ---------------------------------------------------------------- preview

    private void renderPreview() {
        previewScreen.removeAllViews();
        int side = sidePadding(900);
        previewScreen.setPadding(dp(side), dp(14), dp(side), dp(14));
        LinearLayout header = Ui.row(this);
        header.addView(Ui.bold(this, "学校课表原文", 20, Ui.TEXT));
        header.addView(Ui.flexibleGap(this));
        TextView copy = Ui.ghost(this, "复制", 12);
        copy.setOnClickListener(v -> copyRaw());
        header.addView(copy);
        TextView close = Ui.ghost(this, "关闭", 12);
        close.setOnClickListener(v -> showTab(returnTab));
        header.addView(close);
        previewScreen.addView(header);
        previewScreen.addView(Ui.text(this, "保留学校页面原文供核对；不含登录票据或 Cookie。", 10, Ui.TEXT_FAINT));

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setVerticalScrollBarEnabled(false);
        LinearLayout card = Ui.card(this);
        card.setPadding(dp(16), dp(14), dp(16), dp(14));
        String text = data.rawText == null ? "" : data.rawText;
        TextView body = Ui.text(this, text.isEmpty() ? "暂无原文。请先连接学校门户并读取课表。" : text,
                11.5f, text.isEmpty() ? Ui.TEXT_FAINT : Ui.TEXT);
        body.setTextIsSelectable(true);
        body.setLineSpacing(dp(3), 1f);
        card.addView(body);
        sv.addView(card, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        svLp.topMargin = dp(10);
        previewScreen.addView(sv, svLp);
    }

    private void copyRaw() {
        String text = data.rawText == null ? "" : data.rawText;
        if (text.isEmpty()) {
            Toast.makeText(this, "暂无原文可复制", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("学校课表原文", text));
        Toast.makeText(this, "原文已复制到剪贴板", Toast.LENGTH_SHORT).show();
    }

    private void showPendingDialog(List<String> pending) {
        if (pending == null || pending.isEmpty()) {
            Toast.makeText(this, "没有待确认项", Toast.LENGTH_SHORT).show();
            return;
        }
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        int width = Math.min(getResources().getDisplayMetrics().widthPixels - dp(36), dp(640));

        LinearLayout root = Ui.column(this);
        root.setBackground(Ui.round(Ui.CARD, 20, this));
        root.setClipToOutline(true);
        root.setPadding(dp(20), dp(16), dp(20), dp(14));

        LinearLayout head = Ui.row(this);
        head.addView(Ui.bold(this, "未排入周课表的项目", 18, Ui.TEXT));
        head.addView(Ui.flexibleGap(this));
        TextView close = Ui.ghost(this, "关闭", 12);
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close);
        root.addView(head);
        root.addView(Ui.text(this, "这些项目缺少明确的星期或节次，不会被猜测排入周课表。",
                10.5f, Ui.TEXT_FAINT), topMargin(4));

        ScrollView sv = new MaxHeightScrollView(this, dp(340));
        LinearLayout list = Ui.column(this);
        for (String item : pending) {
            LinearLayout box = Ui.tintedCard(this, Ui.WARN_BG, 12);
            TextView text = Ui.text(this, item, 11.5f, 0xFF6B5528);
            text.setLineSpacing(dp(2), 1f);
            box.addView(text);
            LinearLayout.LayoutParams boxLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            boxLp.topMargin = dp(8);
            list.addView(box, boxLp);
        }
        sv.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        LinearLayout.LayoutParams svLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        svLp.topMargin = dp(4);
        root.addView(sv, svLp);

        LinearLayout footer = Ui.row(this);
        LinearLayout.LayoutParams footerLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        footerLp.topMargin = dp(10);
        root.addView(footer, footerLp);
        TextView copy = Ui.ghost(this, "复制全部", 12);
        copy.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("待确认项目",
                    TextUtils.join("\n\n", pending)));
            Toast.makeText(this, "待确认项目已复制", Toast.LENGTH_SHORT).show();
        });
        footer.addView(copy);
        footer.addView(Ui.flexibleGap(this));
        TextView done = Ui.action(this, "知道了", true);
        done.setOnClickListener(v -> dialog.dismiss());
        footer.addView(done);

        dialog.setContentView(root);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setDimAmount(0.42f);
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setGravity(Gravity.CENTER);
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dialog.show();
    }

    // ---------------------------------------------------------------- browser

    private void buildBrowserScreen() {
        browserScreen = Ui.column(this);
        browserScreen.setPadding(0, 0, 0, 0);
        content.addView(browserScreen, 0, full());
        browserScreen.setVisibility(View.GONE);

        studentBar = Ui.row(this);
        studentBar.setVisibility(View.GONE);
        browserScreen.addView(studentBar);
        TextView studentBack = Ui.ghost(this, "‹ 返回", 12);
        studentBack.setOnClickListener(v -> showTab(TAB_HOME));
        studentBar.addView(studentBack);
        studentBar.addView(Ui.flexibleGap(this));
        studentBar.addView(Ui.text(this, "学工移动端", 11, Ui.TEXT_SOFT));
        TextView studentReload = Ui.ghost(this, "刷新", 12);
        studentReload.setOnClickListener(v -> web.reload());
        studentBar.addView(studentReload);

        browserControls = Ui.row(this);
        browserControls.setPadding(dp(8), dp(5), dp(8), dp(3));
        browserScreen.addView(browserControls);

        TextView back = Ui.ghost(this, "‹ 返回", 11);
        back.setOnClickListener(v -> {
            if (web.canGoBack()) web.goBack();
            else Toast.makeText(this, "没有可返回的网页", Toast.LENGTH_SHORT).show();
        });
        browserControls.addView(back);

        TextView reload = Ui.ghost(this, "刷新", 11);
        reload.setOnClickListener(v -> web.reload());
        browserControls.addView(reload);

        browserControls.addView(Ui.flexibleGap(this));

        TextView portal = Ui.ghost(this, "门户", 11);
        portal.setOnClickListener(v -> connectPortal());
        browserControls.addView(portal);

        TextView course = Ui.ghost(this, "课表", 11);
        course.setOnClickListener(v -> {
            browserState.setText("正在打开个人课表查询页…");
            web.loadUrl(COURSE_URL);
        });
        browserControls.addView(course);

        TextView read = Ui.action(this, "读取", true);
        read.setOnClickListener(v -> readCourses());
        browserControls.addView(read);

        browserSecondary = Ui.column(this);
        browserSecondary.setPadding(dp(10), 0, dp(10), dp(2));
        browserScreen.addView(browserSecondary);

        browserAddress = Ui.text(this, "学校门户", 9, Ui.TEXT_FAINT);
        browserAddress.setSingleLine(true);
        browserAddress.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        browserSecondary.addView(browserAddress);

        browserState = Ui.text(this, "点「门户」登录学校账号；进入个人课表后点「读取」。",
                9, Ui.TEXT_SOFT);
        browserState.setSingleLine(true);
        browserState.setEllipsize(TextUtils.TruncateAt.END);
        browserSecondary.addView(browserState);

        browserProgress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        browserProgress.setIndeterminate(true);
        browserProgress.setIndeterminateTintList(ColorStateList.valueOf(Ui.TEAL));
        browserProgress.setVisibility(View.GONE);
        browserScreen.addView(browserProgress, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2)));

        FrameLayout webCard = new FrameLayout(this);
        webCard.setBackgroundColor(Color.WHITE);
        LinearLayout.LayoutParams webLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        browserScreen.addView(webCard, webLp);

        web = new WebView(this);
        web.setBackgroundColor(Color.WHITE);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setSaveFormData(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setSupportMultipleWindows(true);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setGeolocationEnabled(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        WebView.setWebContentsDebuggingEnabled(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebViewClient(client());
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onCreateWindow(WebView view, boolean dialog, boolean gesture, Message msg) {
                WebView child = new WebView(MainActivity.this);
                WebSettings cs = child.getSettings();
                cs.setJavaScriptEnabled(false);
                cs.setDomStorageEnabled(false);
                cs.setAllowFileAccess(false);
                cs.setAllowContentAccess(false);
                cs.setSaveFormData(false);
                cs.setSupportMultipleWindows(false);
                cs.setJavaScriptCanOpenWindowsAutomatically(false);
                cs.setGeolocationEnabled(false);
                cs.setAllowFileAccessFromFileURLs(false);
                cs.setAllowUniversalAccessFromFileURLs(false);
                cs.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
                CookieManager.getInstance().setAcceptThirdPartyCookies(child, false);
                child.setWebViewClient(new WebViewClient() {
                    @Override
                    public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                        String u = r.getUrl().toString();
                        loadTarget(u);
                        v.destroy();
                        return true;
                    }
                });
                ((WebView.WebViewTransport) msg.obj).setWebView(child);
                msg.sendToTarget();
                return true;
            }
        });
        webCard.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        web.loadUrl(HOME_URL);
    }

    private boolean isSchoolHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.US);
        return h.equals("gcp.edu.cn") || h.endsWith(".gcp.edu.cn");
    }

    private boolean isAllowedLibraryHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.US);
        return h.equals("gcp.edu.cn") || h.endsWith(".gcp.edu.cn")
                || h.equals("gzlib.org.cn") || h.endsWith(".gzlib.org.cn")
                || h.equals("10.9.20.13");
    }

    /** Only HTTPS for library hosts; the public OPAC and its internal IP are HTTP exceptions. */
    private boolean isAllowedLibraryUrl(Uri uri) {
        if (uri == null) return false;
        String host = uri.getHost();
        if (!isAllowedLibraryHost(host)) return false;
        String scheme = uri.getScheme();
        if ("https".equalsIgnoreCase(scheme)) return true;
        return "http".equalsIgnoreCase(scheme)
                && ("opac.gzlib.org.cn".equalsIgnoreCase(host)
                || "10.9.20.13".equals(host));
    }

    private boolean allowed(String u) {
        Uri p = Uri.parse(u);
        return "https".equalsIgnoreCase(p.getScheme())
                && isSchoolHost(p.getHost())
                && (p.getPort() == -1 || p.getPort() == 443);
    }

    private boolean isSchoolHttp(String u) {
        Uri p = Uri.parse(u);
        return "http".equalsIgnoreCase(p.getScheme()) && isSchoolHost(p.getHost());
    }

    /** School HTTP links are loaded in place after explicit consent; URLs are never rewritten. */
    private void loadTarget(String url) {
        if (isSchoolHttp(url)) {
            Uri p = Uri.parse(url);
            String host = p.getHost() == null ? "" : p.getHost().toLowerCase(Locale.US);
            if (cleartextApproved.contains(host)) {
                browserState.setText("该学校服务使用 HTTP 明文连接，请勿输入重要密码。");
                web.loadUrl(url);
                return;
            }
            browserState.setText("该学校服务只提供 HTTP 明文连接，等待确认…");
            promptCleartext(url);
            return;
        }
        if (allowed(url)) web.loadUrl(url);
        else blocked(url);
    }

    private void promptCleartext(final String url) {
        if (cleartextPromptShowing) return;
        cleartextPromptShowing = true;
        Uri p = Uri.parse(url);
        final String host = p.getHost() == null ? "该学校服务" : p.getHost();
        new AlertDialog.Builder(this)
                .setTitle("学校服务使用 HTTP")
                .setMessage(host + " 只提供 HTTP 明文连接，登录信息可能被同网段监听。\n\n"
                        + "是否继续访问？仅本次会话会对该域名放行。")
                .setNegativeButton("取消", (d, w) -> {
                    cleartextPromptShowing = false;
                    browserState.setText("已取消加载 HTTP 页面。");
                    if (hoursStage > 0) failHours("已取消加载学工系统，未读取学时。");
                })
                .setPositiveButton("继续访问", (d, w) -> {
                    cleartextPromptShowing = false;
                    cleartextApproved.add(host.toLowerCase(Locale.US));
                    browserState.setText("该页面使用 HTTP 明文连接，请勿输入重要密码。");
                    web.loadUrl(url);
                })
                .setOnCancelListener(d -> {
                    cleartextPromptShowing = false;
                    if (hoursStage > 0) failHours("已取消加载学工系统，未读取学时。");
                })
                .show();
    }

    private void showStudentAffairsNotice() {
        if (studentNoticeShown) return;
        studentNoticeShown = true;
        new AlertDialog.Builder(this)
                .setTitle("学工系统有移动端入口")
                .setMessage("学校停用了学工系统的 PC/浏览器入口，但应用可以用你的学校登录态"
                        + "打开学工移动端，读取学时或使用其它模块。\n\n"
                        + "推荐使用移动端入口；不再需要企业微信。")
                .setPositiveButton("打开学工", (d, w) -> openStudentAffairs())
                .setNeutralButton("去学工页", (d, w) -> showTab(TAB_STUDENT))
                .setNegativeButton("关闭", null)
                .show();
    }

    private void openStudentAffairs() {
        cancelSync();
        hoursStage = 0;
        hoursLoading = false;
        hoursRequestId++;
        if (prefs.getBoolean("studentCleartextOk", false)) {
            cleartextApproved.add("xgxt.gcp.edu.cn");
        }
        if (cleartextApproved.contains("xgxt.gcp.edu.cn")) {
            showStudentBrowser();
            loadStudentAffairs();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("学工移动端使用 HTTP")
                .setMessage("学校学工移动端只提供 HTTP 明文连接，登录信息可能被同网段监听。\n\n"
                        + "是否继续？确认后本机不再重复提示，可在设置里清除。")
                .setPositiveButton("继续访问", (d, w) -> {
                    cleartextApproved.add("xgxt.gcp.edu.cn");
                    prefs.edit().putBoolean("studentCleartextOk", true).apply();
                    showStudentBrowser();
                    loadStudentAffairs();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showStudentBrowser() {
        studentBrowserMode = true;
        tab = TAB_STUDENT;
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        homeScreen.setVisibility(View.GONE);
        weekScreen.setVisibility(View.GONE);
        studyScreen.setVisibility(View.GONE);
        settingsScreen.setVisibility(View.GONE);
        previewScreen.setVisibility(View.GONE);
        browserScreen.setVisibility(View.VISIBLE);
        setStudentMode(true);
        updateTabs();
    }

    private void setStudentMode(boolean student) {
        studentBrowserMode = student;
        if (studentBar != null) studentBar.setVisibility(student ? View.VISIBLE : View.GONE);
        if (browserControls != null) {
            browserControls.setVisibility(student ? View.GONE : View.VISIBLE);
        }
        if (browserSecondary != null) {
            browserSecondary.setVisibility(student ? View.GONE : View.VISIBLE);
        }
    }

    private void loadStudentAffairs() {
        if (!xgxtCleartextOk()) {
            promptXgxtCleartext(this::loadStudentAffairs);
            return;
        }
        browserState.setText("正在打开学工移动端…");
        web.loadUrl(CAS_MOBILE_URL);
    }

    private boolean xgxtCleartextOk() {
        return cleartextApproved.contains("xgxt.gcp.edu.cn")
                || prefs.getBoolean("studentCleartextOk", false);
    }

    private void promptXgxtCleartext(Runnable onOk) {
        if (xgxtCleartextOk()) {
            onOk.run();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("学工移动端使用 HTTP")
                .setMessage("学校学工移动端只提供 HTTP 明文连接，登录信息可能被同网段监听。\n\n"
                        + "是否继续？确认后本机不再重复提示，可在设置里清除。")
                .setPositiveButton("继续访问", (d, w) -> {
                    cleartextApproved.add("xgxt.gcp.edu.cn");
                    prefs.edit().putBoolean("studentCleartextOk", true).apply();
                    onOk.run();
                })
                .setNegativeButton("取消", (d, w) -> {
                    if (hoursLoading) failHours("已取消加载学工系统，未读取学时。");
                })
                .setOnCancelListener(d -> {
                    if (hoursLoading) failHours("已取消加载学工系统，未读取学时。");
                })
                .show();
    }

    private void blocked(String url) {
        Uri p = Uri.parse(url);
        String scheme = p.getScheme() == null ? "未知协议" : p.getScheme();
        String host = p.getHost() == null ? "" : p.getHost();
        String target = host.isEmpty() ? scheme : scheme + "://" + host;
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            browserState.setText("已拦截非网页链接（" + scheme + "），不会自动打开外部应用。");
        } else {
            browserState.setText("已拦截校外跳转：" + target + "。本应用只允许学校域名（*.gcp.edu.cn）。");
        }
        android.util.Log.w("GCT", "blocked " + target);
    }

    private void connectPortal() {
        studentNoticeShown = false;
        showTab(TAB_BROWSER);
        browserState.setText("请在官方页面登录；密码及验证码由你本人输入。教务页只用于浏览。");
        web.loadUrl(HOME_URL);
    }

    private WebViewClient client() {
        return new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
                if (!req.isForMainFrame()) {
                    String sub = req.getUrl().toString();
                    return !(allowed(sub) || isSchoolHttp(sub));
                }
                String target = req.getUrl().toString();
                if (isSchoolHttp(target)) {
                    Uri p = Uri.parse(target);
                    String host = p.getHost() == null ? "" : p.getHost().toLowerCase(Locale.US);
                    if (cleartextApproved.contains(host)) return false;
                    loadTarget(target);
                    return true;
                }
                if (allowed(target)) return false;
                blocked(target);
                return true;
            }

            @Override
            public void onPageStarted(WebView v, String url, android.graphics.Bitmap icon) {
                applyOrientationForPage(url);
                pageGeneration++;
                Uri u = Uri.parse(url);
                browserAddress.setText(u.getHost() == null ? "本机说明" : url);
                browserProgress.setVisibility(View.VISIBLE);
                if ("http".equalsIgnoreCase(u.getScheme())) {
                    if (isSchoolHost(u.getHost())) {
                        String host = u.getHost().toLowerCase(Locale.US);
                        if (!cleartextApproved.contains(host)) {
                            web.stopLoading();
                            loadTarget(url);
                            return;
                        }
                        browserState.setText("该页面使用 HTTP 明文连接，请勿输入重要密码。");
                    } else {
                        web.stopLoading();
                        loadTarget(url);
                        return;
                    }
                } else if (u.getHost() != null && !allowed(url)) {
                    web.stopLoading();
                    blocked(url);
                }
            }

            @Override
            public void onReceivedSslError(WebView v, android.webkit.SslErrorHandler h, SslError e) {
                h.cancel();
                browserState.setText("证书验证失败，已停止连接；不会忽略证书错误。");
            }

            @Override
            public void onReceivedError(WebView v, WebResourceRequest r, WebResourceError e) {
                if (r.isForMainFrame()) {
                    browserState.setText("页面无法访问，请检查校园网或网络连接。原课表仍保留。");
                }
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                applyOrientationForPage(url);
                browserProgress.setVisibility(View.GONE);
                Uri u = Uri.parse(url);
                String host = u.getHost();
                String path = u.getPath() == null ? "" : u.getPath();

                if (oneCardCapture && "zffw.gcp.edu.cn".equals(host)
                        && (path.startsWith("/cashier-mobile") || path.startsWith("/wechat"))) {
                    captureOneCardBalance();
                }
                if (prefillOneCard && "zffw.gcp.edu.cn".equals(host)
                        && "https".equalsIgnoreCase(u.getScheme())
                        && (path.startsWith("/cashier-mobile") || path.startsWith("/wechat"))) {
                    web.postDelayed(MainActivity.this::prefillOneCardCredentials, 1200);
                }
                if ("auth.gcp.edu.cn".equals(host) || "jwcjw.gcp.edu.cn".equals(host)) {
                    web.postDelayed(MainActivity.this::prefillSchoolCredentials, 1200);
                }

                if (syncStage > 0) {
                    if ("auth.gcp.edu.cn".equals(host) || path.contains("login")) {
                        failSync("需要先登录学校门户，自动同步未完成。");
                        return;
                    }
                    if ("jwcjw.gcp.edu.cn".equals(host)) {
                        if (syncStage == 1 && path.contains("xskbcxZccx")) {
                            syncStage = 2;
                            web.postDelayed(() -> runCalendarScript(0), 500);
                            return;
                        }
                        if (syncStage == 3 && path.contains("xskbcx_cxXskbcxIndex")) {
                            syncStage = 4;
                            web.postDelayed(() -> runAutoCourseScript(0), 1200);
                            return;
                        }
                    }
                    return;
                }

                if (hoursStage > 0) {
                    if ("auth.gcp.edu.cn".equals(host)) {
                        final int stage = hoursStage;
                        web.postDelayed(() -> {
                            if (hoursStage == stage && hoursStage > 0 && web.getUrl() != null
                                    && web.getUrl().contains("auth.gcp.edu.cn")) {
                                failHours("需要先登录学校门户。请到「教务」完成登录后再读取学时。");
                            }
                        }, 2500);
                        return;
                    }
                    if (isSchoolHttp(url) && "xgxt.gcp.edu.cn".equals(host)
                            && path.startsWith("/m")) {
                        if (hoursStage == 1) {
                            if (!xgxtCleartextOk()) {
                                failHours("需要先确认学工 HTTP 访问。");
                                return;
                            }
                            hoursStage = 2;
                            web.loadUrl(HOURS_PAGE_URL);
                            return;
                        }
                        if (hoursStage == 2) {
                            hoursStage = 3;
                            web.postDelayed(() -> runHoursScript(0), 1000);
                            return;
                        }
                    }
                }

                boolean schoolHttps = allowed(url);
                boolean schoolHttp = isSchoolHttp(url);
                if (!schoolHttps && !schoolHttp) return;
                CookieManager.getInstance().flush();
                if (hoursStage > 0) return;
                if ("xgxt.gcp.edu.cn".equals(host) && path.contains("disablepc")) {
                    browserState.setText("学工系统的 PC 入口被学校停用。可打开学工移动端，或到「学工」页读取。");
                    showStudentAffairsNotice();
                    return;
                }
                if (schoolHttp) {
                    browserState.setText("HTTP 明文页面：" + u.getHost() + " · 请勿输入重要密码");
                    return;
                }
                if ("auth.gcp.edu.cn".equals(host) || path.contains("login")) {
                    browserState.setText("等待你完成官方登录；不会读取或保存密码。");
                } else if ("jwcjw.gcp.edu.cn".equals(host)
                        && path.contains("xskbcx_cxXskbcxIndex")) {
                    browserState.setText("已进入个人课表页。选择学期并查询后，点「读取课表」；不点就不会读取。");
                } else if ("jwcjw.gcp.edu.cn".equals(host)) {
                    browserState.setText("已进入教务。可自行浏览，或用上方「进入课表」打开个人课表查询。");
                } else if ("home.gcp.edu.cn".equals(host)) {
                    browserState.setText("门户已打开。请自行点击学校入口；只有点「读取课表」才会读取。");
                }
            }
        };
    }

    private String extractionScript() throws Exception {
        try (InputStream input = getAssets().open("extract-course.js");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int n;
            while ((n = input.read(buffer)) != -1) output.write(buffer, 0, n);
            return output.toString("UTF-8");
        }
    }

    private boolean extracting;

    private void readCourses() {
        cancelSync();
        if (extracting) return;
        String url = web.getUrl();
        Uri u = url == null ? null : Uri.parse(url);
        if (u == null || !allowed(url) || !"jwcjw.gcp.edu.cn".equals(u.getHost())
                || u.getPath() == null || !u.getPath().contains("xskbcx_cxXskbcxIndex")) {
            browserState.setText("请先进入教务的个人课表查询页面，再点「读取课表」。");
            return;
        }
        final int g = pageGeneration;
        String script;
        try {
            script = extractionScript();
        } catch (Exception ex) {
            browserState.setText("课表适配资源无法读取，未修改本地课表。");
            return;
        }
        extracting = true;
        browserState.setText("正在读取当前页面的课表…");
        web.evaluateJavascript(script, result -> {
            extracting = false;
            if (g != pageGeneration) return;
            try {
                if (result == null || "null".equals(result)) {
                    browserState.setText("课表脚本未返回结果，请等待页面加载后再读取。");
                    return;
                }
                JSONObject obj = new JSONObject(result);
                if (obj.has("error")) {
                    String error = obj.getString("error");
                    if ("maintenance".equals(error)) {
                        browserState.setText("学校返回维护页面，本次同步停止；旧课表未覆盖。可回门户重新进入。");
                    } else if ("script".equals(error)) {
                        browserState.setText("读取脚本异常：" + obj.optString("kind") + "；旧课表未覆盖。");
                    } else {
                        browserState.setText("尚未找到完整课表（表格数 " + obj.optInt("tables")
                                + "）。请选择学期并查询后再读取。");
                    }
                    return;
                }

                final CourseData candidate = new CourseData();
                candidate.load(obj);
                candidate.term = obj.optString("term", "待确认");
                if (candidate.courses.isEmpty()) {
                    browserState.setText("没有可靠识别到排课，未覆盖本地课表；请检查页面学期和格式。");
                    showPendingDialog(candidate.pending);
                    return;
                }

                final String stamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
                        .format(new Date());
                final String rawText = buildRawText(candidate, obj, stamp);
                final String json = obj.toString();
                browserState.setText("已读取 " + candidate.courses.size() + " 条排课，请确认是否导入。");

                new AlertDialog.Builder(this)
                        .setTitle("确认导入 " + candidate.term)
                        .setMessage("识别到 " + candidate.courses.size() + " 条排课，"
                                + candidate.pending.size() + " 项待确认。\n\n"
                                + "待确认项目不会放入周课表。调课标记保留，不自动删除其它安排。"
                                + "导入后请核对星期、周次、节次和地点。\n\n"
                                + "确认后替换当前结构化课表；取消则保留旧数据。")
                        .setNegativeButton("取消", (d, w) ->
                                browserState.setText("已取消导入，原课表保留。"))
                        .setPositiveButton("确认导入", (d, w) ->
                                commitImport(candidate, json, rawText, stamp))
                        .show();
            } catch (Exception e) {
                browserState.setText("当前页面结构尚未适配，未覆盖原课表。");
            }
        });
    }

    private String buildRawText(CourseData candidate, JSONObject obj, String stamp) throws JSONException {
        StringBuilder raw = new StringBuilder("读取时间：" + stamp + "\n学期：" + candidate.term + "\n");
        JSONArray cells = obj.getJSONArray("cells");
        for (int i = 0; i < cells.length(); i++) {
            JSONObject cell = cells.getJSONObject(i);
            raw.append("\n星期").append(cell.optInt("day")).append("\n")
                    .append(cell.optString("text")).append("\n");
        }
        for (String item : candidate.pending) raw.append("\n待确认：\n").append(item).append("\n");
        return raw.toString();
    }

    private void commitImport(CourseData candidate, String json, String rawText, String stamp) {
        data = candidate;
        data.rawText = rawText;
        data.lastSync = stamp;
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("courseJson", json);
        editor.putString("courseText", rawText);
        editor.putString("lastSync", stamp);
        if (autoWeek && termStart != null) {
            int current = currentWeek();
            if (current >= 1 && current <= MAX_WEEK) {
                viewWeek = current;
                editor.putInt("selectedWeek", current);
            }
        }
        editor.apply();
        refreshVisible();
        String message = termStart == null
                ? "已导入 " + data.courses.size() + " 条排课；可在首页设置开学日期"
                : "已导入 " + data.courses.size() + " 条排课";
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void confirmClear() {
        new AlertDialog.Builder(this)
                .setTitle("清除本应用数据？")
                .setMessage("清除课表、网页会话、Cookie 和本地设置；不会修改学校账号，也不会退出其他应用。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清除", (d, w) -> clearAll())
                .show();
    }

    private void clearAll() {
        pageGeneration++;
        extracting = false;
        cleartextApproved.clear();
        cleartextPromptShowing = false;
        studentNoticeShown = false;
        hoursRequestId++;
        hoursStage = 0;
        hoursLoading = false;
        hoursError = null;
        hoursData = null;
        syncStage = 0;
        syncRequestId++;
        web.stopLoading();
        web.loadUrl("about:blank");
        prefs.edit().clear().commit();
        oneCardBalance = "";
        oneCardUser = "";
        oneCardCapture = false;
        oneCardReturnToLife = false;
        oneCardUser = "";
        prefillOneCard = false;
        try {
            KeyStore ks = KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            ks.deleteEntry(ONE_CARD_KEY_ALIAS);
            ks.deleteEntry(SCHOOL_KEY_ALIAS);
        } catch (Exception ignored) {
        }
        qrFile().delete();
        WebStorage.getInstance().deleteAllData();
        web.clearCache(true);
        web.clearHistory();
        web.clearFormData();
        data.clear();
        termStart = null;
        autoWeek = true;
        showTeacher = true;
        viewWeek = 1;
        CookieManager.getInstance().removeAllCookies(done -> {
            CookieManager.getInstance().flush();
            web.loadUrl(HOME_URL);
            browserState.setText("本应用的课表、网页缓存和 Cookie 已清除。");
        });
        showTab(TAB_HOME);
        Toast.makeText(this, "本应用数据已清除", Toast.LENGTH_SHORT).show();
    }

    // ---------------------------------------------------------------- lifecycle

    @Override
    protected void onResume() {
        super.onResume();
        LocalDate today = LocalDate.now();
        if (lastToday != null && !lastToday.equals(today)) {
            lastToday = today;
            if (autoWeek && termStart != null) {
                int current = currentWeek();
                if (current >= 1 && current <= MAX_WEEK) {
                    viewWeek = current;
                    prefs.edit().putInt("selectedWeek", current).apply();
                }
            }
            refreshVisible();
            maybeAutoSync();
        }
    }

    @Override
    public void onBackPressed() {
        if (studentBrowserMode) {
            showTab(TAB_STUDENT);
        } else if (previewScreen.getVisibility() == View.VISIBLE) {
            showTab(returnTab);
        } else if (tab == TAB_BROWSER && web != null && web.canGoBack()) {
            web.goBack();
        } else if (tab != TAB_HOME) {
            showTab(TAB_HOME);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
        out.putInt("viewWeek", viewWeek);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }

    /** Wraps short content but never exceeds a maximum height, so dialogs stay on screen. */
    private static final class MaxHeightScrollView extends ScrollView {
        private final int maxHeight;

        MaxHeightScrollView(Context context, int maxHeight) {
            super(context);
            this.maxHeight = Math.max(1, maxHeight);
            setVerticalScrollBarEnabled(false);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec,
                    MeasureSpec.makeMeasureSpec(maxHeight, MeasureSpec.AT_MOST));
        }
    }
}
