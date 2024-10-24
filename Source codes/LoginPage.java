import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;

public class LoginPage extends JFrame implements ActionListener, Serializable {
    String gameMode;
    JTextField username;
    JButton normalMode, loadGame, friendlyMode, speedMode, join;
    LoginPage() {

        setDefaultCloseOperation(this.EXIT_ON_CLOSE);
        getContentPane().setBackground(Color.BLACK);
        setSize(400, 400);
        setLocationRelativeTo(null);
        setResizable(false);
        setTitle("Login");

        JPanel options = new JPanel(new GridLayout(6,1));

        normalMode = new JButton();
        loadGame = new JButton();
        friendlyMode = new JButton();
        speedMode = new JButton();
        join = new JButton();
        normalMode.setText("New game");
        loadGame.setText("Load game");
        friendlyMode.setText("Friendly mode");
        speedMode.setText("Speed mode");
        join.setText("Join");
        normalMode.setFocusable(false);
        loadGame.setFocusable(false);
        friendlyMode.setFocusable(false);
        speedMode.setFocusable(false);
        join.setFocusable(false);
        normalMode.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 23));
        loadGame.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 23));
        friendlyMode.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 23));
        speedMode.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 23));
        join.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 23));
        normalMode.setForeground(Color.white);
        loadGame.setForeground(Color.white);
        friendlyMode.setForeground(Color.white);
        speedMode.setForeground(Color.white);
        join.setForeground(Color.black);
        normalMode.setBackground(Color.black);
        loadGame.setBackground(Color.black);
        friendlyMode.setBackground(Color.black);
        speedMode.setBackground(Color.black);
        join.setBackground(Color.cyan);
        normalMode.addActionListener(this);
        loadGame.addActionListener(this);
        friendlyMode.addActionListener(this);
        speedMode.addActionListener(this);
        join.addActionListener(this);

        username = new JTextField();
        username.setText("Enter your username:");
        username.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
        username.setForeground(Color.black);
        username.setBackground(new Color(45, 141, 33));

        options.add(username);
        options.add(normalMode);
        options.add(loadGame);
        options.add(friendlyMode);
        options.add(speedMode);
        options.add(join);

        add(options);

        setVisible(true);

    }

    @Override
    public void actionPerformed(ActionEvent e) {

        if(e.getSource() == normalMode)
            gameMode = "Normal";
        else if (e.getSource() == loadGame)
            gameMode = "Load";
        else if (e.getSource() == friendlyMode)
            gameMode = "Friendly";
        else if (e.getSource() == speedMode)
            gameMode = "Speed";
        else if (e.getSource() == join) {
            try {
                Socket socket;
                socket = new Socket("localhost", 8080);
                ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
                gameMode = input.readUTF();
                new Game(socket, output, input, gameMode, "Guest_" + username.getText());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            dispose();
            return;
        }
        try {
            ServerSocket serverSocket;
            serverSocket = new ServerSocket(8080);
            Socket socket = serverSocket.accept();
            ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(socket.getInputStream());
            output.writeUTF(gameMode);
            output.flush();
            new Game(socket, output, input, gameMode, "Host_" + username.getText());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        dispose();

    }

}
