package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.text.DefaultCaret;

import main.ClientConversation;
import main.ClientMessageSender;
import main.Message;

/**
 * GUI for individual conversation windows
 * @author descioli
 */
public class ConvoGUI extends JPanel implements KeyListener {

    private JTable talkers;
    private MyTableModel tm;
    private JScrollPane talkerScrollPane;
    private JTextArea convoText;
    private JScrollPane convoScrollPane;
    private JTextArea msgText;
    private JButton addFriend;
    //private JScrollPane msgScrollPane;

    private ConcurrentLinkedQueue<Message> sendables;
    private String user;
    private int ID;
    public ClientConversation clientConvo;
    private boolean typing;
    private ScheduledExecutorService timer;
    private ScheduledFuture<?> typingCheck;

    public ConvoGUI(ClientConversation clientConvo,
            ConcurrentLinkedQueue<Message> sendables, String user, int ID) {
        super(false);

        this.sendables = sendables;
        this.user = user;
        this.ID = ID;
        this.clientConvo = clientConvo;
        this.timer = Executors.newSingleThreadScheduledExecutor();

        BorderLayout layout = new BorderLayout();
        this.setLayout(layout);
        
        String[] users = clientConvo.UserStatus.keySet().toArray(new String[0]);
        String[][] data = new String[users.length][2];
        for (int i = 0; i < users.length; i++) {
            data[i][0] = users[i];
            data[i][1] = clientConvo.UserStatus.get(users[i]);
        }
        Object[] colNames = { "In conversation", "Status" };
        this.tm = new MyTableModel(data, colNames);
        
        this.talkers = new JTable(tm);
        this.convoText = new JTextArea();
        this.convoText.setLineWrap(true);
        this.convoText.setWrapStyleWord(true);
        
        // This keeps the scroll bar at the bottom on refreshes
        DefaultCaret caret = (DefaultCaret)this.convoText.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
        
        convoText.setEditable(false);
        this.msgText = new JTextArea();
        msgText.setLineWrap(true);
        msgText.setWrapStyleWord(true);
        this.addFriend = new JButton("Add Friend");

        this.talkerScrollPane = new JScrollPane(talkers);
        this.talkerScrollPane.setPreferredSize(new Dimension(350,80));
        this.convoScrollPane = new JScrollPane(convoText);
        this.convoScrollPane.setPreferredSize(new Dimension(350, 200));

        
        JPanel topPane = new JPanel();
        topPane.setLayout(new BoxLayout(topPane, BoxLayout.LINE_AXIS));
        topPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        topPane.add(this.talkerScrollPane);
        topPane.add(Box.createRigidArea(new Dimension(10,10)));
        topPane.add(this.addFriend);
        
        this.add(topPane, BorderLayout.PAGE_START);
        
        JPanel midPane = new JPanel();
        midPane.setLayout(new BoxLayout(midPane, BoxLayout.PAGE_AXIS));
        midPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        midPane.add(this.convoScrollPane);
        this.add(midPane, BorderLayout.CENTER);
        this.add(this.msgText, BorderLayout.PAGE_END);

        this.msgText.addKeyListener(this);
        this.typing = false;
        
        
        this.addFriend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendAddMsg();
            }
        });
        
        this.addFocusListener(new FocusListener(){
            public void focusGained(FocusEvent e){
                msgText.requestFocusInWindow();
            }

            @Override
            public void focusLost(FocusEvent e) {}
        });
    }

    /**
     * This makes a pop-up where the user can add someone to a conversation, then sends a message to the server requesting it.
     */
    public void sendAddMsg() {
        String s = JOptionPane.showInputDialog(null, "To:", "New Conversation",
                JOptionPane.PLAIN_MESSAGE);

        if (s != null) {
            Message request = ClientMessageSender.CreateAddMessage(ID,s);
            this.sendables.add(request);
        }
    }
    
    /**
     * Sends a message with the text given to the server, with the conversation ID.
     * @param text
     */
    public void sendMsg(String text) {
        Message request = ClientMessageSender.CreateSentMessage(this.ID,
                this.user, text);
        this.sendables.add(request);
        this.clientConvo.UpdateConversation(this.user, text);
        this.updateText();
    }

    /**
     * Sends a message saying the user is typing.
     */
    public void sendTyping() {
        Message request = ClientMessageSender.CreateTypingTextMessage(this.ID,
                this.user);
        this.sendables.add(request);
    }

    /**
     * Sends a message saying the user has entered text.
     */
    public void sendEntered() {
        Message request = ClientMessageSender.CreateEnteredTextMessage(this.ID,
                this.user);
        this.sendables.add(request);
    }

    /**
     * Sends a message saying the user has cleared his input box
     */
    public void sendCleared() {
        Message request = ClientMessageSender.CreateClearedTextMessage(this.ID,
                this.user);
        this.sendables.add(request);
    }

    @Override
    /**
     * This handles typing, cleared, and entered text overhead, calls smaller functions to send the messages.
     */
    public void keyPressed(KeyEvent e) {
        if (e.getSource() == this.msgText) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                sendMsg(msgText.getText());
                // If a timer had been started, cancel it
                if(this.typingCheck!=null){
                    this.typingCheck.cancel(true);
                }
                this.typing= false;
                msgText.setText("");
                sendCleared();
                msgText.requestFocus();
                e.consume();
            } 
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if (!this.typing){
            sendTyping();
            this.typing = true;
        }
        // If the timer had been started before, cancel it
        if(this.typingCheck!=null){
            this.typingCheck.cancel(true);
        }
        // Restart the timer
        typingCheck = this.timer.schedule(new Runnable() {
            public void run(){
                // Once the timer finishes, send a messages
                // based on if the text is empty or not
                if(msgText.getText()==null ||
                        msgText.getText().equals("")){
                    sendCleared();
                } else{
                    sendEntered();
                }
                typing = false;
            }
        }, 1, TimeUnit.SECONDS);
    }

    /**
     * Refreshes the user table whenever there is a new status, or user (rebuilds the whole table)
     */
    public void updateUsers() {
        // Looks through the hashmap keys (the hashmap is User -> Status
        String[] users = clientConvo.UserStatus.keySet().toArray(new String[0]);
        String[][] data = new String[users.length][2];
        for (int i = 0; i < clientConvo.UserStatus.size(); i++) {
            // Place every user next to their status
            data[i][0] = users[i];
            data[i][1] = clientConvo.UserStatus.get(users[i]);
        }
        
        // Set the tableModel
        String[] colNames = { "In conversation", "Status" };
        this.tm = new MyTableModel(data, colNames);
        this.talkers.setModel(tm);
    }

    /**
     * Updates the text box for conversations
     */
    public void updateText() {
        this.convoText.setText(this.clientConvo.buff.toString());
    }
    
}
