package com.limelight.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FloatingWindow2 extends LinearLayout {

    private static final int ACTIVE_COLOR = Color.parseColor("#ADD8E6");
    private static final int INACTIVE_COLOR = Color.parseColor("#26FFFFFF");
    private static final int HOLD_MODE_COLOR = Color.parseColor("#88ADD8E6");
    private static final int BTN_TEXT_COLOR = Color.WHITE;
    private static final int BTN_SIZE_DP = 36;
    private static final int BTN_MARGIN_DP = 2;

    private boolean holdModeActive = false;
    private boolean isReleasing = false;
    private Set<Integer> heldKeys = new HashSet<>();
    private Map<View, Integer> buttonToKeyMap = new HashMap<>();
    private Map<View, ButtonState> buttonStates = new HashMap<>();

    private OnKeyEventListener keyEventListener;
    private OnHoldModeChangeListener holdModeChangeListener;
    private Button holdBtn;
    private OnKeyboardToggleListener keyboardToggleListener;

    private enum ButtonState {
        NORMAL,
        ACTIVE,
        HELD
    }

    public FloatingWindow2(Context context) {
        super(context);
        init(context);
    }

    public FloatingWindow2(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FloatingWindow2(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER_HORIZONTAL);

        // 15% opacity background
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#26000000"));
        setBackground(background);

        int btnHeight = dpToPx(context, BTN_SIZE_DP);
        int btnMargin = dpToPx(context, BTN_MARGIN_DP);

        // Row 1: F1-F12 + Up arrow
        HorizontalScrollView scroll1 = new HorizontalScrollView(context);
        scroll1.setHorizontalScrollBarEnabled(false);
        LinearLayout row1 = new LinearLayout(context);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        row1.setGravity(Gravity.CENTER_VERTICAL);

        addKeyButton(context, row1, "F1", btnHeight, btnMargin, 131); // KEYCODE_F1
        addKeyButton(context, row1, "F2", btnHeight, btnMargin, 132);
        addKeyButton(context, row1, "F3", btnHeight, btnMargin, 133);
        addKeyButton(context, row1, "F4", btnHeight, btnMargin, 134);
        addKeyButton(context, row1, "F5", btnHeight, btnMargin, 135);
        addKeyButton(context, row1, "F6", btnHeight, btnMargin, 136);
        addKeyButton(context, row1, "F7", btnHeight, btnMargin, 137);
        addKeyButton(context, row1, "F8", btnHeight, btnMargin, 138);
        addKeyButton(context, row1, "F9", btnHeight, btnMargin, 139);
        addKeyButton(context, row1, "F10", btnHeight, btnMargin, 140);
        addKeyButton(context, row1, "F11", btnHeight, btnMargin, 141);
        addKeyButton(context, row1, "F12", btnHeight, btnMargin, 142);

        scroll1.addView(row1);
        addView(scroll1);

        // Row 2: hold, esc, tab, ctrl, alt, shift, enter, /, \, win, arrows
        HorizontalScrollView scroll2 = new HorizontalScrollView(context);
        scroll2.setHorizontalScrollBarEnabled(false);
        LinearLayout row2 = new LinearLayout(context);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        row2.setGravity(Gravity.CENTER_VERTICAL);

        // Hold mode toggle button (moved to the leftmost position)
        holdBtn = new Button(context);
        holdBtn.setText("长按");
        holdBtn.setTextColor(BTN_TEXT_COLOR);
        holdBtn.setTextSize(10);
        LinearLayout.LayoutParams holdParams = new LinearLayout.LayoutParams(
                dpToPx(context, 44), btnHeight);
        holdParams.setMargins(btnMargin, btnMargin, btnMargin, btnMargin);
        holdBtn.setLayoutParams(holdParams);
        updateButtonVisual(holdBtn, ButtonState.NORMAL);
        holdBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setHoldModeActive(!holdModeActive);
            }
        });
        row2.addView(holdBtn);

        addKeyButton(context, row2, "Esc", btnHeight, btnMargin, 111); // KEYCODE_ESCAPE
        addKeyButton(context, row2, "Tab", btnHeight, btnMargin, 61);  // KEYCODE_TAB
        addKeyButton(context, row2, "Ctrl", btnHeight, btnMargin, 113); // KEYCODE_CTRL_LEFT
        addKeyButton(context, row2, "Alt", btnHeight, btnMargin, 57);  // KEYCODE_ALT_LEFT
        addKeyButton(context, row2, "Shift", btnHeight, btnMargin, 59); // KEYCODE_SHIFT_LEFT
        addKeyButton(context, row2, "Enter", btnHeight, btnMargin, 66); // KEYCODE_ENTER
        addKeyButton(context, row2, "Win", btnHeight, btnMargin, 117); // KEYCODE_META_LEFT
        scroll2.addView(row2);
        addView(scroll2);

        // Row 3: keyboard, arrows, / and \
        HorizontalScrollView scroll3 = new HorizontalScrollView(context);
        scroll3.setHorizontalScrollBarEnabled(false);
        LinearLayout row3 = new LinearLayout(context);
        row3.setOrientation(LinearLayout.HORIZONTAL);
        row3.setGravity(Gravity.CENTER_VERTICAL);

        Button keyboardBtn = new Button(context);
        keyboardBtn.setText("软键盘");
        keyboardBtn.setTextColor(BTN_TEXT_COLOR);
        keyboardBtn.setTextSize(10);
        keyboardBtn.setAllCaps(false);
        keyboardBtn.setTransformationMethod(null);
        LinearLayout.LayoutParams kbParams = new LinearLayout.LayoutParams(
                dpToPx(context, 72), btnHeight);
        kbParams.setMargins(btnMargin, btnMargin, btnMargin, btnMargin);
        keyboardBtn.setLayoutParams(kbParams);
        updateButtonVisual(keyboardBtn, ButtonState.NORMAL);
        keyboardBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (keyboardToggleListener != null) {
                    keyboardToggleListener.onKeyboardToggled(true);
                }
            }
        });
        row3.addView(keyboardBtn);

        // Arrow keys and / \ moved to row 3
        addKeyButton(context, row3, "\u2190", btnHeight, btnMargin, 21); // DPAD_LEFT
        addKeyButton(context, row3, "\u2192", btnHeight, btnMargin, 22); // DPAD_RIGHT
        addKeyButton(context, row3, "\u2191", btnHeight, btnMargin, 19); // DPAD_UP
        addKeyButton(context, row3, "\u2193", btnHeight, btnMargin, 20); // DPAD_DOWN
        addKeyButton(context, row3, "/", btnHeight, btnMargin, 76);    // KEYCODE_SLASH
        addKeyButton(context, row3, "\\", btnHeight, btnMargin, 73);   // KEYCODE_BACKSLASH

        scroll3.addView(row3);
        addView(scroll3);
    }

    private void addKeyButton(Context context, LinearLayout parent, String label,
                              int height, int margin, int keyCode) {
        Button btn = new Button(context);
        btn.setText(label);
        btn.setTextColor(BTN_TEXT_COLOR);
        btn.setTextSize(10);
        btn.setAllCaps(false);
        btn.setTransformationMethod(null);
        int width = label.length() > 4 ? dpToPx(context, 58) : (label.length() > 2 ? dpToPx(context, 50) : dpToPx(context, 36));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(width, height);
        params.setMargins(margin, margin, margin, margin);
        btn.setLayoutParams(params);
        updateButtonVisual(btn, ButtonState.NORMAL);

        buttonToKeyMap.put(btn, keyCode);
        buttonStates.put(btn, ButtonState.NORMAL);

        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onKeyButtonClick(btn, keyCode);
            }
        });

        parent.addView(btn);
    }

    private void onKeyButtonClick(Button btn, int keyCode) {
        ButtonState state = buttonStates.get(btn);

        if (holdModeActive) {
            if (state == ButtonState.HELD) {
                // Release hold
                buttonStates.put(btn, ButtonState.NORMAL);
                updateButtonVisual(btn, ButtonState.NORMAL);
                heldKeys.remove(keyCode);
                if (keyEventListener != null) {
                    keyEventListener.onKeyEvent(keyCode, false);
                }
            } else {
                // Start hold
                buttonStates.put(btn, ButtonState.HELD);
                updateButtonVisual(btn, ButtonState.HELD);
                heldKeys.add(keyCode);
                if (keyEventListener != null) {
                    keyEventListener.onKeyEvent(keyCode, true);
                }
            }
        } else {
            // Normal click: press and release
            if (keyEventListener != null) {
                keyEventListener.onKeyEvent(keyCode, true);
                keyEventListener.onKeyEvent(keyCode, false);
            }
            // Brief visual feedback
            buttonStates.put(btn, ButtonState.ACTIVE);
            updateButtonVisual(btn, ButtonState.ACTIVE);
            btn.postDelayed(new Runnable() {
                @Override
                public void run() {
                    buttonStates.put(btn, ButtonState.NORMAL);
                    updateButtonVisual(btn, ButtonState.NORMAL);
                }
            }, 150);
        }
    }

    private void updateButtonVisual(Button btn, ButtonState state) {
        GradientDrawable bg = new GradientDrawable();
        switch (state) {
            case ACTIVE:
                bg.setColor(ACTIVE_COLOR);
                break;
            case HELD:
                bg.setColor(HOLD_MODE_COLOR);
                break;
            case NORMAL:
            default:
                bg.setColor(INACTIVE_COLOR);
                break;
        }
        bg.setCornerRadius(dpToPx(getContext(), 4));
        btn.setBackground(bg);
    }

    public void setHoldModeActive(boolean active) {
        holdModeActive = active;
        if (holdBtn != null) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(active ? ACTIVE_COLOR : INACTIVE_COLOR);
            bg.setCornerRadius(dpToPx(getContext(), 4));
            holdBtn.setBackground(bg);
        }
        if (holdModeChangeListener != null) {
            holdModeChangeListener.onHoldModeChanged(active);
        }
    }

    public boolean isHoldModeActive() {
        return holdModeActive;
    }

    public void releaseAllHeldKeys() {
        if (isReleasing) return;
        isReleasing = true;
        try {
            for (Map.Entry<View, ButtonState> entry : new HashMap<>(buttonStates).entrySet()) {
                if (entry.getValue() == ButtonState.HELD) {
                    Button btn = (Button) entry.getKey();
                    Integer keyCode = buttonToKeyMap.get(btn);
                    if (keyCode != null) {
                        heldKeys.remove(keyCode);
                        if (keyEventListener != null) {
                            keyEventListener.onKeyEvent(keyCode, false);
                        }
                    }
                    buttonStates.put(btn, ButtonState.NORMAL);
                    updateButtonVisual(btn, ButtonState.NORMAL);
                }
            }
        } finally {
            isReleasing = false;
        }
    }

    public void setOnKeyEventListener(OnKeyEventListener listener) {
        this.keyEventListener = listener;
    }

    public void setOnHoldModeChangeListener(OnHoldModeChangeListener listener) {
        this.holdModeChangeListener = listener;
    }

    public void setOnKeyboardToggleListener(OnKeyboardToggleListener listener) {
        this.keyboardToggleListener = listener;
    }

    public interface OnKeyEventListener {
        void onKeyEvent(int keyCode, boolean down);
    }

    public interface OnHoldModeChangeListener {
        void onHoldModeChanged(boolean active);
    }

    public interface OnKeyboardToggleListener {
        void onKeyboardToggled(boolean active);
    }

    private int dpToPx(Context context, int dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (dp * metrics.density + 0.5f);
    }
}
