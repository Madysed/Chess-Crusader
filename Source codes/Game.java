import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class Game extends JFrame implements ActionListener, Board, Serializable {
    JTextArea chat, logs;
    JButton sendButton, saveExit;
    JTextField sendField;
    JLayeredPane layeredPane;
    JPanel downRightPanel, upRightPanel;
    JLabel turn;
    ObjectOutputStream output;
    ObjectInputStream input;
    Socket socket;
    String gameMode, oppUsername;
    Player player;
    int turnCounter = 0, selectedX, selectedY;
    char[] letter = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h'};
    boolean isSelected = false;

    Game(Socket socket, ObjectOutputStream output, ObjectInputStream input, String gameMode, String username) {

        this.socket = socket;
        this.output = output;
        this.input = input;
        this.gameMode = gameMode;

        Border blackborder = BorderFactory.createLineBorder(Color.black, 1);

        getContentPane().setPreferredSize(new Dimension(960, 640));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        if (username.startsWith("Guest_")) {
            username = username.substring(6);
            setTitle("Chess Crusader - [" + username + "]" + " Muslim");
            player = new Player("muslim", username);
        } else {
            username = username.substring(5);
            setTitle("Chess Crusader - [" + username + "]" + " Christian");
            player = new Player("christian", username);
        }
        setResizable(false);
        setLayout(new BorderLayout());
        pack();

        JPanel rightSide = new JPanel(new GridLayout(2, 1));
        rightSide.setPreferredSize(new Dimension(320, 640));
        upRightPanel = new JPanel();
        upRightPanel.setLayout(new BoxLayout(upRightPanel, BoxLayout.Y_AXIS));
        downRightPanel = new JPanel(new BorderLayout());
        layeredPane = new JLayeredPane();
        layeredPane.setSize(new Dimension(640, 640));
        layeredPane.setOpaque(true);
        layeredPane.setVisible(true);

        turn = new JLabel();
        JLabel timer = new JLabel();
        logs = new JTextArea();
        logs.setEditable(false);
        turn.setPreferredSize(new Dimension(320, 53));
        turn.setMaximumSize(new Dimension(320, 53));
        timer.setPreferredSize(new Dimension(320, 53));
        timer.setMaximumSize(new Dimension(320, 53));
        logs.setPreferredSize(new Dimension(320, 160));
        logs.setMaximumSize(new Dimension(320, 160));
        turn.setBorder(blackborder);
        timer.setBorder(blackborder);
        logs.setBorder(blackborder);
        JScrollPane logsScrollPane = new JScrollPane(logs);
        logsScrollPane.setVerticalScrollBarPolicy(logsScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        logsScrollPane.setHorizontalScrollBarPolicy(logsScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        saveExit = new JButton("Save & Exit");
        saveExit.setPreferredSize(new Dimension(320, 53));
        saveExit.setMaximumSize(new Dimension(320, 53));
        saveExit.setFocusable(false);
        saveExit.setBackground(Color.cyan);
        saveExit.setForeground(Color.black);
        saveExit.setBorder(blackborder);
        logs.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        turn.setFont(new Font(Font.SERIF, Font.PLAIN, 26));
        timer.setFont(new Font(Font.SERIF, Font.PLAIN, 26));
        saveExit.setFont(new Font(Font.SERIF, Font.PLAIN, 26));
        saveExit.addActionListener(this);

        upRightPanel.add(saveExit);
        upRightPanel.add(turn);
        upRightPanel.add(timer);
        upRightPanel.add(logsScrollPane);

        JPanel sendArea = new JPanel(new BorderLayout());
        sendButton = new JButton("Send");
        sendButton.setFocusable(false);
        sendField = new JTextField();
        sendButton.addActionListener(this);
        sendArea.add(sendField, BorderLayout.CENTER);
        sendArea.add(sendButton, BorderLayout.EAST);
        chat = new JTextArea();
        chat.setFont(new Font(Font.SERIF, Font.PLAIN, 20));
        JScrollPane chatScrollPane = new JScrollPane(chat);
        chatScrollPane.setVerticalScrollBarPolicy(chatScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chat.setLineWrap(true);
        chat.setEditable(false);
        downRightPanel.add(chatScrollPane, BorderLayout.CENTER);
        downRightPanel.add(sendArea, BorderLayout.SOUTH);
        downRightPanel.setBorder(blackborder);
        rightSide.add(upRightPanel);
        rightSide.add(downRightPanel);

        if (gameMode.equals("Normal"))
            gameBoardGenerator();
        else if (gameMode.equals("Load"))
            Load();

        new Thread(() -> {
            while (socket.isConnected()) {
                updateGUI();
                if (turnCounter % 2 == 0)
                    turn.setText("          Turn : Christian");
                else
                    turn.setText("           Turn : Muslim");
                String request;
                try {
                    request = input.readUTF();
                    switch (request) {
                        case "Message":
                            getMessage();
                            break;
                        case "Turn":
                            turnCounter++;
                            getNewBoard();
                            break;
                        case "Exit":
                            System.exit(0);
                            break;
                        case "Get_Username":
                            output.writeUTF(player.getUsername());
                            output.flush();
                            break;
                        case "Update_Logs":
                            logs.setText(input.readUTF());
                            upRightPanel.repaint();
                            break;
                        case "Game_Status":
                            dispose();
                            break;
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();

        add(layeredPane);
        add(rightSide, BorderLayout.EAST);
        setVisible(true);

    }

    private void printGUIBoard() {

        if (turnCounter % 2 == 0)
            turn.setText("          Turn : Christian");
        else
            turn.setText("           Turn : Muslim");
        layeredPane.removeAll();
        ImageIcon imageIcon = new ImageIcon("assets/" + "mainBoard.png");
        imageIcon.setImage(imageIcon.getImage().getScaledInstance(640, 640, Image.SCALE_SMOOTH));
        JLabel tileLabel = new JLabel(imageIcon);
        tileLabel.setBounds(0, 0, 640, 640);
        layeredPane.add(tileLabel, Integer.valueOf(1));
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (gameTiles[i][j] == null)
                    continue;
                String type = gameTiles[i][j].getType();
                if (gameTiles[i][j] instanceof Archer) {
                    imageIcon = new ImageIcon("assets/" + type + "_archer_Chess Crusader.png");
                    imageIcon.setImage(imageIcon.getImage().getScaledInstance(80, 80, 0));
                } else if (gameTiles[i][j] instanceof Knight) {
                    imageIcon = new ImageIcon("assets/" + type + "_knight_Chess Crusader.png");
                    imageIcon.setImage(imageIcon.getImage().getScaledInstance(70, 70, 0));
                } else if (gameTiles[i][j] instanceof Catapult) {
                    imageIcon = new ImageIcon("assets/" + type + "_catapult_Chess Crusader.png");
                    imageIcon.setImage(imageIcon.getImage().getScaledInstance(80, 80, 0));
                } else if (gameTiles[i][j] instanceof Castle) {
                    imageIcon = new ImageIcon("assets/" + type + "_castle_Chess Crusader.png");
                    imageIcon.setImage(imageIcon.getImage().getScaledInstance(65, 65, 0));
                } else if (gameTiles[i][j] instanceof Soldier) {
                    imageIcon = new ImageIcon("assets/" + type + "_soldier_Chess Crusader.png");
                    imageIcon.setImage(imageIcon.getImage().getScaledInstance(80, 80, 0));
                }
                tileLabel = new JLabel(imageIcon);
                tileLabel.setBounds(j * 80, i * 80, 80, 80);

                layeredPane.add(tileLabel, Integer.valueOf(100));
            }
        }
        layeredPane.repaint();
        powerCalculation();

    }

    private void gameBoardGenerator() {

        gameTiles[0][0] = new Archer(2, 0, 1, 80, 80, 0, "christian");
        gameTiles[0][1] = new Archer(2, 0, 1, 160, 80, 0, "christian");
        gameTiles[0][2] = new Knight(1, 0, 2, 240, 80, 0, "christian");
        gameTiles[0][3] = new Castle(1, 0, 1, 320, 80, 1, "christian");
        gameTiles[0][4] = new Catapult(0, 0, 1, 400, 80, -2, "christian");
        gameTiles[0][5] = new Knight(1, 0, 2, 480, 80, 0, "christian");
        gameTiles[0][6] = new Archer(2, 0, 1, 560, 80, 0, "christian");
        gameTiles[0][7] = new Archer(2, 0, 1, 640, 80, 0, "christian");
        for (int i = 0; i < 8; i++)
            gameTiles[1][i] = new Soldier(1, 0, 1, 80 * i, 160, 1, "christian");

        gameTiles[7][0] = new Archer(2, 0, 1, 80, 560, 0, "muslim");
        gameTiles[7][1] = new Archer(2, 0, 1, 160, 560, 0, "muslim");
        gameTiles[7][2] = new Knight(1, 0, 2, 240, 560, 0, "muslim");
        gameTiles[7][3] = new Castle(1, 0, 1, 320, 560, 1, "muslim");
        gameTiles[7][4] = new Catapult(0, 0, 1, 400, 560, -2, "muslim");
        gameTiles[7][5] = new Knight(1, 0, 2, 480, 560, 0, "muslim");
        gameTiles[7][6] = new Archer(2, 0, 1, 560, 560, 0, "muslim");
        gameTiles[7][7] = new Archer(2, 0, 1, 640, 560, 0, "muslim");
        for (int i = 0; i < 8; i++)
            gameTiles[6][i] = new Soldier(1, 0, 1, 80 * i, 480, 1, "muslim");

    }

    private void printBoard() {

        ImageIcon imageIcon = null;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (gameTiles[i][j] == null)
                    continue;
                String type = gameTiles[i][j].getType();
                if (gameTiles[i][j] instanceof Archer) {
                    imageIcon = new ImageIcon("assets/" + type + "_archer_Chess Crusader.png");
                    imageIcon.setImage(imageIcon.getImage().getScaledInstance(80, 80, 0));
                } else if (gameTiles[i][j] instanceof Knight) {
                    imageIcon = new ImageIcon("assets/" + type + "_knight_Chess Crusader.png");
                    imageIcon.setImage(imageIcon.getImage().getScaledInstance(70, 70, 0));
                } else if (gameTiles[i][j] instanceof Catapult) {
                    imageIcon = new ImageIcon("assets/" + type + "_catapult_Chess Crusader.png");
                    imageIcon.setImage(imageIcon.getImage().getScaledInstance(80, 80, 0));
                } else if (gameTiles[i][j] instanceof Castle) {
                    imageIcon = new ImageIcon("assets/" + type + "_castle_Chess Crusader.png");
                    imageIcon.setImage(imageIcon.getImage().getScaledInstance(65, 65, 0));
                } else if (gameTiles[i][j] instanceof Soldier) {
                    imageIcon = new ImageIcon("assets/" + type + "_soldier_Chess Crusader.png");
                    imageIcon.setImage(imageIcon.getImage().getScaledInstance(80, 80, 0));
                }
                JLabel tileLabel = new JLabel(imageIcon);
                tileLabel.setBounds(j * 80, i * 80, 80, 80);

                if ((player.getType().equals("christian") && turnCounter % 2 == 0) || (player.getType().equals("muslim") && turnCounter % 2 == 1)) {
                    int x = i;
                    int y = j;
                    tileLabel.addMouseListener(new MouseAdapter() {
                        public void mouseClicked(MouseEvent evt) {
                            if (evt.getButton() == MouseEvent.BUTTON1) {
                                selectedX = x;
                                selectedY = y;
                                possibleMoves(x, y);
                            }
                        }
                    });
                }

                layeredPane.add(tileLabel, Integer.valueOf(100));
            }
        }
        layeredPane.repaint();

    }

    private void powerCalculation() {
        ImageIcon imageIcon;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                if (gameTiles[i][j] == null)
                    continue;
                gameTiles[i][j].setPower(0);
                for (int a = i - 1; a <= i + 1; a++) {
                    for (int b = j - 1; b <= j + 1; b++) {
                        if (a == i && b == j)
                            continue;
                        if (a < 0 || a >= 8 || b < 0 || b >= 8)
                            continue;
                        if (gameTiles[a][b] == null)
                            continue;
                        if (gameTiles[a][b] instanceof Soldier && gameTiles[i][j].getType().equals(gameTiles[a][b].getType())) {
                            if (gameTiles[i][j] instanceof Soldier) {
                                gameTiles[i][j].setPower(gameTiles[i][j].getPower() + gameTiles[a][b].getBoost());
                            }
                        } else if (gameTiles[a][b] instanceof Catapult) {
                            if (!gameTiles[i][j].getType().equals(gameTiles[a][b].getType())) {
                                gameTiles[i][j].setPower(gameTiles[i][j].getPower() - 2);
                            }
                        } else if (gameTiles[i][j].getType().equals(gameTiles[a][b].getType()))
                            gameTiles[i][j].setPower(gameTiles[i][j].getPower() + gameTiles[a][b].getBoost());
                    }
                }
                if (gameTiles[i][j].getMainPower() + gameTiles[i][j].getPower() < 0)
                    gameTiles[i][j].setPower(0);
                else
                    gameTiles[i][j].setPower(gameTiles[i][j].getMainPower() + gameTiles[i][j].getPower());
                imageIcon = new ImageIcon("assets/" + gameTiles[i][j].getPower() + "_power_Chess Crusader.png");
                imageIcon.setImage(imageIcon.getImage().getScaledInstance(80, 80, 0));
                JLabel tileLabel = new JLabel(imageIcon);
                tileLabel.setBounds(j * 80, i * 80, 80, 80);
                layeredPane.add(tileLabel, Integer.valueOf(40));
            }
        }
    }

    private void possibleMoves(int x, int y) {

        ImageIcon imageIcon;
        if (isSelected) {
            for (Component component : layeredPane.getComponentsInLayer(110))
                layeredPane.remove(component);
            for (Component component : layeredPane.getComponentsInLayer(50))
                layeredPane.remove(component);
            isSelected = false;
        } else {
            if (gameTiles[x][y].getType().equals(player.getType())) {
                imageIcon = new ImageIcon("assets/" + "selected_Chess Crusader.png");
                imageIcon.setImage(imageIcon.getImage().getScaledInstance(80, 80, 0));
                JLabel tileLabel = new JLabel(imageIcon);
                tileLabel.setBounds(y * 80, x * 80, 80, 80);
                layeredPane.add(tileLabel, Integer.valueOf(50));

                if (gameTiles[x][y] instanceof Soldier)
                    highlightSoldierMoves(x, y);
                else
                    highlightMoves(x, y);
                isSelected = true;
            }
        }

        layeredPane.repaint();
    }

    private void highlightMoves(int x, int y) {

        tryHighlightMove(x + 1, y);
        tryHighlightMove(x + 1, y + 1);
        tryHighlightMove(x + 1, y - 1);
        tryHighlightMove(x, y + 1);
        tryHighlightMove(x, y - 1);
        tryHighlightMove(x - 1, y);
        tryHighlightMove(x - 1, y + 1);
        tryHighlightMove(x - 1, y - 1);
        if (gameTiles[selectedX][selectedY] instanceof Knight) {
            tryHighlightMove(x + 2, y);
            tryHighlightMove(x + 2, y + 2);
            tryHighlightMove(x + 2, y - 2);
            tryHighlightMove(x, y + 2);
            tryHighlightMove(x, y - 2);
            tryHighlightMove(x - 2, y);
            tryHighlightMove(x - 2, y + 2);
            tryHighlightMove(x - 2, y - 2);
        }

    }

    private void highlightSoldierMoves(int x, int y) {
        if (player.getType().equals("christian") && x + 1 <= 4) {
            tryHighlightMove(x + 1, y);
            tryHighlightMove(x + 1, y + 1);
            tryHighlightMove(x + 1, y - 1);
        } else if (player.getType().equals("muslim") && x - 1 >= 3) {
            tryHighlightMove(x - 1, y);
            tryHighlightMove(x - 1, y + 1);
            tryHighlightMove(x - 1, y - 1);
        }
    }

    private void tryHighlightMove(int x, int y) {
        if (gameTiles[selectedX][selectedY] instanceof Castle) {
            if (isWithinBounds(x, y) && (gameTiles[x][y] == null)) {
                ImageIcon imageIcon = new ImageIcon("assets/" + "moves_Chess Crusader.png");
                imageIcon.setImage(imageIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH));
                JLabel tileLabel = new JLabel(imageIcon);
                tileLabel.setBounds(y * 80, x * 80, 80, 80);
                layeredPane.add(tileLabel, Integer.valueOf(110));
                tileLabel.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent evt) {
                        if (evt.getButton() == MouseEvent.BUTTON1) {
                            moveTo(x, y);
                        }
                    }
                });
            }
        } else if (isWithinBounds(x, y) && ((gameTiles[x][y] == null) || (!gameTiles[x][y].getType().equals(player.getType()) && gameTiles[x][y].getPower() < gameTiles[selectedX][selectedY].getPower()))) {
            ImageIcon imageIcon = new ImageIcon("assets/" + "moves_Chess Crusader.png");
            imageIcon.setImage(imageIcon.getImage().getScaledInstance(25, 25, Image.SCALE_SMOOTH));
            JLabel tileLabel = new JLabel(imageIcon);
            tileLabel.setBounds(y * 80, x * 80, 80, 80);
            layeredPane.add(tileLabel, Integer.valueOf(110));
            tileLabel.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    if (evt.getButton() == MouseEvent.BUTTON1) {
                        moveTo(x, y);
                    }
                }
            });
        }
    }

    private boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < 8 && y >= 0 && y < 8;
    }

    private void moveTo(int x, int y) {

        if (gameTiles[x][y] instanceof Castle && (!gameTiles[x][y].getType().equals(player.getType()) && gameTiles[x][y].getPower() < gameTiles[selectedX][selectedY].getPower())) {

            try {
                output.writeUTF("Game_Status");
                output.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            JOptionPane.showMessageDialog(null,player.getType() + " won!", "End", JOptionPane.INFORMATION_MESSAGE);
            deleteFiles();
            dispose();

        } else if (gameTiles[x][y] == null || (!gameTiles[x][y].getType().equals(player.getType()) && gameTiles[x][y].getPower() < gameTiles[selectedX][selectedY].getPower())) {
            gameTiles[x][y] = gameTiles[selectedX][selectedY];
            gameTiles[selectedX][selectedY] = null;
            turnCounter++;
            try {
                output.writeUTF("Turn");
                output.flush();
                for (int i = 0; i < 8; i++) {
                    for (int j = 0; j < 8; j++) {
                        output.writeObject(gameTiles[i][j]);
                        output.flush();
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            powerCalculation();
            logs.setText(logs.getText() + player.getType() + " moved from " + (8 - selectedX) + letter[selectedY] + " to " + (8 - x) + letter[y] + " and his power is " + gameTiles[x][y].getPower() + "\n");
            upRightPanel.repaint();
            try {
                output.writeUTF("Update_Logs");
                output.flush();
                output.writeUTF(logs.getText());
                output.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            printGUIBoard();
        }

    }

    private void getMessage() {

        String message;
        try {
            message = input.readUTF();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (!message.isBlank()) {
            chat.setText(chat.getText() + message + "\n");
            downRightPanel.repaint();
        }

    }

    private void getNewBoard() {

        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++)
                gameTiles[i][j] = null;
        for (int i = 0; i < 8; i++) {
            for (int j = 0; j < 8; j++) {
                try {
                    gameTiles[i][j] = (Piece) input.readObject();
                } catch (IOException | ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        updateGUI();

    }

    private void updateGUI() {

        layeredPane.removeAll();
        ImageIcon imageIcon = new ImageIcon("assets/" + "mainBoard.png");
        imageIcon.setImage(imageIcon.getImage().getScaledInstance(640, 640, Image.SCALE_SMOOTH));
        JLabel tileLabel = new JLabel(imageIcon);
        tileLabel.setBounds(0, 0, 640, 640);
        layeredPane.add(tileLabel, Integer.valueOf(1));
        printBoard();
        powerCalculation();
    }

    private void Save() {

        File board = new File("GameBoard.txt");
        File info = new File("Information.txt");
        File log = new File("Logs.txt");
        File chats = new File("Chat.txt");
        board.delete();
        info.delete();
        log.delete();
        chats.delete();
        try {
            board.createNewFile();
            FileOutputStream fileOutputStream = new FileOutputStream(board);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            for (int i = 0; i < 8; i++)
                for (int j = 0; j < 8; j++)
                    objectOutputStream.writeObject(gameTiles[i][j]);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            output.writeUTF("Get_Username");
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            info.createNewFile();
            FileWriter fileWriter = new FileWriter(info);
            if (player.getType().equals("christian")) {
                fileWriter.write(player.getUsername() + "\n");
                fileWriter.write(oppUsername + "\n");
            } else {
                fileWriter.write(oppUsername + "\n");
                fileWriter.write(player.getUsername() + "\n");
            }
            fileWriter.write(turnCounter + "\n");
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            log.createNewFile();
            FileWriter fileWriter = new FileWriter(log);
            fileWriter.write(logs.getText());
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try {
            chats.createNewFile();
            FileWriter fileWriter = new FileWriter(chats);
            fileWriter.write(chat.getText());
            fileWriter.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.exit(0);

    }

    private void Load() {

        File board = new File("GameBoard.txt");
        File info = new File("Information.txt");
        File log = new File("Logs.txt");
        File chats = new File("Chat.txt");
        if (board.exists()) {
            try {
                FileInputStream fileInputStream = new FileInputStream(board);
                ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);
                for (int i = 0; i < 8; i++)
                    for (int j = 0; j < 8; j++)
                        gameTiles[i][j] = (Piece) objectInputStream.readObject();
                objectInputStream.close();
                fileInputStream.close();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        if (info.exists()) {
            try {
                Scanner fileReader = new Scanner(info);
                String temp = fileReader.nextLine();
                if (player.getType().equals("christian")) {
                    player.setUsername(temp);
                    oppUsername = fileReader.nextLine();
                    setTitle("Chess Crusader - [" + player.getUsername() + "]" + " Christian");
                } else if (player.getType().equals("muslim")) {
                    temp = fileReader.nextLine();
                    player.setUsername(temp);
                    setTitle("Chess Crusader - [" + player.getUsername() + "]" + " Muslim");
                }
                turnCounter = Integer.parseInt(fileReader.nextLine());
                fileReader.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        if (log.exists()) {
            try {
                Scanner fileReader = new Scanner(log);
                while (fileReader.hasNextLine())
                    logs.setText(logs.getText() + fileReader.nextLine() + "\n");
                fileReader.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        if (chats.exists()) {
            try {
                Scanner fileReader = new Scanner(chats);
                while (fileReader.hasNextLine())
                    chat.setText(chat.getText() + fileReader.nextLine() + "\n");
                fileReader.close();
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                output.writeUTF("Exit");
                output.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            JOptionPane.showMessageDialog(null, "There is no save!", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }

    }

    private void deleteFiles () {

        File board = new File("GameBoard.txt");
        File info = new File("Information.txt");
        File log = new File("Logs.txt");
        File chats = new File("Chat.txt");
        board.delete();
        info.delete();
        log.delete();
        chats.delete();

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == sendButton) {
            try {
                String message = sendField.getText().trim();
                if (!message.isBlank()) {
                    output.writeUTF("Message");
                    output.flush();
                    output.writeUTF(player.getUsername() + ": " + message);
                    output.flush();
                    chat.setText(chat.getText() + player.getUsername() + ": " + message + "\n");
                    sendField.setText("");
                    downRightPanel.repaint();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == saveExit) {
            try {
                output.writeUTF("Exit");
                output.flush();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            Save();
        }

    }


}
