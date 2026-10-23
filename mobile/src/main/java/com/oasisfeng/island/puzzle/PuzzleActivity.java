package com.oasisfeng.island.puzzle;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;


/** Small, fully local 2048 launcher. */
public final class PuzzleActivity {
    private Activity host;
    private Runnable onUnlock;
    private final PuzzleEngine engine = new PuzzleEngine();
    private SharedPreferences preferences;
    private TextView score;
    private BoardView board;
    private long best;
    private int secretStep;
    private long secretTime;
    private static final int[] SECRET = {0, 3, 15, 12};
    private static final int INK = Color.rgb(100, 91, 81);

    public static void attach(Activity host, Runnable onUnlock) {
        ViewGroup content = (ViewGroup) host.findViewById(android.R.id.content);
        if (content.findViewWithTag("puzzle_gate") == null) new PuzzleActivity().initialize(host, onUnlock);
    }
    private void initialize(Activity activity, Runnable unlock) {
        host = activity;
        ViewGroup content = (ViewGroup) host.findViewById(android.R.id.content);
        List<View> previous = new ArrayList<>();
        for (int i = 0; i < content.getChildCount(); i++) {
            View child = content.getChildAt(i);
            if (child.getVisibility() == View.VISIBLE) { previous.add(child); child.setVisibility(View.GONE); }
        }
        onUnlock = () -> {
            View gate = content.findViewWithTag("puzzle_gate");
            if (gate != null) content.removeView(gate);
            for (View child : previous) child.setVisibility(View.VISIBLE);
            unlock.run();
        };
        preferences = host.createDeviceProtectedStorageContext().getSharedPreferences("puzzle_2048", Context.MODE_PRIVATE);
        best = preferences.getLong("best", 0);
        int[] saved = new int[16];
        for (int i = 0; i < 16; i++) saved[i] = preferences.getInt("tile_" + i, 0);
        if (!engine.restore(saved, preferences.getLong("score", 0))) engine.reset();
        LinearLayout root = new LinearLayout(host);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(20), dp(12), dp(20), dp(12));
        root.setBackgroundColor(Color.rgb(250, 248, 239));
        TextView title = text("2048", 48);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setOnLongClickListener(v -> {
            if (secretStep == 4 && SystemClock.elapsedRealtime() - secretTime < 10000) {
                secretStep = 0;
                save();
                onUnlock.run();
            } else secretStep = 0;
            return true;
        });
        root.addView(title);
        score = text("", 17);
        root.addView(score);
        TextView hint = text("Join the numbers and reach 2048!", 16);
        hint.setPadding(0, dp(12), 0, dp(8));
        root.addView(hint);
        board = new BoardView();
        root.addView(board, new LinearLayout.LayoutParams(-1, 0, 1));
        LinearLayout controls = new LinearLayout(host);
        controls.setGravity(Gravity.CENTER);
        addDirection(controls, "←", "Move left", PuzzleEngine.LEFT);
        addDirection(controls, "↑", "Move up", PuzzleEngine.UP);
        addDirection(controls, "↓", "Move down", PuzzleEngine.DOWN);
        addDirection(controls, "→", "Move right", PuzzleEngine.RIGHT);
        root.addView(controls);
        Button reset = new Button(host);
        reset.setText("New game");
        reset.setOnClickListener(v -> {
            secretStep = 0;
            new AlertDialog.Builder(host).setMessage("Start a new game?")
                .setNegativeButton("Cancel", null).setPositiveButton("New game", (d, w) -> {
                    engine.reset(); secretStep = 0; refresh(); save();
                }).show();
        });
        root.addView(reset);
        root.setTag("puzzle_gate");
        root.setClickable(true);
        root.setOnClickListener(v -> secretStep = 0);
        content.addView(root, new ViewGroup.LayoutParams(-1, -1));
        refresh();
        save();
    }
    private TextView text(String value, int size) {
        TextView view = new TextView(host);
        view.setText(value); view.setTextSize(size); view.setTextColor(INK); view.setGravity(Gravity.CENTER);
        return view;
    }
    private void addDirection(LinearLayout parent, String symbol, String description, int direction) {
        Button button = new Button(host);
        button.setText(symbol); button.setTextSize(24); button.setContentDescription(description);
        button.setOnClickListener(v -> move(direction));
        parent.addView(button, new LinearLayout.LayoutParams(0, dp(52), 1));
    }
    private void move(int direction) {
        secretStep = 0;
        if (engine.move(direction)) { refresh(); save(); }
    }
    private void refresh() {
        best = Math.max(best, engine.score());
        score.setText("Score  " + engine.score() + "     Best  " + best
                + (engine.canMove() ? "" : "\nGame over — try a new game"));
        board.invalidate();
        StringBuilder description = new StringBuilder("2048 board. ");
        for (int i = 0; i < 16; i++) description.append(engine.tile(i)).append(i % 4 == 3 ? ". " : ", ");
        board.setContentDescription(description.toString());
    }
    private void save() {
        SharedPreferences.Editor editor = preferences.edit().putLong("score", engine.score()).putLong("best", best);
        for (int i = 0; i < 16; i++) editor.putInt("tile_" + i, engine.tile(i));
        editor.apply();
    }
    private int dp(int value) { return Math.round(value * host.getResources().getDisplayMetrics().density); }
    private void tap(int index) {
        long now = SystemClock.elapsedRealtime();
        if (now - secretTime > 10000) secretStep = 0;
        if (secretStep < 4 && index == SECRET[secretStep]) secretStep++;
        else secretStep = index == SECRET[0] ? 1 : 0;
        secretTime = now;
    }
    private final class BoardView extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF rect = new RectF();
        private float left, top, size, downX, downY;
        BoardView() { super(host); setFocusable(true); }
        @Override public void onWindowFocusChanged(boolean hasWindowFocus) {
            super.onWindowFocusChanged(hasWindowFocus);
            if (!hasWindowFocus) secretStep = 0;
        }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            size = Math.min(getWidth(), getHeight());
            left = (getWidth() - size) / 2f; top = (getHeight() - size) / 2f;
            paint.setColor(Color.rgb(187, 173, 160));
            rect.set(left, top, left + size, top + size);
            canvas.drawRoundRect(rect, dp(7), dp(7), paint);
            float gap = size * .025f, tile = (size - gap * 5) / 4;
            for (int i = 0; i < 16; i++) {
                int value = engine.tile(i);
                float x = left + gap + (i % 4) * (tile + gap), y = top + gap + (i / 4) * (tile + gap);
                paint.setColor(tileColor(value)); rect.set(x, y, x + tile, y + tile);
                canvas.drawRoundRect(rect, dp(4), dp(4), paint);
                if (value == 0) continue;
                paint.setColor(value <= 4 ? INK : Color.WHITE);
                paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                paint.setTextAlign(Paint.Align.CENTER);
                String label = Integer.toString(value);
                paint.setTextSize(tile * (label.length() < 3 ? .43f : label.length() < 5 ? .32f : .22f));
                canvas.drawText(label, x + tile / 2, y + tile / 2 - (paint.ascent() + paint.descent()) / 2, paint);
            }
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: downX = event.getX(); downY = event.getY(); return true;
                case MotionEvent.ACTION_UP:
                    float dx = event.getX() - downX, dy = event.getY() - downY;
                    if (Math.max(Math.abs(dx), Math.abs(dy)) > dp(24)) {
                        move(Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? PuzzleEngine.RIGHT : PuzzleEngine.LEFT)
                                : (dy > 0 ? PuzzleEngine.DOWN : PuzzleEngine.UP));
                    } else {
                        performClick();
                        float gap = size * .025f, tile = (size - 5 * gap) / 4;
                        float x = event.getX() - left - gap, y = event.getY() - top - gap;
                        int col = (int) Math.floor(x / (tile + gap)), row = (int) Math.floor(y / (tile + gap));
                        if (col >= 0 && col < 4 && row >= 0 && row < 4 && x % (tile + gap) <= tile && y % (tile + gap) <= tile)
                            tap(row * 4 + col);
                        else secretStep = 0;
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL: secretStep = 0; return true;
                default: return true;
            }
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        private int tileColor(int value) {
            switch (value) {
                case 0: return 0xffcdc1b4;
                case 2: return 0xffeee4da;
                case 4: return 0xffede0c8;
                case 8: return 0xfff2b179;
                case 16: return 0xfff59563;
                case 32: return 0xfff67c5f;
                case 64: return 0xfff65e3b;
                case 128: return 0xffedcf72;
                case 256: return 0xffedcc61;
                case 512: return 0xffedc850;
                case 1024: return 0xffedc53f;
                default: return 0xffedc22e;
            }
        }
    }
}
