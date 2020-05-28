package snakegameserver;


import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SnakeGameServer {

    List<Client> clients = new ArrayList<>();
    List<Game> games = new ArrayList<>();

    public static void main(String[] args) throws IOException {

        new SnakeGameServer().run();

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
        Random random;
        String playerOneNick = "", playerTwoNick = "";
        int[] playerOneSnakePosXY = {9, 5};
        String playerOneSnakeDir = "dright";
        int[] playerTwoSnakePosXY = {90, 95};
        String playerTwoSnakeDir = "dleft";
        int[] fruit = new int[2];

        public void setPlayerOneSnakeDir(String playerOneSnakeDir) {
            this.playerOneSnakeDir = playerOneSnakeDir;
        }

        public void setPlayerTwoSnakeDir(String playerTwoSnakeDir) {
            this.playerTwoSnakeDir = playerTwoSnakeDir;
        }

        public void setPlayerOneNick(String playerOneNick) {
            this.playerOneNick = playerOneNick;
        }

        public void setPlayerTwoNick(String playerTwoNick) {
            this.playerTwoNick = playerTwoNick;
        }
        

        public Game(String name, Client player) {
            gameName = name;
            playerOne = player;
            snake = new Snake();
            playerOne.s.initSnake();
            random = new Random();
        }

        public Game joinGame(Client client) {
            playerTwo = client;
            playerTwo.s.initSnake();
            return this;
        }

        public void startGame() {
            System.out.println("Sukces" + gameName);
            playerOne.s.sendPlayersNicks(playerOneNick, playerTwoNick);
            playerTwo.s.sendPlayersNicks(playerOneNick, playerTwoNick);
            this.drawAndSendFruit();
            this.start();
        }

        @Override
        public void run() {
            try {
                Thread.sleep(500);
                while (true) {
                    playerOneSnakePosXY = snake.getNextPos(playerOneSnakePosXY, playerOneSnakeDir);
                    playerOne.s.sendHeadPos(playerOneSnakePosXY[0], playerOneSnakePosXY[1], "o");
                    playerTwo.s.sendHeadPos(playerOneSnakePosXY[0], playerOneSnakePosXY[1], "o");
                    Thread.sleep(50);
                    playerTwoSnakePosXY = snake.getNextPos(playerTwoSnakePosXY, playerTwoSnakeDir);
                    playerOne.s.sendHeadPos(playerTwoSnakePosXY[0], playerTwoSnakePosXY[1], "t");
                    playerTwo.s.sendHeadPos(playerTwoSnakePosXY[0], playerTwoSnakePosXY[1], "t");
                    if(playerOneSnakePosXY[0] == fruit[0] && playerOneSnakePosXY[1] == fruit[1]) {
                        playerOne.s.sendPoint("o");
                        playerTwo.s.sendPoint("o");
                        this.drawAndSendFruit();
                    }
                    else if(playerTwoSnakePosXY[0] == fruit[0] && playerTwoSnakePosXY[1] == fruit[1]) {
                        playerOne.s.sendPoint("t");
                        playerTwo.s.sendPoint("t");
                        this.drawAndSendFruit();
                    }

                }
            } catch (InterruptedException ex) {

            }
        }
        public void drawAndSendFruit() {
            this.drawFruitPos(fruit);
            playerOne.s.sendFruitPos(fruit);
            playerTwo.s.sendFruitPos(fruit);
        }
        public int[] drawFruitPos(int[] fruit) {
            fruit[0] = random.nextInt(100);
            fruit[1] = random.nextInt(100);
            return fruit;
        }

    }

    class Snake {

        public int[] getNextPos(int[] playerPosXY, String dir) {
            int snakeHeadX = playerPosXY[0];
            int snakeHeadY = playerPosXY[1];

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
        }

        class Reading extends Thread {

            Game game;
            String player = "";
            String playerNick = "";
            boolean gameExists = false;
            boolean gameNameExists = false;

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
                            gameNameExists = false;
                            for (int i = 0; i < games.size(); i++) {
                                if (games.get(i).gameName.equals(gName)) {
                                    gameNameExists = true;
                                }
                            }
                            if(!gameNameExists) {
                                gameExists = true;
                                game = new Game(gName, client);
                                games.add(game);
                                player = "one";
                            }
                            else {
                                s.sendMessage("me");
                            }
                        }
                        if (data.charAt(0) == 'a' && gameExists) {
                            playerNick = data.substring(1, data.length());
                            game.setPlayerOneNick(playerNick);
                            // mo = ok, start game
                            s.sendMessage("mo");
                        }
                        if (data.charAt(0) == 'j') {
                            String gName = data.substring(1, data.length());
                            for (int i = 0; i < games.size(); i++) {
                                if (games.get(i).gameName.equals(gName)) {
                                    s.sendMessage("mo");
                                    gameExists = true;
                                    game = games.get(i).joinGame(client);
                                    player = "two";
                                }
                            }
                            if (!gameExists) {
                                s.sendMessage("mn");
                            }
                        }
                        if (data.charAt(0) == 'b' && gameExists) {
                            playerNick = data.substring(1, data.length());
                            game.setPlayerTwoNick(playerNick);
                            game.startGame();
                        }

                        if (data.charAt(0) == 'd') {

                            switch (player) {
                                case "one":
                                    game.setPlayerOneSnakeDir(data);
                                    break;
                                case "two":
                                    game.setPlayerTwoSnakeDir(data);
                                    break;
                            }
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

        class Sending {
            
            public void sendPlayersNicks(String playerOneNick, String playerTwoNick) {
                try {
                    String nickToSend = "a" + playerOneNick;
                    out.write(nickToSend.getBytes());
                    out.write("\r\n".getBytes());
                    nickToSend = "b" + playerTwoNick;
                    out.write(nickToSend.getBytes());
                    out.write("\r\n".getBytes());
                    
                     } catch (IOException ex) {

                }
            }

            public void sendHeadPos(int snakeHeadX, int snakeHeadY, String playerCode) {

                try {
                    String x = Integer.toString(snakeHeadX);
                    String y = Integer.toString(snakeHeadY);
                    StringBuilder posToSend = new StringBuilder();
                    posToSend.append(playerCode);
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

            public void sendFruitPos(int[] fruit) {
                try {
                    String x = Integer.toString(fruit[0]);
                    String y = Integer.toString(fruit[1]);
                    StringBuilder fruitToSend = new StringBuilder();
                    fruitToSend.append("f");
                    if (x.length() == 1) {
                        fruitToSend.append(Integer.toString(0));
                    }
                    fruitToSend.append(x);
                    if (y.length() == 1) {
                        fruitToSend.append(Integer.toString(0));
                    }
                    fruitToSend.append(y);

                    String xy = fruitToSend.toString();
                    out.write(xy.getBytes());
                    out.write("\r\n".getBytes());

                } catch (IOException ex) {

                }

            }

            void initSnake() {
                try {
                    //String[] snake = {"i","1", "5", "2", "5", "3", "5", "4", "5", "5", "5"};
                    StringBuilder snakeBuilder = new StringBuilder();
                    snakeBuilder.append("i");
                    snakeBuilder.append("0005");
                    snakeBuilder.append("9995");
                    snakeBuilder.append("0105");
                    snakeBuilder.append("9895");
                    snakeBuilder.append("0205");
                    snakeBuilder.append("9795");
                    snakeBuilder.append("0305");
                    snakeBuilder.append("9695");
                    snakeBuilder.append("0405");
                    snakeBuilder.append("9595");
                    snakeBuilder.append("0505");
                    snakeBuilder.append("9495");
                    snakeBuilder.append("0605");
                    snakeBuilder.append("9395");
                    snakeBuilder.append("0705");
                    snakeBuilder.append("9295");
                    snakeBuilder.append("0805");
                    snakeBuilder.append("9195");
                    snakeBuilder.append("0905");
                    snakeBuilder.append("9095");
                    String snake = snakeBuilder.toString();

                    //String snake = "i00059995010598950205979503059695040595950505949606059395070592950805919509059095";
                    out.write(snake.getBytes());
                    out.write("\r\n".getBytes());

                } catch (IOException ex) {
                    System.err.println(ex);

                }
            }
            
            public void sendPoint(String player) {
                try {
                StringBuilder pointBuilder = new StringBuilder();
                pointBuilder.append("p");
                pointBuilder.append(player);
                String pointToSend = pointBuilder.toString();
                out.write(pointToSend.getBytes());
                out.write("\r\n".getBytes());
                
                 } catch (IOException ex) {
                    System.err.println(ex);

                }
                
            }
             public void sendMessage(String messageCode) {
                try {
                out.write(messageCode.getBytes());
                out.write("\r\n".getBytes());
                
                 } catch (IOException ex) {
                    System.err.println(ex);

                }
            }
        }
    }
}
