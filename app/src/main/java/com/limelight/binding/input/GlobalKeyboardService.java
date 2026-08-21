package com.limelight.binding.input;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

import com.limelight.Game;
import com.limelight.LimeLog;

/**
 * 全局键盘无障碍服务 — 拦截外接键盘的所有按键和组合键，
 * 使其仅在 Moonlight 串流界面内生效，防止穿透到系统。
 */
public class GlobalKeyboardService extends AccessibilityService {

    // 用于判断无障碍服务是否已开启的偏好设置 key
    public static final String PREF_KEY_GLOBAL_KEYBOARD_CAPTURE = "checkbox_global_keyboard_capture";

    // 当前正在前台运行的 Game Activity 实例
    private static Game sActiveGameActivity;

    /**
     * 注册当前活动的 Game Activity。
     * 在 Game.onResume() 中调用。
     */
    public static void registerGameActivity(Game gameActivity) {
        sActiveGameActivity = gameActivity;
        LimeLog.info("GlobalKeyboardService: Game activity registered");
    }

    /**
     * 取消注册 Game Activity。
     * 在 Game.onPause() 中调用。
     */
    public static void unregisterGameActivity(Game gameActivity) {
        if (sActiveGameActivity == gameActivity) {
            sActiveGameActivity = null;
            LimeLog.info("GlobalKeyboardService: Game activity unregistered");
        }
    }

    /**
     * 检查无障碍服务是否已在系统设置中开启。
     */
    public static boolean isServiceEnabled(Context context) {
        String serviceName = context.getPackageName() + "/" + GlobalKeyboardService.class.getName();
        try {
            int enabled = Settings.Secure.getInt(
                    context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED);
            if (enabled != 1) {
                return false;
            }
            String enabledServices = Settings.Secure.getString(
                    context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabledServices != null) {
                return enabledServices.contains(serviceName) ||
                       enabledServices.contains(GlobalKeyboardService.class.getName());
            }
        } catch (Settings.SettingNotFoundException e) {
            // Accessibility settings not found
        }
        return false;
    }

    /**
     * 检查全局键盘捕获功能是否可以使用。
     * 需要同时满足：偏好设置已开启 AND 无障碍服务已开启。
     */
    public static boolean isKeyboardCaptureAvailable(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean prefEnabled = prefs.getBoolean(PREF_KEY_GLOBAL_KEYBOARD_CAPTURE, false);
        boolean serviceEnabled = isServiceEnabled(context);
        return prefEnabled && serviceEnabled;
    }

    /**
     * 检测应用是否从正规应用商店安装（非侧载）。
     * Android 13+ 侧载应用需要先开启"受限设置"才能启用无障碍服务。
     */
    public static boolean isAppFromStore(Context context) {
        try {
            String installer = context.getPackageManager()
                    .getInstallerPackageName(context.getPackageName());
            // 来自 Google Play、Amazon、Samsung 商店或其他已知商店
            if (installer != null) {
                return installer.equals("com.android.vending")      // Google Play
                    || installer.equals("com.amazon.venezia")      // Amazon
                    || installer.equals("com.sec.android.app.samsungapps") // Samsung
                    || installer.equals("com.xiaomi.market")       // Xiaomi
                    || installer.equals("com.oppo.market")         // OPPO
                    || installer.equals("com.bbk.appstore")        // Vivo
                    || installer.equals("com.huawei.appmarket")    // Huawei
                    || installer.equals("com.heytap.market");      // Realme/OnePlus
            }
            // installer 为 null 说明是侧载或系统应用
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 判断是否需要解除"受限设置"限制。
     * Android 13+ 且侧载应用需要此步骤。
     */
    public static boolean needsRestrictedSettingsPermission(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return false; // Android 12 及以下不需要
        }
        // 系统应用不需要
        try {
            ApplicationInfo appInfo = context.getApplicationInfo();
            if ((appInfo.flags & (ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)) != 0) {
                return false;
            }
        } catch (Exception e) {
            // ignore
        }
        return !isAppFromStore(context);
    }

    /**
     * 打开应用详情设置页面，引导用户开启"受限设置"权限。
     * Android 13+ 侧载应用需先在此页面右上角菜单 → "允许受限设置"。
     */
    public static void openAppSettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    /**
     * 打开无障碍服务设置页面，引导用户开启服务。
     * Android 13+ 侧载应用需先调用 openAppSettings() 解除受限设置。
     */
    public static void openAccessibilitySettings(Context context) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        LimeLog.info("GlobalKeyboardService: Service connected");

        // 配置无障碍服务信息 — 仅监听键盘事件
        // 不使用 FLAG_RETRIEVE_INTERACTIVE_WINDOWS，避免 Android 14+ 额外限制
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPES_ALL_MASK;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        info.flags = AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
                   | AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        setServiceInfo(info);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 不需要处理具体的无障碍事件，我们只关注键盘事件
    }

    @Override
    public void onInterrupt() {
        LimeLog.info("GlobalKeyboardService: Service interrupted");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        // 检查是否有活动的 Game Activity
        Game gameActivity = sActiveGameActivity;
        if (gameActivity == null) {
            // 没有活动的串流界面，不拦截按键
            return false;
        }

        // 验证 Game Activity 是否仍然在前台
        if (gameActivity.isFinishing() || gameActivity.isDestroyed()) {
            unregisterGameActivity(gameActivity);
            return false;
        }

        // 检查偏好设置是否启用
        if (!isKeyboardCaptureAvailable(this)) {
            return false;
        }

        // 忽略虚拟导航键（系统产生的）
        if ((event.getFlags() & KeyEvent.FLAG_VIRTUAL_HARD_KEY) != 0) {
            return false;
        }

        // 将按键事件转发给 Game Activity 处理
        boolean handled;
        switch (event.getAction()) {
            case KeyEvent.ACTION_DOWN:
                handled = gameActivity.handleKeyDown(event);
                break;
            case KeyEvent.ACTION_UP:
                handled = gameActivity.handleKeyUp(event);
                break;
            case KeyEvent.ACTION_MULTIPLE:
                handled = gameActivity.handleKeyMultiple(event);
                break;
            default:
                return false;
        }

        // 如果 Game Activity 处理了该按键，消费事件
        if (handled) {
            return true;
        }

        // 安全网：即使 Game 未处理，也强制消费系统级按键
        // 防止 Win+Tab、Alt+Tab、Win+D 等系统快捷键泄漏到 Android
        if (isSystemModifierKey(event.getKeyCode())) {
            LimeLog.info("GlobalKeyboardService: Blocking system key: " + event.getKeyCode());
            return true;
        }

        return false;
    }

    /**
     * 判断是否为系统级按键，应始终被拦截防止泄漏到 Android 系统。
     */
    private static boolean isSystemModifierKey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_META_LEFT:
            case KeyEvent.KEYCODE_META_RIGHT:
            case KeyEvent.KEYCODE_ALT_LEFT:
            case KeyEvent.KEYCODE_ALT_RIGHT:
            case KeyEvent.KEYCODE_CTRL_LEFT:
            case KeyEvent.KEYCODE_CTRL_RIGHT:
            case KeyEvent.KEYCODE_SHIFT_LEFT:
            case KeyEvent.KEYCODE_SHIFT_RIGHT:
            case KeyEvent.KEYCODE_FUNCTION:
            case KeyEvent.KEYCODE_CAPS_LOCK:
            case KeyEvent.KEYCODE_NUM_LOCK:
            case KeyEvent.KEYCODE_SCROLL_LOCK:
            case KeyEvent.KEYCODE_SYM:
            case KeyEvent.KEYCODE_HOME:
            case KeyEvent.KEYCODE_APP_SWITCH:
            case KeyEvent.KEYCODE_SEARCH:
            case KeyEvent.KEYCODE_ASSIST:
            case KeyEvent.KEYCODE_VOICE_ASSIST:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sActiveGameActivity = null;
        LimeLog.info("GlobalKeyboardService: Service destroyed");
    }
}
