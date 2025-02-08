// ShipsGameUI.java

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.util.Arrays;

public class ShipsGameUI extends Application {
    private static final int GRID_SIZE = 9;
    private static final int CELL_SIZE = 40;
    private Rectangle[][] playerGrid = new Rectangle[GRID_SIZE][GRID_SIZE];
    private Rectangle[][] opponentGrid = new Rectangle[GRID_SIZE][GRID_SIZE];
    private int[][] ships = new int[GRID_SIZE][GRID_SIZE]; // 0: empty, 1: ship, 2: hit, 3: miss

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Label messageLabel = new Label("Place your ships (4-length ship)");

    private int currentShipLength = 4; // Start with the largest ship
    private int shipOrientation = 0; // 0: horizontal, 1: vertical, 2: diagonal
    private boolean placingShips = true;
    private boolean myTurn = false;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        connectToServer();

        messageLabel.setPadding(new Insets(10));

        VBox root = new VBox(10);
        root.setAlignment(Pos.CENTER);
        GridPane playerBoard = createGrid(playerGrid, true);
        GridPane opponentBoard = createGrid(opponentGrid, false);

        HBox boards = new HBox(10);
        boards.getChildren().addAll(playerBoard, opponentBoard);

        Button rotateButton = new Button("Rotate Ship");
        rotateButton.setOnAction(e -> rotateShip());

        root.getChildren().addAll(messageLabel, boards, rotateButton);

        Scene scene = new Scene(root, GRID_SIZE * CELL_SIZE * 2 + 100, GRID_SIZE * CELL_SIZE + 100);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Ships Game");
        primaryStage.show();
    }

    private GridPane createGrid(Rectangle[][] grid, boolean isPlayerBoard) {
        GridPane gridPane = new GridPane();
        gridPane.setAlignment(Pos.CENTER);
        gridPane.setPadding(new Insets(10));
        gridPane.setHgap(1);
        gridPane.setVgap(1);

        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                Rectangle cell = new Rectangle(CELL_SIZE, CELL_SIZE);
                cell.setFill(Color.LIGHTGRAY);
                cell.setStroke(Color.BLACK);
                grid[row][col] = cell;

                final int r = row;
                final int c = col;
                cell.setOnMouseClicked(e -> {
                    if (isPlayerBoard) {
                        if (placingShips) {
                            placeShip(cell, r, c);
                        }
                    } else {
                        if (!placingShips && myTurn) {
                            attackOpponent(r, c);
                        }
                    }
                });
                gridPane.add(cell, col, row);
            }
        }
        return gridPane;
    }

    private void placeShip(Rectangle cell, int row, int col) {
        if (isValidPlacement(row, col, currentShipLength, shipOrientation)) {
            switch (shipOrientation) {
                case 0: // Horizontal
                    for (int i = 0; i < currentShipLength; i++) {
                        ships[row][col + i] = 1;
                        playerGrid[row][col + i].setFill(Color.DARKBLUE);
                        // Add margins (example: 2 pixels on each side)
                        playerGrid[row][col + i].setStrokeWidth(2);
                        playerGrid[row][col + i].setStroke(Color.LIGHTGRAY);
                    }
                    break;
                case 1: // Vertical
                    for (int i = 0; i < currentShipLength; i++) {
                        ships[row + i][col] = 1;
                        playerGrid[row + i][col].setFill(Color.DARKBLUE);
                        playerGrid[row + i][col].setStrokeWidth(2);
                        playerGrid[row + i][col].setStroke(Color.LIGHTGRAY);
                    }
                    break;
                case 2: // Diagonal
                    for (int i = 0; i < currentShipLength; i++) {
                        ships[row + i][col + i] = 1;
                        playerGrid[row + i][col + i].setFill(Color.DARKBLUE);
                        playerGrid[row + i][col + i].setStrokeWidth(2);
                        playerGrid[row + i][col + i].setStroke(Color.LIGHTGRAY);
                    }
                    break;
            }

            currentShipLength--;
            if (currentShipLength > 0) {
                messageLabel.setText("Place your ships (" + currentShipLength + "-length ship)");
            } else {
                placingShips = false;
                sendBoardToServer();
                messageLabel.setText("Waiting for other player...");
            }
        } else {
            showAlert("Invalid Placement", "You cannot place a ship there.");
        }
    }

    private boolean isValidPlacement(int row, int col, int shipLength, int orientation) {
        switch (orientation) {
            case 0: // Horizontal
                if (col + shipLength > GRID_SIZE) {
                    return false;
                }
                for (int i = 0; i < shipLength; i++) {
                    if (ships[row][col + i] != 0) {
                        return false;
                    }
                }
                break;
            case 1: // Vertical
                if (row + shipLength > GRID_SIZE) {
                    return false;
                }
                for (int i = 0; i < shipLength; i++) {
                    if (ships[row + i][col] != 0) {
                        return false;
                    }
                }
                break;
            case 2: // Diagonal
                if (row + shipLength > GRID_SIZE || col + shipLength > GRID_SIZE) {
                    return false;
                }
                for (int i = 0; i < shipLength; i++) {
                    if (ships[row + i][col + i] != 0) {
                        return false;
                    }
                }
                break;
        }
        return true;
    }

    private void rotateShip() {
        shipOrientation = (shipOrientation + 1) % 3;
    }

    private void attackOpponent(int row, int col) {
        try {
            // Send attack coordinates to the server
            out.println(row + " " + col);
            myTurn = false;
            messageLabel.setText("Opponent's turn.");

            // No need to update the opponent's grid here, the server will handle it
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error communicating with server.");
        }
    }

    private void sendBoardToServer() {
        try {
            // Send the ship placement information to the server
            for (int[] row : ships) {
                out.println(Arrays.toString(row));
            }
            out.println("READY"); // Signal readiness to the server
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Error sending board to server.");
        }
    }

    private void connectToServer() {
        try {
            socket = new Socket("localhost", 12345);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Wait for the "START" message from the server
            String message = in.readLine();
            if (message.equals("START")) {
                messageLabel.setText("Your turn! (2 guesses)"); // Indicate 2 guesses
                myTurn = true;
            }

            // Start a new thread to listen for incoming messages
            new Thread(() -> {
                try {
                    while (true) {
                        String response = in.readLine();
                        if (response == null) {
                            break; // Connection closed
                        }

                        String[] parts = response.split(" ");
                        if (response.startsWith("HIT")) {
                            int row = Integer.parseInt(parts[1]);
                            int col = Integer.parseInt(parts[2]);
                            Platform.runLater(() -> {
                                opponentGrid[row][col].setFill(Color.RED);
                                opponentGrid[row][col].setOnMouseClicked(null); // Disable the cell
                            });
                        } else if (response.startsWith("MISS")) {
                            int row = Integer.parseInt(parts[1]);
                            int col = Integer.parseInt(parts[2]);
                            // Update your guess board with a miss marker (e.g., white)
                            Platform.runLater(() -> {
                                opponentGrid[row][col].setFill(Color.WHITE);
                                opponentGrid[row][col].setOnMouseClicked(null); // Disable the cell
                            });
                        } else if (response.startsWith("OPPONENT_HIT")) {
                            int row = Integer.parseInt(parts[1]);
                            int col = Integer.parseInt(parts[2]);
                            // Update your own board to show where the opponent hit
                            Platform.runLater(() -> playerGrid[row][col].setFill(Color.RED));
                        } else if (response.startsWith("OPPONENT_MISS")) {
                            int row = Integer.parseInt(parts[1]);
                            int col = Integer.parseInt(parts[2]);
                            // Update your own board to show where the opponent missed
                            Platform.runLater(() -> playerGrid[row][col].setFill(Color.WHITE));
                        } else if (response.equals("YOUR_TURN")) {
                            myTurn = true;
                            Platform.runLater(() -> messageLabel.setText("Your turn! (2 guesses)")); // Indicate 2 guesses
                        } else if (response.equals("OPPONENT_TURN")) {
                            myTurn = false;
                            Platform.runLater(() -> messageLabel.setText("Opponent's turn."));
                        } else if (response.equals("WIN")) {
                            Platform.runLater(() -> {
                                messageLabel.setText("YOU WIN!");
                                showRestartOrCloseDialog("You have won the game!");
                            });
                        } else if (response.equals("LOSE")) {
                            Platform.runLater(() -> {
                                messageLabel.setText("YOU LOSE!");
                                showRestartOrCloseDialog("You have lost the game!");
                            });
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    showAlert("Error", "Error communicating with server.");
                }
            }).start();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not connect to server.");
        }
    }

    private void showRestartOrCloseDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Game Over");
        alert.setHeaderText(message);
        alert.setContentText("Do you want to restart or close the game?");

        ButtonType restartButton = new ButtonType("Restart");
        ButtonType closeButton = new ButtonType("Close");

        alert.getButtonTypes().setAll(restartButton, closeButton);

        alert.showAndWait().ifPresent(result -> {
            if (result == restartButton) {
                resetGame(); // Call the resetGame() method to restart
            } else if (result == closeButton) {
                Platform.exit(); // Close the game
            }
        });
    }

    private void resetGame() {
        // Reset game variables
        ships = new int[GRID_SIZE][GRID_SIZE];
        currentShipLength = 4;
        shipOrientation = 0;
        placingShips = true;
        myTurn = false;

        // Clear the grids
        for (int row = 0; row < GRID_SIZE; row++) {
            for (int col = 0; col < GRID_SIZE; col++) {
                playerGrid[row][col].setFill(Color.LIGHTGRAY);
                opponentGrid[row][col].setFill(Color.LIGHTGRAY);
            }
        }

        // Update the message label
        messageLabel.setText("Place your ships (4-length ship)");
    }


    private void updatePlayerBoard(int row, int col, boolean hit) {
        if (hit) {
            playerGrid[row][col].setFill(Color.RED); // Mark as a hit on your board
        } else {
            playerGrid[row][col].setFill(Color.WHITE); // Mark as a miss on your board
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @Override
    public void stop() {
        try {
            if (out != null) {
                out.println("QUIT");
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}