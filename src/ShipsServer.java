import java.io.*;
import java.net.*;
import java.util.*;

public class ShipsServer {
    private static final int PORT = 12345;
    private static final int GRID_SIZE = 9;
    private static List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            // Accept only two clients
            while (clients.size() < 2) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket);

                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                new Thread(clientHandler).start();
            }

            // Prevent further connections
            serverSocket.close();

            // Notify clients that the game is ready to start
            for (ClientHandler client : clients) {
                client.out.println("START");
            }
        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private int[][] ships = new int[GRID_SIZE][GRID_SIZE];
        private boolean ready = false;

        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                // Receive and process client messages
                receiveBoard();

                // Wait for both players to be ready
                waitForBothPlayers();

                // Start the game loop
                gameLoop();

            } catch (IOException | InterruptedException e) {
                System.err.println("Error handling client: " + e.getMessage());
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                synchronized (clients) {
                    clients.remove(this);
                    clients.notifyAll(); // Notify other clients if this one disconnects
                }
            }
        }

        private void receiveBoard() throws IOException {
            // Receive the ship placement information from the client
            for (int i = 0; i < GRID_SIZE; i++) {
                String row = in.readLine();
                String[] values = row.substring(1, row.length() - 1).split(", ");
                for (int j = 0; j < GRID_SIZE; j++) {
                    ships[i][j] = Integer.parseInt(values[j]);
                }
            }
            in.readLine(); // Consume the "READY" message
            ready = true;
        }

        private void waitForBothPlayers() throws InterruptedException {
            // Wait for both clients to be ready before starting the game
            synchronized (clients) {
                while (clients.size() < 2 || !allClientsReady()) {
                    clients.wait();
                }
                clients.notifyAll(); // Notify all clients that both are ready
            }
        }

        private void gameLoop() throws IOException {
            // Game starts, arbitrarily choose the first client to start
            clients.get(0).out.println("YOUR_TURN");
            clients.get(1).out.println("OPPONENT_TURN");
            int currentPlayerIndex = 0; // Keep track of current player

            while (true) {
                String message = in.readLine();
                if (message == null) {
                    break;
                }

                String[] parts = message.split(" ");
                int row = Integer.parseInt(parts[0]);
                int col = Integer.parseInt(parts[1]);

                // Determine the opponent
                ClientHandler opponent = (this == clients.get(0)) ? clients.get(1) : clients.get(0);

                boolean hit = opponent.registerAttack(row, col);

                // Send the result to both clients
                synchronized (clients) {
                    for (ClientHandler client : clients) {
                        if (client == this) {
                            client.out.println(hit ? "HIT " + row + " " + col : "MISS " + row + " " + col);
                        } else {
                            client.out.println(hit ? "OPPONENT_HIT " + row + " " + col : "OPPONENT_MISS " + row + " " + col);
                        }

                        if (opponent.hasLost()) {
                            client.out.println(client == this ? "WIN" : "LOSE");
                        }
                    }
                }

                // Switch turns
                currentPlayerIndex = (currentPlayerIndex + 1) % 2;
                clients.get(currentPlayerIndex).out.println("YOUR_TURN");
                clients.get((currentPlayerIndex + 1) % 2).out.println("OPPONENT_TURN");
            }
        }

        private boolean registerAttack(int row, int col) {
            // Register the attack on the opponent's board ONLY if it's a ship
            if (ships[row][col] == 1) {
                ships[row][col] = 2; // Mark as hit
                return true;
            }
            return false;
        }


        private boolean hasLost() {
            // Check if all ships have been sunk
            for (int[] row : ships) {
                for (int cell : row) {
                    if (cell == 1) { // Check if any ship parts are left
                        return false;
                    }
                }
            }
            return true;
        }

        private boolean allClientsReady() {
            // Check if both clients are ready to start the game
            for (ClientHandler client : clients) {
                if (!client.ready) {
                    return false;
                }
            }
            return true;
        }
    }
}