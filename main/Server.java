package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import main.Message.Type;

/**
 * The chat server. It can be run as the main program, or can be started as it's
 * own thread in a different program
 * @author Charles (cjfman)
 *
 */
public class Server extends Thread {
	private static Server server;
	private final static int PORT = 4444;
	private ServerSocket serverSocket;
	private final static int max_connections = 500;
	private final static Integer max_conversations = 0x7FFFFFFF;
	private Integer num_connections = 0;
	private static boolean debug_mode;
	private static boolean all_users_mode;
	
	private HashMap<Integer, HashSet<String>> conversations;	// Maps a conversation ID to a list of people
	private HashMap<Integer, ServerConnection> connections; 	// Maps a connection ID to the thread running that connection
	private HashMap<String, Integer> usernames;					// Maps a username to a connection ID
	
	/**
	 * Initialize a server instance on the specified port
	 * @param port
	 * @throws IOException
	 */
	public Server(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        conversations = new HashMap<Integer, HashSet<String>>();
        connections = new HashMap<Integer, ServerConnection>();
        usernames = new HashMap<String, Integer>();
	}
	
	/**
	 * Initialize a server instance on the specified port and specified debug mode
	 * @param port
	 * @throws IOException
	 */
	public Server(int port, boolean debug) throws IOException {
        serverSocket = new ServerSocket(port);
        debug_mode = debug;
        conversations = new HashMap<Integer, HashSet<String>>();
        connections = new HashMap<Integer, ServerConnection>();
        usernames = new HashMap<String, Integer>();
	}

	/**
     * Start a chat server.
     */
    public static void main(String[] args) {
    	logln("Starting Server...");
    	for (String arg:args) {
    		if (arg.equals("--debug"))
    			debug_mode = true;
    		else if (arg.equals("--all"))
    			all_users_mode = true;
    	}
    	try {
			server = new Server(PORT);
			server.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    public void run() {
    	try {
    		if (server != null)	// The server was initialized using the main loop
    			server.serve();
    		else
    			serve();		// The server was initialized outside of the main loop
		} catch (IOException e) {
			e.printStackTrace();
			try {
				serverSocket.close();
			} catch (IOException e1) {
				//e1.printStackTrace();
			}
		}
    }
    
    /**
     * Listens on the specified port for connection requests
     * Spins off new connections in their own threads
     * @throws IOException
     */
	private void serve() throws IOException {
    	logln("Server Started");
        while (true) {
            // Block until a client connects
            Socket socket = serverSocket.accept();
            if (num_connections >= max_connections) {
            	// Too many connections. Close connection
            	socket.close();
            	logln("Max connections reached");
            	continue;
            }
            
            // Start a new connection with an unique ID
            int ID = unusedID();
            ServerConnection connection = new ServerConnection(socket, ID, this);
            synchronized (connections) { connections.put(ID, connection); }
            connection.start();
            synchronized (num_connections) { num_connections++; }
            log(String.format("New Connection ID %d\n", ID));
        }
    }
	
	/**
	 * Looks for an ID that is not being used in the range of acceptable ID's
	 * Acceptable ID's range from 0 to max_connections
	 * @return an unused ID
	 */
	private int unusedID() {
		int res;
		synchronized (connections) {
			// Look for first unused ID in the connections HashMap
			for (res = 0; res < max_connections; res++) {
				if (!connections.containsKey(res)) break;
			}
		}
		return res;
	}
	
	/**
	 * Logs in a connection ID with the specified username. 
	 * @param ID
	 * @param username
	 * @return
	 * Returns true if the log in was successful, otherwise false
	 */
	public boolean login(int ID, String username) {
		synchronized (usernames) {
			if (usernames.containsValue(ID)) return false;	// ID is already associated with username
			if (usernames.containsKey(username)) {
				return usernames.get(username).equals(ID); 	// Returns false if username is registered with different ID
			}
			usernames.put(username, ID);	// Register user
		}
		logln(String.format("Connection ID %d logged in as " + username, ID));
		return true;
	}
	
	/**
	 * Unregisters the specified connection from it's ID
	 * and removes the connection
	 * @param username
	 */
	public void logoff(ServerConnection user) {
		String username = user.getUser();
		int ID = user.getID();
		synchronized (usernames) {
			usernames.remove(username);
		}
		synchronized (connections) {
			connections.remove(ID);
		}
		logln(username + " logged off");
	}
	
	/**
	 * Unregisters the specified username from it's ID
	 * and removes the connection
	 * @param username
	 */
	public void logoff(String username) {
		Integer ID = null;
		synchronized (usernames) {
			if (usernames.containsKey(username)) {
				ID = usernames.get(username);
				usernames.remove(ID);
			}
		}
		if (ID == null)
			return;
		synchronized (connections) {
			connections.remove(ID);
		}
		logln(username + " logged off");
	}
	
	/**
	 * Checks whether a user is currently logged on or not
	 * @param username
	 * @return returns true if the user is logged on
	 */
	public boolean loggedOn(String username) {
		synchronized (usernames) {
			return usernames.containsKey(username);
		}
	}
	
	/**
	 * Initializes a conversation with username
	 * @param username: user to start the conversation
	 * @return
	 * Returns the ID of the newly created conversation, or NULL if a conversation could not be created
	 */
	public Integer startConversation(String username) {
		int ID;
		synchronized (conversations) {
			// Search for first unused conversation ID
			// Acceptable range is 0 to max_conversations
			logln(conversations.toString());

			for (ID = 0; ID < max_conversations; ID++)
				if (!conversations.containsKey(ID)) break;
			if (ID == max_conversations) {
				logln("Max conversations reached");
				return null;
			}
			// Create new recipient list with username
			HashSet<String> conversation = new HashSet<String>();
			conversation.add(username);
			conversations.put(ID, conversation);
		}
		logln(String.format("New conversation ID %d", ID));
		return ID;
	}
	
	/**
	 * Removes the specified user from the specified conversation
	 * @param ID: specifies the conversation
	 * @param username: user to be removed
	 */
	public void leaveConversation(int ID, String username) {
		HashSet<String> conversation;
		synchronized (conversations) { conversation = conversations.get(ID); }
		if (conversation == null) return;	// Conversation does not exist
		synchronized (conversation)	 {
			if (conversation.contains(username))
				conversation.remove(username);
			logln(String.format("%s has left conversation ID %d", username, ID));
			// Destroy the conversation when it's recipient list is empty
			if (conversation.size() == 0) {
				synchronized (conversations) { conversations.remove(ID); }
				logln(String.format("Conversation ID %d closed", ID));
			}
		}
	}
	
	/**
	 * Adds a user to the specified conversation
	 * @param ID: specified conversation
	 * @param username: user to add to conversation
	 * @return true if the user was added to the conversation, otherwise false
	 */
	public boolean addToConversation(int ID, String username) {
		
		// Get conversation
		HashSet<String> conversation;
		synchronized (conversations) {
			conversation = conversations.get(ID);
			if (conversation == null) return false;	// Conversation does not exist
		}
		
		// Add user to conversation
		String members[];
		synchronized(conversation) {
			members = conversation.toArray(new String[conversation.size()]);
			conversation.add(username);
			logln(String.format(username + " has been added conversation ID %d", ID));
		}
			
		// Add conversation ID to connection
		Integer id;
		synchronized (usernames) { id = usernames.get(username); };
		ServerConnection user;
		if (id != null)
		{
			// Look up the connection
			synchronized (connections) { user = connections.get(id); }
			if (user != null) {
				boolean success;
				synchronized (user) { success = user.addConversation(ID, members); }
				if (success) {
					Message message = new Message();
					message.setID(ID);
					message.setType(Type.Add);
					message.setCode(601);
					message.setUser(username);
					pushMessage(message, members);
				}
			}
		}
		

		return true;
	}
	
	/**
	 * Gets the members of a conversation
	 * @param ID, the specified conversation
	 * @return the members of the conversation
	 */
	@SuppressWarnings("unchecked")
	public HashSet<String> getConversationMembers(Integer ID) {
		synchronized (conversations) {
			HashSet<String> conversation = conversations.get(ID);
			if (conversation == null) return null;	// Conversation does not exist
			return (HashSet<String>) conversation.clone();
		}
	}
    
	/**
	 * Called when a connection has closed. It removes it's ID and username
	 * @param ID: Specifies the process to kill
	 */
    public void connectionClosed(int ID) {
        logln(String.format("Connection ID %d closed", ID));
		synchronized (connections) { connections.remove(ID); }
		synchronized (usernames) {
			if (usernames.containsKey(ID)) usernames.remove(ID);
		}
		synchronized (num_connections) { num_connections--; }
    }
    
    /**
     * Sends a chat message to the specified conversation from the specified user
     * @param ID: The conversation to send a message to
     * @param user: The user who is sending the message
     * @param text: The message to be sent
     * @return A String[] containing the usernames of failed recipients
     */
    public ArrayList<String> sendMessage(int ID, String user, String text) {
    	
    	logln(String.format("Message received for conversation %d from user %s", ID, user));
    	
    	ArrayList<String> failed = new ArrayList<String>();	// A list of users who did not receive the message
		
    	// Look up the conversation
    	HashSet<String> conversation;
    	synchronized (conversations) {
	    	if (!conversations.containsKey(ID)) {
	    		logln(String.format("Conversation ID %d does not exist", ID));
	    		return null;
	    	}
	    	conversation = conversations.get(ID);
		}
    	
    	// Loop through each person in the conversation
		synchronized (conversation) {
			for (String contact:conversation) {
				if (contact.equals(user)) continue;	// Do not send message to the original sender
				int user_ID;
				synchronized (usernames) {
					if (!usernames.containsKey(contact)) {
						// Username not registered
						failed.add(contact);
						continue;
					}
					user_ID = usernames.get(contact); 
				}
				ServerConnection receiver;
				synchronized (connections) { 
					if (!connections.containsKey(user_ID)) {
						// Connection is missing
						logoff(contact); // Log off the user
						failed.add(contact);
						continue;
					}
					receiver = connections.get(user_ID); 
				}
				// Attempt to send the message
				synchronized (receiver) { 
					try {
						receiver.receiveMessage(ID, user, text);
						logln(String.format("Message sent from %s to %s through ID %d", user, contact, ID));
					} catch (IOException e) {
						// Message send failed
						failed.add(contact);
					} 
				}
			}
		}
		
		if (failed.size() == 0)
			logln("Message Delivered Successfully");
		else
			logln("Failed to deliver message to the following recipients: " + failed);
		
    	return failed;
    }
    
    /**
     * Adds the specified user to the friend list of another user
     * @param user: the user to add to
     * @param friend: the user to be added
     */
    public void addFriend(String user, String friend) {
    	Integer ID = null;
    	
    	// Look up user ID
    	synchronized(usernames) {
    		if (usernames.containsKey(user))
    			ID = usernames.get(user);
    	}
    	
    	if (ID == null) return;
    	
    	// Look up connection
    	ServerConnection connection = null;
    	synchronized(connections) {
    		if (connections.containsKey(ID))
    			connection = connections.get(ID);
    	}
    	
    	// Add friend
    	synchronized(connection) { connection.addFriend(friend); }
    }
    
    /**
     * Finds every user who has friended the specified user
     * @param user: the user to search for
     * @return a list of users who have friended the specified user
     */
    public ArrayList<String> findFriends(String user) {
    	logln("Finding friends for " + user);
    	ArrayList<String> friends = new ArrayList<String>();
    	synchronized (connections) {
    		// Loop through every connection
	    	for (Integer ID:connections.keySet()) {
	    		ServerConnection connection = connections.get(ID);
	    		synchronized (connection) {
	    			// Add username of connection if friend
	    			String friend = connection.getUser();
	    			if (friend == null) continue;
	    			if (friend.equals(user)) continue;
	    			if (connection.isFriend(user)) {
	    				friends.add(friend);
	    				logln("Found " + friend);
	    			}
	    		}
	    	}
    	}
    	return friends;
    }
    
    /**
     * Pushes a message to a list of users
     * @param message: The message to be pushed
     * @param users: The list of users to forward the message to
     * @return A list of users who failed to receive the message
     */
    public ArrayList<String> pushMessage(Message message, String[] users) {
    	String log = "Push message to { ";
    	for (String user:users) {
    		log += user + ", ";
    	}
    	logln(log + "}");
    	
    	ArrayList<String> failed = new ArrayList<String>();	// A list of failed recipients
    	
    	// Loop through every user in the list
    	for (String user:users) {
    		Integer ID = null;
    		
    		// Look up the user ID
        	synchronized(usernames) {
        		if (usernames.containsKey(user))
        			ID = usernames.get(user);
        	}
        	
        	if (ID == null) continue;
        	
        	// Lookup the connection
        	ServerConnection connection = null;
        	synchronized(connections) {
        		if (connections.containsKey(ID))
        			connection = connections.get(ID);
        	}
        	
        	// Push the message
        	synchronized(connection) { 
        		try {
					connection.pushMessage(message);
				} catch (IOException e) {
					failed.add(user);
				} 
        	}
    	}
    	return failed;
    }
    
    @SuppressWarnings("deprecation")
	public void kill() {
    	try {
			serverSocket.close();
		} catch (IOException e) {
		}
    	this.stop();
    }
    
    /**
     * Pushes a message to a conversation
     */
    public ArrayList<String> pushMessageToConversation(Integer ID, Message message) {
    	String[] conversation;
    	
    	// Look up members of conversation
    	synchronized (conversations) {
    		if (!conversations.containsKey(ID))
    			return null;
    		HashSet<String> temp = conversations.get(ID);
    		conversation = temp.toArray(new String[temp.size()]);
    	}
    	return pushMessage(message, conversation);
    }
    
    /**
     * Getter for all_users_mode
     * @return
     */
    public boolean allUsersMode() {
    	return all_users_mode;
    }
    
    /**
     * Prints out a log message
     * @param message
     */
    private void log(String message) {
    	if (debug_mode)
    		System.out.print(message);
    }
    
    /**
     * Prints out a log message with a new line
     * @param message
     */
    private static void logln(String message) {
    	if (debug_mode)
    		System.out.println(message);
    }
}