package com.oasisfeng.island.puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Android-independent 2048 rules. A tile can merge only once per move. */
public final class PuzzleEngine {
    public static final int LEFT = 0, UP = 1, RIGHT = 2, DOWN = 3;
    private final int[] cells = new int[16];
    private final Random random;
    private long score;

    public PuzzleEngine() { this(new Random()); }
    public PuzzleEngine(Random random) { this.random = random; }
    public int tile(int index) { return cells[index]; }
    public long score() { return score; }
    public void reset() {
        java.util.Arrays.fill(cells, 0);
        score = 0;
        spawn(); spawn();
    }
    public boolean restore(int[] board, long savedScore) {
        if (board == null || board.length != 16 || savedScore < 0) return false;
        boolean occupied = false;
        for (int value : board) {
            if (value < 0 || value == 1 || (value != 0 && (value & (value - 1)) != 0)) return false;
            occupied |= value != 0;
        }
        if (!occupied) return false;
        System.arraycopy(board, 0, cells, 0, 16);
        score = savedScore;
        return true;
    }
    public boolean move(int direction) {
        if (direction < LEFT || direction > DOWN) throw new IllegalArgumentException("direction");
        boolean changed = false;
        for (int line = 0; line < 4; line++) {
            int[] packed = new int[4];
            int count = 0;
            for (int pos = 0; pos < 4; pos++) {
                int value = cells[index(direction, line, pos)];
                if (value != 0) packed[count++] = value;
            }
            int[] merged = new int[4];
            int target = 0;
            for (int pos = 0; pos < count; pos++) {
                int value = packed[pos];
                if (pos + 1 < count && packed[pos + 1] == value && value < (1 << 30)) {
                    value *= 2;
                    score += value;
                    pos++;
                }
                merged[target++] = value;
            }
            for (int pos = 0; pos < 4; pos++) {
                int index = index(direction, line, pos);
                changed |= cells[index] != merged[pos];
                cells[index] = merged[pos];
            }
        }
        if (changed) spawn();
        return changed;
    }
    public boolean canMove() {
        for (int i = 0; i < 16; i++) {
            if (cells[i] == 0) return true;
            if (cells[i] < (1 << 30) && ((i % 4 < 3 && cells[i] == cells[i + 1])
                    || (i < 12 && cells[i] == cells[i + 4]))) return true;
        }
        return false;
    }
    private int index(int direction, int line, int pos) {
        switch (direction) {
            case LEFT: return line * 4 + pos;
            case RIGHT: return line * 4 + 3 - pos;
            case UP: return pos * 4 + line;
            default: return (3 - pos) * 4 + line;
        }
    }
    private void spawn() {
        List<Integer> empty = new ArrayList<>();
        for (int i = 0; i < 16; i++) if (cells[i] == 0) empty.add(i);
        if (!empty.isEmpty()) cells[empty.get(random.nextInt(empty.size()))] = random.nextInt(10) == 0 ? 4 : 2;
    }
}
