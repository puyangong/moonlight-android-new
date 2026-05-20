package com.limelight.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;

import com.limelight.R;

public class FloatingWindow1 extends LinearLayout {
    public enum Mode {
        NONE,
        SCROLL,      // Button2 mode: move/scale screen
        MOUSE_TOUCH  // Button3 mode: normal mouse/touch
    }

    private Mode currentMode = Mode.MOUSE_TOUCH;
    private OnModeChangeListener modeChangeListener;
    private OnDragListener dragListener;
    private OnKeyboardToggleListener keyboardToggleListener;

    private ImageButton btnDrag;
    private ImageButton btnScrollMode;
    private ImageButton btnMouseMode;
    private ImageButton btnKeyboard;

    private float dragStartX, dragStartY;
    private float windowStartX, windowStartY;
    private boolean isDragging = false;
    private boolean keyboardActive = false;

    private static final int ACTIVE_COLOR = Color.parseColor("#ADD8E6"); // light blue
    private static final int INACTIVE_COLOR = Color.TRANSPARENT;
    private static final int BTN_SIZE_DP = 36;
    private static final int BTN_MARGIN_DP = 2;

    public FloatingWindow1(Context context) {
        super(context);
        init(context);
    }

    public FloatingWindow1(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public FloatingWindow1(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(LinearLayout.VERTICAL);
        setGravity(Gravity.CENTER);

        // Semi-transparent background
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.parseColor("#AA000000"));
        background.setCornerRadius(dpToPx(context, 8));
        setBackground(background);

        int btnSize = dpToPx(context, BTN_SIZE_DP);
        int btnMargin = dpToPx(context, BTN_MARGIN_DP);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(btnSize, btnSize);
        btnParams.setMargins(btnMargin, btnMargin, btnMargin, btnMargin);

        // Button 1: Drag handle
        btnDrag = createImageButton(context, R.drawable.ic_btn1, btnParams);
        btnDrag.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return handleDragTouch(event);
            }
        });
        addView(btnDrag);

        // Button 2: Scroll/Move screen mode
        btnScrollMode = createImageButton(context, R.drawable.ic_btn4, btnParams);
        btnScrollMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(Mode.SCROLL);
            }
        });
        addView(btnScrollMode);

        // Button 3: Mouse/Touch mode
        btnMouseMode = createImageButton(context, R.drawable.ic_btn3, btnParams);
        btnMouseMode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setMode(Mode.MOUSE_TOUCH);
            }
        });
        addView(btnMouseMode);

        // Button 4: Keyboard / FloatingWindow2 toggle
        btnKeyboard = createImageButton(context, R.drawable.ic_btn2, btnParams);
        btnKeyboard.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setKeyboardActive(!keyboardActive);
            }
        });
        addView(btnKeyboard);

        updateButtonColors();
    }

    private ImageButton createImageButton(Context context, int drawableRes, LinearLayout.LayoutParams params) {
        ImageButton btn = new ImageButton(context);
        btn.setImageResource(drawableRes);
        btn.setScaleType(android.widget.ImageView.ScaleType.FIT_CENTER);
        btn.setLayoutParams(params);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(INACTIVE_COLOR);
        bg.setCornerRadius(dpToPx(context, 4));
        btn.setBackground(bg);

        btn.setPadding(4, 4, 4, 4);
        return btn;
    }

    private boolean handleDragTouch(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragStartX = event.getRawX();
                dragStartY = event.getRawY();
                windowStartX = getX();
                windowStartY = getY();
                isDragging = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                float deltaX = event.getRawX() - dragStartX;
                float deltaY = event.getRawY() - dragStartY;

                if (!isDragging && (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10)) {
                    isDragging = true;
                }

                if (isDragging && dragListener != null) {
                    dragListener.onDrag(windowStartX + deltaX, windowStartY + deltaY);
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging && dragListener != null) {
                    dragListener.onDragEnd();
                }
                isDragging = false;
                return true;
        }
        return false;
    }

    public void setMode(Mode mode) {
        if (currentMode == mode) {
            // Do nothing if clicking the same mode (keep it selected)
            return;
        }
        currentMode = mode;
        updateButtonColors();

        if (modeChangeListener != null) {
            modeChangeListener.onModeChanged(currentMode);
        }
    }

    public Mode getMode() {
        return currentMode;
    }
private void updateButtonColors() {
        setButtonBackground(btnScrollMode, currentMode == Mode.SCROLL);
        setButtonBackground(btnMouseMode, currentMode == Mode.MOUSE_TOUCH);
        setButtonBackground(btnKeyboard, keyboardActive);
    }

    public void setKeyboardActive(boolean active) {
        keyboardActive = active;
        updateButtonColors();
        if (keyboardToggleListener != null) {
            keyboardToggleListener.onKeyboardToggled(active);
        }
    }

    public boolean isKeyboardActive() {
        return keyboardActive;
    }

    /** Restore keyboard button visual state only, without triggering the toggle listener */
    public void restoreKeyboardButtonState(boolean active) {
        keyboardActive = active;
        updateButtonColors();
    }

    public void setOnKeyboardToggleListener(OnKeyboardToggleListener listener) {
        this.keyboardToggleListener = listener;
    }

    public interface OnKeyboardToggleListener {
        void onKeyboardToggled(boolean active);
    }

    private void setButtonBackground(ImageButton btn, boolean active) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(active ? ACTIVE_COLOR : INACTIVE_COLOR);
        bg.setCornerRadius(dpToPx(getContext(), 4));
        btn.setBackground(bg);
    }

    public void setOnModeChangeListener(OnModeChangeListener listener) {
        this.modeChangeListener = listener;
    }

    public void setOnDragListener(OnDragListener listener) {
        this.dragListener = listener;
    }

    public interface OnModeChangeListener {
        void onModeChanged(Mode mode);
    }

    public interface OnDragListener {
        void onDrag(float x, float y);
        void onDragEnd();
    }

    private int dpToPx(Context context, int dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (dp * metrics.density + 0.5f);
    }
}