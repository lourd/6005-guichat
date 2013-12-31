package main;

import gui.ClientGUI;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
/**
 * Interprets received messsages and keeps track of conversations and who is online.
 * @author Andres
 *
 */
public class ClientModel {
    private ArrayList<String> FriendsList;
    private ArrayList<String> OfflineFriends;
    private final String user;
    private ClientGUI GUI;
    private ConcurrentLinkedQueue<Message> sendables;
    private HashMap<Integer,ClientConversation> Conversations;
    public boolean Debug = false; // Used for testing in JUnit
    public ArrayList<String> Tester = new ArrayList<String>();
    
    public ClientModel(String user, ConcurrentLinkedQueue<Message> sendables) {
        this.user = user;
        this.FriendsList = new ArrayList<String>();
        this.GUI = null;
        this.Conversations = new HashMap<Integer,ClientConversation>();
        this.sendables = sendables;
        this.OfflineFriends = new ArrayList<String>();
    }
    
    /**
     * On first login packet, start the client-side GUI and set it.
     */
    public void login() {
        ClientGUI GUI = new ClientGUI(this.sendables,this.user);
        this.GUI = GUI;
        if (Debug) {
            Tester.add("Login");
        }
    }
    
    /**
     * If received a logout message, exit the client (presumably only happens after you request a logout
     */
    public void logout() {
        if (Debug) {
            Tester.add("Logout");
        }
        this.Debug = false;
    }
    
    /**
     * Adds friends to friends lists from "success" messages from the server.
     * @param input
     */
    public void friends(Message input) {
        // You can't add yourself!
        if (input.User.equals(this.user)) {
            ;
        }
        
        // Add a user (may be offline)
        else if (input.getCode() == 301) {
            if (!this.OfflineFriends.contains(input.User)) {
                this.OfflineFriends.add(input.User);
                ChangedFriendsList();
            }
        }
        // Add multiple users ( may also be offline)
        else if (input.getCode() == 302) {
            for (String i: input.Friends) {
                if (!this.OfflineFriends.contains(i)) {
                    this.OfflineFriends.add(i); 
                }
            }
            ChangedFriendsList();
        }
        
        // Single user add, is online!
        else if (input.getCode() == 304 | input.getCode() == 305) {
            if (!this.FriendsList.contains(input.User)) {
                this.FriendsList.add(input.User);
                if (this.OfflineFriends.contains(input.User)) {
                    this.OfflineFriends.remove(input.User);
                }
                ChangedFriendsList(); 
            }
        }
        
        // Many user add, are online!
        else if (input.getCode() == 305) {
            for (String i: input.Friends) {
                if (!this.FriendsList.contains(i)) {
                    this.FriendsList.add(i); 
                }
            }
            ChangedFriendsList();
        }
        
        if (Debug) {
            Tester.add("Friends: " + input.User);
        }
    }
    /**
     * Starts a new conversation with selected users.
     * @param input
     */
    public void startConversation(Message input) {
        // Single user Conversation with yourself
        if (input.User.equals(this.user)) {
            gui.ErrorWindow.makeError("What?", "You can't start a conversation with yourself!");
        }

        //Single User conversation
        else if (input.getCode() == 601 |  input.getCode() == 605) {
            String[] Users = {input.User};
            if (input.getCode() == 605) {
                Users = new String[0];
            }
            ClientConversation Conv = new ClientConversation(Users);
            Conversations.put(input.getID(),Conv);
            GUI.createNewConversation(input.getID(),Conv);
            // Check if the user in the conversation isn't your friend and adds them automatically
            if (!this.FriendsList.contains(input.User)) {
                this.FriendsList.add(input.User);
                ChangedFriendsList();
                this.sendables.add(ClientMessageSender.CreateFriendAddMessage(input.User));   
            }
        }
        // Multi User conversation
        else if (input.getCode() == 602) {
            ClientConversation Conv = new ClientConversation(input.Friends);
            Conversations.put(input.getID(),Conv);
            GUI.createNewConversation(input.getID(),Conv);
            // Check if any of the users in the conversation aren't your friend and adds them automatically
            for (String friend : input.Friends) {
                if (!this.FriendsList.contains(friend)) {
                    this.FriendsList.add(friend);
                    ChangedFriendsList();
                    this.sendables.add(ClientMessageSender.CreateFriendAddMessage(friend));   
                }
            }
        }
        if (Debug) {
            Tester.add("Start: " + input.getID());
        }
    }
    
    public void updateConversation(Message input) {
        //If not your own message, update the conversation
        if (input.getCode() == 400) {}
        else if (input.User != this.user) {
            if (Conversations.containsKey(input.ConversationID)) {
                Conversations.get(input.ConversationID).UpdateConversation(input.User,input.Status);
                GUI.updateConvoText(input.ConversationID);
                if (Debug) {
                    Tester.add("Update: " + input.Status);
                }
            }
            else {
                if (Debug) {
                    System.out.println("Received Message, wasn't in Conversation");
                }
            }
        }
    }
    
    public void addToConversation(Message input) {
        //Adding a single person to a conversation
        if (input.getCode() == 601) {
            if (Conversations.containsKey(input.ConversationID)) {
                Conversations.get(input.ConversationID).AddUser(input.User);
                GUI.updateConvoUsers(input.ConversationID);
                if (Debug) {
                    Tester.add("Add: " + input.User);
                }
            }
            else {
                if (Debug) {
                    System.out.println("Received a bad message");
                }
            }
        }
        // Adding multiple people to a conversation
        if (input.getCode() == 602) {
            if (Conversations.containsKey(input.ConversationID)) {
                for (String friend : input.Friends) {
                    Conversations.get(input.ConversationID).AddUser(friend);
                    GUI.updateConvoUsers(input.ConversationID);
                    if (Debug) {
                        Tester.add("Add: " + input.Friends);
                    }
                }
            }
            else {
                if (Debug) {
                    System.out.println("Received a bad message");
                }
            }
        }
    }
    
    /**
     * Deals with typing, entered, and cleared text "Events", as well as away and idle events
     * @param input
     */
    public void event(Message input) {
        
        if (input.User.equals(this.user)) {
            if (input.getCode() == 704) {
                Conversations.remove(input.ConversationID);
            }
        }
        //Friend online
        else if (input.getCode() == 700) {
            if (!this.FriendsList.contains(input.User)) {
                this.FriendsList.add(input.User);
                if (this.OfflineFriends.contains(input.User)) {
                    this.OfflineFriends.remove(input.User);
                }
                ChangedFriendsList();
            }
            if (Debug) {
                Tester.add("Event: " + input.User);
            }
        }
        //Friend offline
        else if (input.getCode() == 701) {
            this.FriendsList.remove(input.User);
            this.OfflineFriends.add(input.User);
            ChangedFriendsList();
            if (Debug) {
                Tester.add("Event: " + input.User);
            }
        }
        //User entered text
        else if (input.getCode() == 702) {
            if (Conversations.containsKey(input.ConversationID)) {
                Conversations.get(input.ConversationID).ChangeStatus(input.User, "User has entered text.");
                GUI.updateConvoUsers(input.ConversationID);
                if (Debug) {
                    Tester.add("Event: " + input.User);
                }
            }
        }
        //User entering text
        else if (input.getCode() == 703) {
            if (Conversations.containsKey(input.ConversationID)) {
                Conversations.get(input.ConversationID).ChangeStatus(input.User, "User is typing");
                GUI.updateConvoUsers(input.ConversationID);
                if (Debug) {
                    Tester.add("Event: " + input.User);
                }
            }
        }
        //User left conversation
        else if (input.getCode() == 704) {
            if (Conversations.containsKey(input.ConversationID)) {
                Conversations.get(input.ConversationID).RemoveUser(input.User);
                GUI.updateConvoUsers(input.ConversationID);
            }
            if (Debug) {
                Tester.add("Event: " + input.User);
            }
        }
        //User cleared text box
        else if (input.getCode() == 705) {
            if (Conversations.containsKey(input.ConversationID)) {
                Conversations.get(input.ConversationID).ChangeStatus(input.User, "");
                GUI.updateConvoUsers(input.ConversationID);
                if (Debug) {
                    Tester.add("Event: " + input.User);
                }
            }
        }
    }
    
    public void ChangedFriendsList() {
        String[] newList = this.FriendsList.toArray(new String[0]);
        String[] newOfflineList = this.OfflineFriends.toArray(new String[0]);
        this.GUI.fireChanges(newList, newOfflineList);
    }
    
    public void status(Message input) {
        //Not yet implemented
    }
}
