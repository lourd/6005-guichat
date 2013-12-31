package main;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

/**
 * GUI chat client runner.
 * @author descioli
 */
public class Client {

    /**
     * Start a GUI chat client.
     */
    public static void main(String[] args) {
        // Start the shared queue (for all client threads) for sending messages out.
        ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<Message>();

        // Start the dummy gui for username input
        UsernameInput main = new UsernameInput(messages);
        main.setVisible(true);
        
        // Wait for the user to input a name in the GUI
        while (messages.isEmpty()) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        // Get the name, then start a new model and message handler with the name.
        String Username = messages.peek().getUser();
        ClientModel model = new ClientModel(Username, messages);
        try {
            // The IP is defaulted to localhost (would be hardcoded in a real application as well)
            ClientSocketThread dealer = new ClientSocketThread(new Socket(
                    "localhost", 4444), messages, model, false);
            dealer.run();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class UsernameInput extends JFrame {
    private static final long serialVersionUID = 1L;
    private JLabel enterUser;
    private JTextField userText;
    private JButton loginButton;
    private ConcurrentLinkedQueue<Message> sendables;

    public UsernameInput(ConcurrentLinkedQueue<Message> sendables) {
        super();

        enterUser = new JLabel("Enter a username:");
        userText = new JTextField();
        loginButton = new JButton("Login");

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setAutoCreateGaps(true);
        layout.setAutoCreateContainerGaps(true);

        layout.setHorizontalGroup(layout
                .createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(enterUser).addComponent(userText)
                .addComponent(loginButton));

        layout.setVerticalGroup(layout.createSequentialGroup()
                .addComponent(enterUser).addComponent(userText)
                .addComponent(loginButton));

        setTitle("Login");

        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close(userText.getText());
            }
        });

        userText.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close(userText.getText());
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // 119 comes from seeing how tall the window was after
        // packing it
        setSize(new Dimension(250, 119));
        setResizable(false);
        // Centers the frame on the screen
        setLocationRelativeTo(null);
        setVisible(true);

        setDefaultCloseOperation(EXIT_ON_CLOSE);

        this.sendables = sendables;

    }
    
    /**
     * Called once the box has been finished, closes everything and adds a Login Message to the queue.
     * @param User
     */
    public void close(String User) {
        Message Login = ClientMessageSender.CreateLoginMessage(User);
        this.sendables.add(Login);
        System.out.println(this.sendables);
        this.dispose();
    }
}