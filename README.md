# Battleship: Network Edition

This project is a Java implementation of the classic Battleship game for two players over a network.  It includes a server (`ShipsServer.java`) and a client (`ShipsGameUI.java`). This README covers both the player instructions and developer setup.

**Important Note:** This version of Battleship uses a 9x9 grid, and each player places *five* ships, *each occupying a single cell (1x1)*. Each player gets *one* shot per turn.

## Gameplay Overview (for Players)

**Objective:** Be the first player to sink all five of your opponent's ships.

**Setup:**

1.  **Server:** One player must run the `ShipsServer.java` file (see "Setup (for Developers)" below). The server will listen for connections on port 12345.  The server's IP address (or hostname) is needed by both clients.
2.  **Clients:** Both players run the `ShipsGameUI.java` client application.
3.  **Connection:** Launch the client.  It will automatically attempt to connect to `localhost` on port 12345.  If the server is on a different machine, you'll need to modify the `connectToServer()` method in `ShipsGameUI.java` to use the correct IP address.
4.  **Ship Placement:** You'll see two grids.  The left grid is yours; the right grid is your opponent's (initially empty).  Click on squares on *your* (left) grid to place your five ships.  Each ship occupies only *one* square.  You will place them one at a time.
5. **Ready:** After placing your ships the game will be ready.

**Playing the Game:**

1.  **Turns:** The game starts, and one player is randomly chosen to go first.
2.  **Firing (One Shot per Turn):** On your turn, click *once* on a square in your *opponent's* (right) grid to fire a shot.
3.  **Hit or Miss:**  The square you clicked will change color:
    *   **Red:**  You hit an opponent's ship.
    *   **White:** You missed.
    *   The opponent's grid will show where *you* shot (hit or miss).
    *   Your grid will show where *your opponent* shot (hit or miss).
4.  **Opponent's Turn:** After you fire, it's your opponent's turn. They will click on your grid to fire.
5.  **Winning:** The first player to hit all five of their opponent's ships (red squares on the opponent's grid) wins.  A message will appear announcing the winner.

**Game Controls:**

*   **Ship Placement:** Click on individual squares on your (left) grid to place your five single-square ships.
*   **Firing:** Click on a square on your opponent's (right) grid to fire a shot.
* **Reset**: Click on yes after a player wins in order to restart the game.

**Troubleshooting:**

*   **Connection Problems:**
    *   Make sure `ShipsServer.java` is running.
    *   The client tries to connect to `localhost`.  If the server is on another machine, change the `connectToServer()` method in `ShipsGameUI.java` to use the correct IP address.
    *   Check your firewall settings.
*   **Game Freezes:** Restart both the client and server applications.

## Setup (for Developers)

1.  **Prerequisites:**
    *   Java Development Kit (JDK) 8 or later.
    *   JavaFX (included with most modern JDKs; if not, install it separately).

2.  **Clone the Repository:**

    ```bash
    git clone <repository_url>
    cd <repository_directory>
    ```

3.  **Compile:**

    ```bash
    javac ShipsServer.java ShipsGameUI.java
    ```

4.  **Run the Server:**

    ```bash
    java ShipsServer
    ```

5.  **Run the Clients (Two Instances):**

    ```bash
    java ShipsGameUI
    ```
    Run this command *twice*, in separate terminal windows, to start two client instances.

## Project Structure

*   `ShipsServer.java`: The game server.
*   `ShipsGameUI.java`: The JavaFX game client.
*   `README.md`: This file.

## Contributing
Feel free to contribute.
