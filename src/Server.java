
import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server {

    List<Client> clients = new ArrayList<>();
    List<Game> games = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        new Server().run();

    }

    public void run() throws IOException {

        ServerSocket server = new ServerSocket(2006);
        System.out.println("Server run ...");

        while (true) {

            Socket socket = server.accept();

            Client thread = new Client(socket);

            clients.add(thread);
            System.out.println("New client id: " + clients.indexOf(thread));

        }
    }

    class Game extends Thread {

        String gameName;
        Client playerOne;
        Client playerTwo;
        Snake snake;
        int playerOneSnakePosX = 9;
        int playerOneSnakePosY = 5;
        String  playerOneSnakeDir = "dup";
        int playerTwoSnakePosX = 9;
        int playerTwoSnakePosY = 5;
        String  playerTwoSnakeDir = "dup";
        int[] returnedPos;

        public Game(String name, Client player) {
            gameName = name;
            playerOne = player;
            snake = new Snake();
            returnedPos = new int[2];
            
            this.startGame();
        }

        public void joinGame(Client client) {
            playerTwo = client;
            //this.startGame();

        }

        public void startGame() {
            System.out.println("Sukces" + gameName);
            this.start();
        }

        @Override
        public void run() {
            try {
                while (true) {
                    returnedPos = snake.getNextPos(playerOneSnakePosX, playerOneSnakePosY, playerOneSnakeDir);
                    playerOneSnakePosX = returnedPos[0];
                    playerOneSnakePosY = returnedPos[1];
                    playerOne.s.sendHeadPos(playerOneSnakePosX, playerOneSnakePosY);
                    Thread.sleep(50);
                }
            } catch (InterruptedException ex) {

            }
        }

    }

    class Snake {

        public int[] getNextPos(int snakeHeadX, int snakeHeadY, String dir) {

            if (snakeHeadX < 99 && snakeHeadY < 99 && snakeHeadX > 0 && snakeHeadY > 0) {

                switch (dir) {
                    case "dup":
                        snakeHeadY--;
                        break;
                    case "ddown":
                        snakeHeadY++;
                        break;
                    case "dleft":
                        snakeHeadX--;
                        break;
                    case "dright":
                        snakeHeadX++;
                        break;
                    default:
                        snakeHeadX++;

                }

            } else if (snakeHeadX >= 99) {
                snakeHeadX = 1;
            } else if (snakeHeadX <= 0) {
                snakeHeadX = 98;
            } else if (snakeHeadY <= 0) {
                snakeHeadY = 98;
            } else if (snakeHeadY >= 99) {
                snakeHeadY = 1;
            } else {
                snakeHeadX = 1;
                snakeHeadY = 1;
            }

            return new int[]{snakeHeadX, snakeHeadY};
        }
    }

    class Client extends Thread {

        Client client;
        Socket socket;
        InputStream in;
        OutputStream out;
        BufferedReader fromKeyboard;

        Reading r;
        Sending s;
        // Snake sn;

        List<Fields> fields = new ArrayList();

        String dir = "";

        public class Fields {

            int x;

            int y;

            public Fields(int x, int y) {
                this.x = x;
                this.y = y;
            }
        }

        Client(Socket socket) throws IOException {

            this.socket = socket;
            this.in = socket.getInputStream();
            this.out = socket.getOutputStream();
            client = this;

            start();

        }

        @Override
        public void run() {

            r = new Reading();
            r.start();
            s = new Sending();
            s.start();
//            sn = new Snake();
//            sn.start();

        }

        class Reading extends Thread {

            @Override
            public void run() {
                try {
                    while (true) {

                        int k = 0;
                        StringBuilder sb = new StringBuilder();

                        while ((k = in.read()) != -1 && k != '\n') {
                            sb.append((char) k);
                        }

                        String data = sb.toString().trim();

                        if (data.charAt(0) == 'c') {
                            String gName = data.substring(1, data.length());
                            System.out.println(gName);
                            Game game = new Game(gName, client);
                            games.add(game);

                        }
                        if (data.charAt(0) == 'j') {
                            String gName = data.substring(1, data.length());
                            boolean gameExists = false;
                            for (int i = 0; i < games.size(); i++) {
                                if (games.get(i).gameName.equals(gName)) {
                                    gameExists = true;
                                    games.get(i).joinGame(client);
                                }
                            }
                            if (!gameExists) {
                                System.out.println("Gra o podanej nazwie nie istnieje");
                            }

                        }
                        if (data.charAt(0) == 'd') {
                            //sn.dir = data;
                        }

                        if (data.toLowerCase().equals("exit")) {

                            in.close();
                            out.close();
                            socket.close();
                            System.exit(0);
                        }

                    }
                } catch (IOException ex) {
                    System.err.println();
                }
            }
        }

        class Sending extends Thread {

            StringBuilder posToSend = new StringBuilder();

            @Override
            public void run() {

                try {

                    initSnake();
                    Thread.sleep(50);

                } catch (InterruptedException ex) {

                }
            }

            public void sendHeadPos(int snakeHeadX, int snakeHeadY) {

                try {
                    String x = Integer.toString(snakeHeadX);
                    String y = Integer.toString(snakeHeadY);
                    posToSend = new StringBuilder();
                    if (x.length() == 1) {
                        posToSend.append(Integer.toString(0));
                    }
                    posToSend.append(x);
                    if (y.length() == 1) {
                        posToSend.append(Integer.toString(0));
                    }
                    posToSend.append(y);
                    String xy = posToSend.toString();
                    out.write(xy.getBytes());
                    out.write("\r\n".getBytes());
                } catch (IOException ex) {

                }

            }

            void initSnake() {
                try {
                    //String[] snake = {"i","1", "5", "2", "5", "3", "5", "4", "5", "5", "5"};
                    String snake = "i152535455565758595";

                    out.write(snake.getBytes());
                    out.write("\r\n".getBytes());

                } catch (IOException ex) {
                    System.err.println(ex);

                }
            }
        }
    }
}
