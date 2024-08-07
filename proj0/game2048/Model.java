package game2048;

import java.util.Formatter;
import java.util.Observable;


/** The state of a game of 2048.
 *  @author moon
 */
public class Model extends Observable {
    /** Current contents of the board. */
    private Board board;
    /** Current score. */
    private int score;
    /** Maximum score so far.  Updated when game ends. */
    private int maxScore;
    /** True iff game is ended. */
    private boolean gameOver;

    /* Coordinate System: column C, row R of the board (where row 0,
     * column 0 is the lower-left corner of the board) will correspond
     * to board.tile(c, r).  Be careful! It works like (x, y) coordinates.
     */

    /** Largest piece value. */
    public static final int MAX_PIECE = 2048;

    /** A new 2048 game on a board of size SIZE with no pieces
     *  and score 0. */
    public Model(int size) {
        board = new Board(size);
        score = maxScore = 0;
        gameOver = false;
    }

    /** A new 2048 game where RAWVALUES contain the values of the tiles
     * (0 if null). VALUES is indexed by (row, col) with (0, 0) corresponding
     * to the bottom-left corner. Used for testing purposes. */
    public Model(int[][] rawValues, int score, int maxScore, boolean gameOver) {
        int size = rawValues.length;
        board = new Board(rawValues, score);
        this.score = score;
        this.maxScore = maxScore;
        this.gameOver = gameOver;
    }

    /** Return the current Tile at (COL, ROW), where 0 <= ROW < size(),
     *  0 <= COL < size(). Returns null if there is no tile there.
     *  Used for testing. Should be deprecated and removed.
     *  */
    public Tile tile(int col, int row) {
        return board.tile(col, row);
    }

    /** Return the number of squares on one side of the board.
     *  Used for testing. Should be deprecated and removed. */
    public int size() {
        return board.size();
    }

    /** Return true iff the game is over (there are no moves, or
     *  there is a tile with value 2048 on the board). */
    public boolean gameOver() {
        checkGameOver();
        if (gameOver) {
            maxScore = Math.max(score, maxScore);
        }
        return gameOver;
    }

    /** Return the current score. */
    public int score() {
        return score;
    }

    /** Return the current maximum game score (updated at end of game). */
    public int maxScore() {
        return maxScore;
    }

    /** Clear the board to empty and reset the score. */
    public void clear() {
        score = 0;
        gameOver = false;
        board.clear();
        setChanged();
    }

    /** Add TILE to the board. There must be no Tile currently at the
     *  same position. */
    public void addTile(Tile tile) {
        board.addTile(tile);
        checkGameOver();
        setChanged();
    }

    /** Tilt the board toward SIDE. Return true iff this changes the board.
     *
     * 1. If two Tile objects are adjacent in the direction of motion and have
     *    the same value, they are merged into one Tile of twice the original
     *    value and that new value is added to the score instance variable
     * 2. A tile that is the result of a merge will not merge again on that
     *    tilt. So each move, every tile will only ever be part of at most one
     *    merge (perhaps zero).
     * 3. When three adjacent tiles in the direction of motion have the same
     *    value, then the leading two tiles in the direction of motion merge,
     *    and the trailing tile does not.
     * */
    public boolean tilt(Side side) {
        boolean changed;
        changed = false;

        board.setViewingPerspective(side);

        // TODO: Modify this.board (and perhaps this.score) to account
        // for the tilt to the Side SIDE. If the board changed, set the
        // changed local variable to true.

        //keep track of which cells have seen a merge per tilt.
        boolean [][] mergedCells = {
                {false, false, false, false},
                {false, false, false, false,},
                {false, false, false, false,},
                {false, false, false, false,}
        };

        //column and row value on the reoriented board.
        int c;
        int r;
        for(c = board.size()-1; c >= 0; --c){
            for(r = board.size()-1; r >= 0; --r){

                Tile tStd = board.tile(c, r);
                if(tStd == null){
                    continue;
                }

                int nextMove = nextAvailableCell(c, r, tStd, board.size(), side, mergedCells);

                //implement the move if nextMove is different from original position
                if(nextMove != r){
                    int nextCol = c;
                    int nextRow = nextMove;
                    boolean merge = board.move(nextCol, nextRow, tStd);
                    if(merge){
                        this.score += board.tile(nextCol, nextRow).value();
                    }
                    changed = true;

                }


            }
        }

        checkGameOver();
        if (changed) {
            setChanged();
        }

        board.setViewingPerspective(Side.NORTH);
        return changed;
    }



    public int nextAvailableCell(int orientedC, int orientedR, Tile tile,  int size, Side s, boolean[][] arr){

        int result = 0;

        //if the tile is vertically at the end of the Side on the
        // Reoriented board, return the same row.
        if(orientedR >= size-1){
            return orientedR;
        }

        //Iterate through the oriented column starting from next row until the end
        for(int i = orientedR + 1; i < size; i++){

            /*  return the values and not set the values for result because the column search is from down up
               which can give us wrong results because the algorithm will keep searching for next cell in cells above.
            */
            if(board.tile(orientedC, i) != null && board.tile(orientedC, i).value() != tile.value()){
                return i-1; //land before the tile
            }

            else if(board.tile(orientedC, i) != null && board.tile(orientedC, i).value() == tile.value()){
                if(arr[i][orientedC]){
                    return i-1; // land before the tile if the tile has been merged with in the same tilt.

                }
                else {
                    arr[i][orientedC] = true;
                    return  i; //Land on the tile
                }
            }

            else{
                result = size - 1; //land at the end of the board
            }
        }


        return result;

    }

    /** Checks if the game is over and sets the gameOver variable
     *  appropriately.
     */
    private void checkGameOver() {
        gameOver = checkGameOver(board);
    }

    /** Determine whether game is over. */
    private static boolean checkGameOver(Board b) {
        return maxTileExists(b) || !atLeastOneMoveExists(b);
    }

    /** Returns true if at least one space on the Board is empty.
     *  Empty spaces are stored as null.
     * */
    public static boolean emptySpaceExists(Board b) {
        for(int row = 0; row < b.size(); row++){
            for(int col = 0; col < b.size(); col++){
                if(b.tile(col, row) == null){
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns true if any tile is equal to the maximum valid value.
     * Maximum valid value is given by MAX_PIECE. Note that
     * given a Tile object t, we get its value with t.value().
     */
    public static boolean maxTileExists(Board b) {
        for(int row = 0; row < b.size(); row++){
            for(int col = 0; col < b.size(); col++){
                if((b.tile(col, row) != null) && (b.tile(col, row).value() == MAX_PIECE)){
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Returns true if there are any valid moves on the board.
     * There are two ways that there can be valid moves:
     * 1. There is at least one empty space on the board.
     * 2. There are two adjacent tiles with the same value.
     */
    public static boolean atLeastOneMoveExists(Board b) {
        if(emptySpaceExists(b) || sameValueNeighborExists(b)){
            return true;
        }
        return false;
    }

    /**
     * Helper function for atLeastOneMoveExists
     * Returns true if the coordinates are valid for a tile
     */
    public static boolean validCoordinate(Board b, int row, int col){
        if(row >= b.size() || row < 0 || col >= b.size() || col < 0){
            return false;
        }
        return true;
    }

    /**
     * Helper function for atLeastOneMoveExists
     * Returns true if any of the non-diagonal neighbor tiles have the same value.
     */
    public static boolean sameValueNeighborExists(Board b) {
        for (int row = 0; row < b.size(); row++) {
            for (int col = 0; col < b.size(); col++) {
                if(sameValueinRow(row, col, b) || sameValueinCol(row, col, b)){
                    return true;
                }

            }
        }

        return false;
    }

    /**
     * Helper function for sameValueNeighbor
     * Returns true if a tile on the left or right has the same value
     */
    public static boolean sameValueinRow(int row, int col, Board b){
        for(int x = -1; x <= 1; x++){
            //test if valid coordinate, not self, and not empty
            if(validCoordinate(b, (row + x), col) && x != 0 && b.tile(col, row+x) != null) {
                if (b.tile(col, row).value() == b.tile(col, row + x).value()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Helper function for sameValueNeighbor
     * Returns true if a tile on the top or bottom has the same value
     */
    public static boolean sameValueinCol(int row, int col, Board b){
        for(int y = -1; y <= 1; y++){
            if(validCoordinate(b, (row), col+y) && y != 0 && b.tile(col+y, row) != null){
                if(b.tile(col, row).value() == b.tile(col+y, row).value()){
                    return true;
                }
            }
        }
        return false;
    }







    @Override
     /** Returns the model as a string, used for debugging. */
    public String toString() {
        Formatter out = new Formatter();
        out.format("%n[%n");
        for (int row = size() - 1; row >= 0; row -= 1) {
            for (int col = 0; col < size(); col += 1) {
                if (tile(col, row) == null) {
                    out.format("|    ");
                } else {
                    out.format("|%4d", tile(col, row).value());
                }
            }
            out.format("|%n");
        }
        String over = gameOver() ? "over" : "not over";
        out.format("] %d (max: %d) (game is %s) %n", score(), maxScore(), over);
        return out.toString();
    }

    @Override
    /** Returns whether two models are equal. */
    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } else if (getClass() != o.getClass()) {
            return false;
        } else {
            return toString().equals(o.toString());
        }
    }

    @Override
    /** Returns hash code of Model’s string. */
    public int hashCode() {
        return toString().hashCode();
    }
}
