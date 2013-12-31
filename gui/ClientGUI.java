package gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;

import main.ClientConversation;
import main.ClientMessageSender;
import main.Message;

/**
 * Main client GUI using Java Swing
 * @author descioli
 */
public class ClientGUI extends JPanel {
    private JList friendsList;
    private DefaultListModel friendsListModel;
    private JList OfflineFriends;
    private DefaultListModel offlineFriendsModel;
    private ConcurrentLinkedQueue<Message> sendables;
    private String user;
    private NewMsgDialog newMsgDialog;
    private JFrame friendsFrame;
    private JFrame convosFrame;
    private JComponent convoCon;
    private JTabbedPane convosTabPane;
    private HashMap<Integer, ConvoGUI> convos;

    public ClientGUI(ConcurrentLinkedQueue<Message> sendables, String user) {
        super();

        // Create and setup the window
        JFrame frame = new JFrame("Friend List");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        this.user = user;
        this.sendables = sendables;
        this.friendsListModel = new DefaultListModel();
        this.friendsFrame = frame;
        this.convos = new HashMap<Integer, ConvoGUI>();

        frame.setJMenuBar(setupMenu());

        newMsgDialog = new NewMsgDialog(this.friendsFrame);
        newMsgDialog.pack();

        this.friendsListModel = new DefaultListModel();

//        this.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createEmptyBorder(eb, eb, eb, eb),
//                BorderFactory.createEtchedBorder()));
        // Create the list and put it in a scroll pane.
        friendsList = new JList(friendsListModel);
        friendsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        friendsList.setSelectedIndex(0);
        // friendsList.addSelectionListener(this);
        friendsList.setVisibleRowCount(5);
        JScrollPane listScrollPane = new JScrollPane(friendsList);

        this.offlineFriendsModel = new DefaultListModel();

//        this.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createEmptyBorder(eb, eb, eb, eb),
//                BorderFactory.createEtchedBorder()));
        // Create the list and put it in a scroll pane.
        OfflineFriends = new JList(offlineFriendsModel);
        OfflineFriends.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        OfflineFriends.setSelectedIndex(0);
        OfflineFriends.setVisibleRowCount(5);
        JScrollPane offlinelistScrollPane = new JScrollPane(OfflineFriends);

        // Creating and setting up the conversations window
        this.convosFrame = new JFrame("Conversations");
        this.convosTabPane = new JTabbedPane();
        convoCon = new ConvoContainer(convosFrame, convosTabPane);
        convosFrame.getContentPane().add(convoCon, BorderLayout.CENTER);
        convosFrame.pack();
        convosFrame.setLocationRelativeTo(frame);
        convosFrame.setVisible(false);
        convosFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        // disable the close button
        // create custom close operation
        convosFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                closeAllConvos();
            }
        });

        MouseListener mouseListener = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int index = friendsList.locationToIndex(e.getPoint());
                    String User = (String) friendsListModel.get(index);
                    newMessageFromList(User);
                }
            }
        };
        this.friendsList.addMouseListener(mouseListener);

        // Making a panel to go within the frame
        // Makes handling the layout easier
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        this.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        
        // Adding the items to the pane, then adding that to the frame
        JLabel onlineLabel = new JLabel("Online");
        this.add(onlineLabel);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(listScrollPane);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        JLabel offlineLabel = new JLabel("Offline");
        this.add(offlineLabel);
        this.add(Box.createRigidArea(new Dimension(0, 5)));
        this.add(offlinelistScrollPane);
        
        // Setting the alignment of everything to left
        onlineLabel.setAlignmentX(LEFT_ALIGNMENT);
        listScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        offlineLabel.setAlignmentX(LEFT_ALIGNMENT);
        offlinelistScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        
        frame.add(this);

        // Pack and display the window
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JMenuBar setupMenu() {
        // Setting up the menu
        JMenuBar menuBar = new JMenuBar();

        JMenuItem exit = new JMenuItem("Exit");
        menuBar.add(exit);

        JMenu addMenu = new JMenu("Add");
        JMenuItem addFriend = new JMenuItem("Add Friend");
        addMenu.add(addFriend);
        menuBar.add(addMenu);

        // Conversation menu and its options
        JMenu convoMenu = new JMenu("Conversation");
        JMenuItem newConvo = new JMenuItem("New conversation");
        convoMenu.add(newConvo);
        menuBar.add(convoMenu);

        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });

        newConvo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                newMessage();
            }
        });

        addFriend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addFriend();
            }
        });
        return menuBar;
    }

    /**
     * New conversation dialog window
     */
    public void newMessage() {
        String s = JOptionPane.showInputDialog(null, "To:", "New Conversation",
                JOptionPane.PLAIN_MESSAGE);

        if (s != null) {
            Message request = ClientMessageSender.CreateStartMessage(s);
            this.sendables.add(request);
        }
    }

    /**
     * Called by the Add friend button listener for the friendslist.
     */
    public void addFriend() {
        String s = JOptionPane.showInputDialog(null, "To:", "Add Friend",
                JOptionPane.PLAIN_MESSAGE);

        if (s != null) {
            Message request = ClientMessageSender.CreateFriendAddMessage(s);
            this.sendables.add(request);
        }
    }

    /**
     * Send a request to start a conversation
     * 
     * @param User
     */
    public void newMessageFromList(String User) {
        Message request = ClientMessageSender.CreateStartMessage(User);
        this.sendables.add(request);
    }

    /**
     * Called by the acknowledgement that your requested conversation was made,
     * or when someone else adds you.
     * 
     * @param ID
     * @param conv
     */
    public void createNewConversation(int ID, ClientConversation conv) {
        ConvoGUI convoGUI = new ConvoGUI(conv, this.sendables, this.user, ID);
        this.convosTabPane.addTab(Integer.toString(ID), convoGUI);
        convoGUI.requestFocus();
        convosTabPane.setTabComponentAt(this.convosTabPane.getTabCount() - 1,
                new ButtonTabComponent(this, convosTabPane));
        this.convos.put(ID, convoGUI);
        this.convosFrame.pack();
        this.convosFrame.setVisible(true);
    }

    /**
     * Refreshes the Online and Offline friends lists.
     * 
     * @param friends
     * @param offlineFriends
     */
    public void fireChanges(String[] friends, String[] offlineFriends) {
        this.friendsListModel.clear();
        for (String friend : friends) {
            this.friendsListModel.addElement(friend);
        }
        this.offlineFriendsModel.clear();
        for (String offlinefriend : offlineFriends) {
            this.offlineFriendsModel.addElement(offlinefriend);
        }
    }

    /**
     * Called when someone is added/removed from a conversation
     * 
     * @param ID
     */
    public void updateConvoUsers(Integer ID) {
        this.convos.get(ID).updateUsers();
    }

    /**
     * Called when you receive new text for a conversation.
     * 
     * @param ID
     */
    public void updateConvoText(Integer ID) {
        this.convos.get(ID).updateText();
    }

    /**
     * Called by the tab's ButtonTabComponent when it's close button is pressed
     * 
     * @param ID
     *            - the unique ID number of the conversation
     */
    public void closeConvo(Integer ID) {
        if (this.convosTabPane.getTabCount() == 0) {
            convosFrame.setVisible(false);
        }
        this.convos.remove(ID);
        Message n = ClientMessageSender.CreateLeftMessage(ID, this.user);
        this.sendables.add(n);
    }

    /**
     * Called when you press the X on the convo window
     */
    public void closeAllConvos() {
        this.convosTabPane.removeAll();
        Integer[] setKeys = this.convos.keySet().toArray(new Integer[0]);
        for (int ID : setKeys) {
            closeConvo(ID);
        }
        this.convos = new HashMap<Integer, ConvoGUI>();
    }
}
