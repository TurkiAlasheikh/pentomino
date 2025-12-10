// File: src/main/java/com/example/pentomino/PentominoPuzzleGame.java
package com.example.pentomino;

import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.*;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.*;
import java.util.stream.Collectors; // allows later code to be used in a shortcut

public class Launcher extends Application {

    private static final int COLS = 10, ROWS = 6, CELL = 40;
    private static final int POOL_HEIGHT = CELL * 6;
    private static final int BOARD_WIDTH = COLS * CELL, BOARD_HEIGHT = ROWS * CELL;
    private static final int INITIAL_SECONDS = 240;
    // since we're using these numbers constantly, we've decided it would be easier if they were variables.
    // it would also be more digestable
    private Pane poolPane, boardPane;


    private StackPane rootStack;
    private BorderPane chrome;
    private Pane glassLayer;

    private Label timerLabel;
    private Label elapsedLabel; // counter up
    private final Board board = new Board(COLS, ROWS);
    private final Random rng = new Random();
    private final List<Pentomino> pentominoSet = PentominoLibrary.all(); //adds all 12 types of pentominoes to a set

    private Timeline gameTimer; //lifetime of the program
    private final IntegerProperty remaining = new SimpleIntegerProperty(INITIAL_SECONDS);
    private final IntegerProperty elapsed = new SimpleIntegerProperty(0); // both are used for showing time

    private DraggablePiece currentPoolPiece = null; // this is for pieces in the pool, to be used
    private final Set<DraggablePiece> boardPieces = new HashSet<>(); //pieces in the board

    // "focused" is the state when the piece has been clicked, abling it to move and rotate
    private final ObjectProperty<DraggablePiece> focused = new SimpleObjectProperty<>();

    private boolean gameOver = false;

    @Override public void start(Stage stage) {
        stage.setTitle("Pentomino Puzzle Game");

        //sets up our scene
        chrome = new BorderPane();
        chrome.setPadding(new Insets(10));
        chrome.setTop(buildControls());
        chrome.setCenter(buildPlayfield());

        glassLayer = new Pane();
        glassLayer.setPickOnBounds(false); // allows mouse to click on objects somewhat accurately

        rootStack = new StackPane(chrome, glassLayer);
        // stackpane allows pentomnioes to be seen above the grid layer

        Scene scene = new Scene(rootStack, Math.max(BOARD_WIDTH, 600), POOL_HEIGHT + BOARD_HEIGHT + 120);
        scene.setOnKeyPressed(e -> {
            if (gameOver) return;
            var s = focused.get();
            if (s == null) return;
            if (e.getCode() == KeyCode.RIGHT) s.rotateCW();
            if (e.getCode() == KeyCode.LEFT)  s.rotateCCW();
        }); // rotates pentominoes ^

        stage.setScene(scene);
        stage.show();
        startNewGame();
    }

    private Node buildControls() {
        HBox box = new HBox(20);
        box.setPadding(new Insets(6));

        Label title = new Label("Pentomino Puzzle");
        timerLabel = new Label();
        timerLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");
        // set style is important, its so we can see the text displayed at the top
        timerLabel.textProperty().bind(remaining.asString("Time Left: %ds"));

        elapsedLabel = new Label();
        elapsedLabel.setStyle("-fx-font-size:16px;-fx-font-weight:bold;");
        elapsedLabel.textProperty().bind(elapsed.asString("Elapsed time of block: %ds"));

        // code for the restart button, the "startnewgame" method can be found below
        Button restartBtn = new Button("Restart");
        restartBtn.setOnAction(e -> startNewGame());

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        box.getChildren().addAll(title, spacer, elapsedLabel, timerLabel, restartBtn);
        return box;
        // "box" is the top part of the program (with all the controls), the spacer region is the small parts on the side
        // buildControl method returns this box as a Node for the scene
    }

    private Node buildPlayfield() {
        VBox v = new VBox(8);


        poolPane = new Pane();
        poolPane.setPrefSize(BOARD_WIDTH, POOL_HEIGHT);
        poolPane.setMinSize(BOARD_WIDTH, POOL_HEIGHT);
        poolPane.setStyle("-fx-background-color:#f6f7fb;-fx-border-color:#cfd3e1;-fx-border-width:1;");

        boardPane = new Pane();
        boardPane.setPrefSize(BOARD_WIDTH, BOARD_HEIGHT);
        boardPane.setMinSize(BOARD_WIDTH, BOARD_HEIGHT);
        boardPane.setStyle("-fx-background-color:white;-fx-border-color:#cfd3e1;-fx-border-width:1;");

        drawGrid(boardPane);
        v.getChildren().addAll(poolPane, boardPane);
        return v;
        // "v" represents both panes, also returned as a Node
    }

    private void drawGrid(Pane pane) {
        pane.getChildren().clear();
        pane.getChildren().add(new Rectangle(BOARD_WIDTH, BOARD_HEIGHT){{
            setFill(Color.WHITE);
        }});
        // colors the board
        for (int c=0;c<=COLS;c++) {
            Line line = new Line(c*CELL,0,c*CELL,ROWS*CELL);
            line.setStroke(Color.LIGHTGRAY);
            pane.getChildren().add(line);
        }
        for (int r=0;r<=ROWS;r++) {
            Line line = new Line(0,r*CELL,COLS*CELL,r*CELL);
            line.setStroke(Color.LIGHTGRAY);
            pane.getChildren().add(line);
        }
        //colors the grid to have a slightly different color than the main board
        pane.setViewOrder(1); // positive values allow the pane to appear behind pentominoes
    }

    private void startNewGame() {
        if (gameTimer != null) gameTimer.stop(); // exception handling, avoids NullPointerException

        board.clear();
        remaining.set(INITIAL_SECONDS);
        elapsed.set(0);
        gameOver = false;

        poolPane.getChildren().clear();
        boardPane.getChildren().removeIf(n -> n instanceof DraggablePiece); // removes pieces
        boardPieces.clear();
        clearFocus();
        drawGrid(boardPane);
        currentPoolPiece = null;

        Alert alert1 = new Alert(Alert.AlertType.INFORMATION, "Double click to lock into a block, use left and right arrow keys to rotate pieces");
        Alert alert2 = new Alert(Alert.AlertType.INFORMATION, "Unclicked blocks disappear after 5-10 seconds, while clicked blocks stick around for 20-30 seconds. Enjoy!");
        alert1.showAndWait();
        alert2.showAndWait();
        setupGameTimer();
        spawnNext();

    }

    private void setupGameTimer() {
        gameTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            // up-counter
            elapsed.set(elapsed.get() + 1);
            // down-counter
            remaining.set(remaining.get() - 1);
            if (remaining.get() <= 0) endGame(false, "Time's up!");
        }));
        gameTimer.setCycleCount(Animation.INDEFINITE);
        gameTimer.playFromStart();
    }

    private void spawnNext() {
        if (gameOver || currentPoolPiece != null) return;

        Pentomino p = pentominoSet.get(rng.nextInt(pentominoSet.size()));
        Color color = Color.hsb(rng.nextDouble()*360, 0.75, 0.95);
        // this spawns a random pentomino from pentomino set, with a random color

        DraggablePiece node = new DraggablePiece(p, color, false);
        // the third perimeter is to check whether the pentomino is in the grid or on the board.
        node.setLayoutX(rng.nextDouble()*(BOARD_WIDTH - 4*CELL));
        node.setLayoutY(rng.nextDouble()*(POOL_HEIGHT - 3*CELL));
        // this sets the location, randomized due to "next double"

        currentPoolPiece = node;
        poolPane.getChildren().add(node);
        node.toFront();
    }

    private void endGame(boolean win, String reason) {
        if (gameOver) return;
        gameOver = true;
        if (gameTimer != null) gameTimer.stop();

        Alert alert = new Alert(win ? Alert.AlertType.INFORMATION : Alert.AlertType.ERROR,
                win ? "You win!" : "You lose. " + reason);
        // the ? represents a simpler "if else", the first part being the "if" and the one after the : being the else
        alert.show();

        timerLabel.setText(win ? "You win! Grid complete." : "You lose. " + reason);
        if (currentPoolPiece != null) currentPoolPiece.setDisable(true);
        boardPieces.forEach(n -> n.setDisable(true));
        // game over, setDisable disables dragging pieces and more
    }

    private void onBoardChanged() {
        if (board.isComplete()) endGame(true, "");
    }

    // method for focusing on a clicked block
    private void setFocus(DraggablePiece p) {
        var old = focused.get();
        if (old == p) return;
        if (old != null) old.setFocusedVisual(false);
        focused.set(p);
        if (p != null) p.setFocusedVisual(true);
    }
    private void clearFocus() {
        var old = focused.get();
        if (old != null) old.setFocusedVisual(false);
        focused.set(null);
    }

    private class DraggablePiece extends Group {
        private Pentomino base;
        private int rotation;
        private final List<Rectangle> rects = new ArrayList<>();
        private final Color color;
        private boolean lockedOnBoard = false;

        // vanish control
        private PauseTransition vanishTimer;
        private boolean everClicked = false; // switches timing & enables fade

        DraggablePiece(Pentomino p, Color color, boolean locked) {
            this.base = p;
            this.rotation = 0;
            this.color = color;
            this.lockedOnBoard = locked;
            setFocusTraversable(true);
            rebuild();
            enableDrag();
            enableSelection();
            enableContextRemove();

            // start random 5 to 10 seconds vanish for unclicked pieces
            startVanishCountdown(false);
        }

        private void setFocusedVisual(boolean on) {
            setScaleX(on ? 1.03 : 1.0);
            setScaleY(on ? 1.03 : 1.0);
        }

        private void rebuild() {
            getChildren().clear();
            rects.clear();
            for (Cell c : base.rotated(rotation)) {
                Rectangle r = new Rectangle(CELL-2, CELL-2);
                r.setFill(color);
                r.setStroke(Color.GRAY);
                r.setArcWidth(8); r.setArcHeight(8);
                r.setLayoutX(c.x()*CELL + 1);
                r.setLayoutY(c.y()*CELL + 1);
                rects.add(r);
            }
            getChildren().addAll(rects);
        }
        // rebuild method allows the piece to keep its visuals after rotation or placing

        private void enableSelection() {
            setOnMouseClicked(e -> {
                if (gameOver) return;

                if (e.getButton() == MouseButton.PRIMARY) {
                    // Double-click on a placed piece deletes it
                    if (e.getClickCount() == 2 && lockedOnBoard) {
                        deletePlacedPiece();
                        e.consume();
                        return;
                    }
                    // Single-click focuses; if not placed, switch to clicked-window (20..30s)
                    setFocus(this);
                    requestFocus();
                    if (!lockedOnBoard) {
                        if (!everClicked) everClicked = true;
                        startVanishCountdown(true); // 20..30s with tiny fade on vanish
                    }
                }
            });
        }
        // allows mouse to select pieces

        private void enableContextRemove() {
            setOnMousePressed(e -> {
                if (gameOver) return;
                // Right-click deletes placed piece
                if (e.getButton() == MouseButton.SECONDARY && lockedOnBoard) {
                    deletePlacedPiece();
                    e.consume();
                }
            });
        }

        private void enableDrag() {
            final Delta d = new Delta();

            setOnMousePressed(e -> {
                if (gameOver) return;
                if (lockedOnBoard) return; // immobile once placed

                setFocus(this);
                if (!everClicked) everClicked = true; // drag counts as clicked
                startVanishCountdown(true);            // 20 to 30 seconds vanish

                // Reparent to overlay at same screen position (always on top)
                Point2D scenePos = localToScene(0, 0);
                if (getParent() instanceof Pane parent) {
                    parent.getChildren().remove(this);
                    glassLayer.getChildren().add(this);
                    setLayoutX(scenePos.getX());
                    setLayoutY(scenePos.getY());
                    toFront();
                }

                d.x = e.getSceneX() - getLayoutX();
                d.y = e.getSceneY() - getLayoutY();
                // for the piece to track the mouse's position while dragged
                e.consume();
            });

            setOnMouseDragged(e -> {
                if (gameOver) return;
                if (lockedOnBoard) return;
                if (focused.get() != this) return;
                setLayoutX(e.getSceneX() - d.x);
                setLayoutY(e.getSceneY() - d.y);
                e.consume();
            });

            setOnMouseReleased(e -> {
                if (gameOver) return;
                if (lockedOnBoard) return;
                if (focused.get() != this) return;

                Bounds boardScene = boardPane.localToScene(boardPane.getLayoutBounds());
                Bounds thisScene  = localToScene(getBoundsInLocal());

                if (boardScene.intersects(thisScene)) tryPlaceOnBoardFromGlass();
                else {}

                e.consume();
            });
        }

        // rotates pieces and rebuilds after that
        void rotateCW()  { rotate(+1); }
        void rotateCCW() { rotate(-1); }
        private void rotate(int dir) {
            if (lockedOnBoard) return; // immobile once placed
            rotation = (rotation + (dir>0?1:3)) % 4;
            rebuild();
        }
        // this method locks the piece onto the place
        private void tryPlaceOnBoardFromGlass() {
            Bounds pieceScene = localToScene(getBoundsInLocal());
            Point2D boardOrigin = boardPane.localToScene(0,0);
            double relX = pieceScene.getMinX() - boardOrigin.getX();
            double relY = pieceScene.getMinY() - boardOrigin.getY();
            int anchorX = (int)Math.round(relX / CELL);
            int anchorY = (int)Math.round(relY / CELL);

            if (board.fits(base, rotation, anchorX, anchorY)) {
                glassLayer.getChildren().remove(this);
                boardPane.getChildren().add(this);
                setLayoutX(anchorX * CELL);
                setLayoutY(anchorY * CELL);
                toFront();

                board.place(base, rotation, anchorX, anchorY, this);
                lockedOnBoard = true;
                boardPieces.add(this);

                cancelVanish();          // placing stops vanish, resetting resets elapsed value
                elapsed.set(0);

                if (currentPoolPiece == this) currentPoolPiece = null;
                spawnNext();
                onBoardChanged();
            }

        }
        // double clicking a piece inside the board removes it completely
        private void deletePlacedPiece() {
            if (!lockedOnBoard) return;
            board.remove(this);
            lockedOnBoard = false;
            boardPieces.remove(this);
            if (focused.get() == this) clearFocus();
            Parent p = getParent();
            if (p instanceof Pane parent) parent.getChildren().remove(this);
            glassLayer.getChildren().remove(this);
            onBoardChanged();
        }

        // --- vanish helpers ---
        private void startVanishCountdown(boolean clicked) {
            if (lockedOnBoard) {
                cancelVanish();
                return;
            }

            int seconds;
            if (!clicked) {
                seconds = 5 + rng.nextInt(6); // 5 to 10 random
            } else {
                seconds = 20 + rng.nextInt(11); // 20 to 30 random
            }

            if (vanishTimer == null) {
                vanishTimer = new PauseTransition();
                vanishTimer.setOnFinished(e -> {
                    if (lockedOnBoard) return; // placed since timer started
                    // fade only if clicked
                    if (everClicked) {
                        FadeTransition ft = new FadeTransition(Duration.millis(150), this);
                        ft.setFromValue(1.0);
                        ft.setToValue(0.0);
                        ft.setOnFinished(ev -> {
                            removeFromScene();
                            setOpacity(1.0);
                        });
                        ft.play();
                    } else {
                        removeFromScene();
                    }
                });
            }
            vanishTimer.stop();
            vanishTimer.setDuration(Duration.seconds(seconds));
            vanishTimer.playFromStart();
        }

        // after placing, cancel vanish
        private void cancelVanish() {
            if (vanishTimer != null) vanishTimer.stop();
        }
        // this removes unplaced pieces after the timer finishes
        private void removeFromScene() {
            Parent p = getParent();
            if (p instanceof Pane parent) parent.getChildren().remove(this);
            glassLayer.getChildren().remove(this);
            if (currentPoolPiece == this) currentPoolPiece = null;
            elapsed.set(0);   // <<< reset elapsed on vanish
            spawnNext();
        }

        private static final class Delta {
            double x, y;
        }
    }

    private static class Board {
        private final int w, h;
        private final boolean[][] occ; // occupied
        private final Map<DraggablePiece, List<Cell>> placement = new HashMap<>();
        Board(int w, int h){
            this.w=w;
            this.h=h;
            this.occ=new boolean[w][h];
        }
        void clear(){
            for(int x=0;x<w;x++) Arrays.fill(occ[x], false);
            placement.clear();
        }
        boolean fits(Pentomino p,int rot,int ax,int ay){
            for (Cell c : p.rotated(rot)) {
                int x=ax+c.x(), y=ay+c.y();
                if (x<0||y<0||x>=w||y>=h) return false;
                if (occ[x][y]) return false;
            } return true;
        }
        void place(Pentomino p,int rot,int ax,int ay,DraggablePiece node){
            List<Cell> cells = new ArrayList<>();
            for (Cell c : p.rotated(rot)) { int x=ax+c.x(), y=ay+c.y(); occ[x][y]=true; cells.add(new Cell(x,y)); }
            placement.put(node, cells);
        }
        void remove(DraggablePiece node){
            List<Cell> cells = placement.remove(node);
            if (cells==null) return;
            for (Cell c:cells) occ[c.x()][c.y()]=false;
        }
        boolean isComplete(){
            for(int x=0;x<w;x++) for(int y=0;y<h;y++) if(!occ[x][y]) return false;
            return true;
        }
    }

    private record Cell(int x,int y){}

    private static class Pentomino {
        private final String name;
        private final List<Cell> cells;

        Pentomino(String name,List<Cell> cells) {
            this.name=name;
            this.cells=normalize(cells);
        }

        List<Cell> rotated(int r){
            List<Cell> pts=cells;
            for(int i=0;i<r;i++) pts=rotateCW(pts);
            return normalize(pts);
        }

        private static List<Cell> rotateCW(List<Cell> pts){
            List<Cell> out= new ArrayList<>();
            for(Cell c:pts) out.add(new Cell(c.y(),-c.x()));
            return out;
        }
        //this keeps the pentomino anchored when rotating
        private static List<Cell> normalize(List<Cell> pts){
            int minX = pts.stream().mapToInt(Cell::x).min().orElse(0);
            int minY = pts.stream().mapToInt(Cell::y).min().orElse(0);
            return pts.stream().map(c -> new Cell(c.x()-minX, c.y()-minY)).collect(Collectors.toList());
        }
        // this method creates the pentominoes
        static Pentomino of(String n,int[][] xy){
            List<Cell> pts=new ArrayList<>();
            for(int[] p:xy) pts.add(new Cell(p[0],p[1]));
            return new Pentomino(n,pts);
        }
    }
    // this creates a list with all pentominoes to be chosen randomly
    private static class PentominoLibrary {
        static List<Pentomino> all() {
            return List.of(
                    Pentomino.of("F", new int[][]{{1,0},{0,1},{1,1},{1,2},{2,2}}),
                    Pentomino.of("I", new int[][]{{0,0},{0,1},{0,2},{0,3},{0,4}}),
                    Pentomino.of("L", new int[][]{{0,0},{0,1},{0,2},{0,3},{1,3}}),
                    Pentomino.of("P", new int[][]{{0,0},{0,1},{0,2},{1,0},{1,1}}),
                    Pentomino.of("N", new int[][]{{0,0},{0,1},{1,1},{1,2},{1,3}}),
                    Pentomino.of("T", new int[][]{{0,0},{1,0},{2,0},{1,1},{1,2}}),
                    Pentomino.of("U", new int[][]{{0,0},{0,1},{1,1},{2,0},{2,1}}),
                    Pentomino.of("V", new int[][]{{0,0},{0,1},{0,2},{1,2},{2,2}}),
                    Pentomino.of("W", new int[][]{{0,0},{1,1},{2,2},{1,0},{2,1}}),
                    Pentomino.of("X", new int[][]{{1,0},{0,1},{1,1},{2,1},{1,2}}),
                    Pentomino.of("Y", new int[][]{{0,0},{0,1},{0,2},{0,3},{1,3}}),
                    Pentomino.of("Z", new int[][]{{0,0},{1,0},{1,1},{1,2},{2,2}})
            );
        }
    }

    public static void main(String[] args) { launch(args); }
}
