package com.oasisfeng.island.puzzle;

import java.util.Random;

/** Standalone rules regression check: javac PuzzleEngine*.java && java ...PuzzleEngineCheck. */
public final class PuzzleEngineCheck {
    public static void main(String[] args) {
        PuzzleEngine game = new PuzzleEngine(new Random(7));
        int[] board = new int[16];
        board[0] = 2; board[1] = 2; board[2] = 2; board[3] = 2;
        require(game.restore(board, 0), "restore");
        require(game.move(PuzzleEngine.LEFT), "left changes");
        require(game.tile(0) == 4 && game.tile(1) == 4, "each tile merges once");
        require(game.score() == 8, "score sums merges");
        require(nonzero(game) == 3, "one tile spawns after changed move");
        board = new int[]{2,4,2,4, 4,2,4,2, 2,4,2,4, 4,2,4,2};
        game.restore(board, 15);
        require(!game.canMove(), "full checkerboard ends game");
        require(!game.move(PuzzleEngine.UP), "invalid move unchanged");
        require(game.score() == 15 && nonzero(game) == 16, "invalid move does not spawn or score");
        board = new int[16]; board[0] = 8; board[4] = 8;
        game.restore(board, 0); game.move(PuzzleEngine.DOWN);
        require(game.tile(12) == 16 && game.score() == 16, "down merges");
        board = new int[16]; board[0] = 8; board[4] = 8;
        game.restore(board, 0); game.move(PuzzleEngine.UP);
        require(game.tile(0) == 16 && game.score() == 16, "up merges");
        board = new int[16]; board[0] = 8; board[1] = 8;
        game.restore(board, 0); game.move(PuzzleEngine.RIGHT);
        require(game.tile(3) == 16 && game.score() == 16, "right merges");
        require(!game.restore(new int[16], 0), "empty save rejected");
        board[0] = 3;
        require(!game.restore(board, 0), "invalid tile rejected");
        game.reset();
        require(nonzero(game) == 2 && game.score() == 0 && game.canMove(), "new game");
        System.out.println("PuzzleEngine checks passed");
    }
    private static int nonzero(PuzzleEngine game) {
        int count = 0;
        for (int i = 0; i < 16; i++) if (game.tile(i) != 0) count++;
        return count;
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
