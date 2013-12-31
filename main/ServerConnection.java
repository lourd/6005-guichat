package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;

import main.Message.Type;

/**
 * This class handles a single connection to the server as it's own thread
 * @author Charles (cjfman)
 *
 */
public class ServerConnection extends Thread {
	private enum State {BEGIN, IDLE, LOGOFF, FRIENDS};
	
	private final Socket socket;
	private final int id;
	private final Server server;
	private State state;
	private ObjectInputStream in;
	private ObjectOutputStream out;
	
	private String user;
	private HashSet<String> friends;
	private HashSet<Integer> conversations;
	private boolean all_users_mode;
	
	private int min_user_length = 4;
	private int max_user_length = 16;

	/**
	 * 
	 * @param socket
	 * @param id
	 * @param server
	 */
	public ServerConnection(Socket socket, int id, Server server) {
		this.socket = socket;
		this.id = id;
		this.server = server;
		this.state = State.BEGIN;
		this.all_users_mode = server.allUsersMode();
		friends = new HashSet<String>();
		conversations = new HashSet<Integer>();
	}
	
	public void run() {
		// Handle the client
		// Send Welcome Message
		Message message = new Message();
		message.setType(Type.Response);
		message.setCode(100);
		message.setStatus("Welcome");
		synchronized (socket) {
			try {
				out = new ObjectOutputStream(socket.getOutputStream());
				out.writeObject(message);
		        in = new ObjectInputStream(socket.getInputStream());
			} catch (IOException e) {
				//e.printStackTrace();
			}
        }
		
		// Keep connection open
		while (!socket.isClosed()) {
	        try {
	        	if (state == State.FRIENDS) {
	        		findFriends();
	        		state = State.IDLE;
	        	}
	            handleConnection();
	            if (state == State.LOGOFF) logoff();
	        } catch (IOException e1) {
        		e1.printStackTrace();
        		try {
            		logoff();
	        	} catch (IOException e2) {
	        		//e2.printStackTrace();
	        	}
	        } catch (Exception e3) {
	        	e3.printStackTrace();
	        }
		}
	}
	
	/**
	 * This function gets called by other threads who want to send messages to this user
	 * @param ID: The conversation ID
	 * @param user: The sender
	 * @param text: The message
	 * @throws IOException
	 */
	public void receiveMessage(int ID, String user, String text) throws IOException {
		Message message = new Message();
		message.setType(Type.Message);
		message.setUser(user);
		message.setID(ID);
		message.setStatus(text);
		synchronized (socket) {
	        out.writeObject(message);
		}
	}
	
	/**
	 * Pushes a message to the client
	 * @param message: the message to be pushed
	 * @throws IOException
	 */
	public void pushMessage(Message message) throws IOException {
		synchronized (socket) {
	        out.writeObject(message);
		}
	}
	
	/**
	 * Adds a friend to this users friend list
	 * Pushes the friend to the client
	 * @param username: the friend to be added
	 */
	public void addFriend(String username) {
		synchronized(friends) { friends.add(username); }
		Message message = new Message();
		message.setType(Type.Friends);
		message.setCode(305);
		message.setUser(username);
		synchronized (socket) {
	        try {
				out.writeObject(message);
			} catch (IOException e) {
				//e.printStackTrace();
			}
		}
	}
	
	/**
	 * This method adds a conversation to the list
	 * @param ID: The conversation to allow access to
	 */
	public void addConversation(Integer ID) {
		// Add conversation ID to list
		synchronized (conversations) { conversations.add(ID); }
	}
	
	/**
	 * This method is called by other thread to allow access to conversations
	 * A start Message is forwarded to the user with the members of the conversation
	 * @param ID: The conversation to allow access to
	 */
	public boolean addConversation(Integer ID, String[] members) {
		// Add conversation ID to list
		synchronized (conversations) { conversations.add(ID); }
		
		// Forward message to client
		//HashSet<String> members = server.getConversationMembers(ID);
		//if (members == null) return;
		Message message = new Message();
		message.setID(ID);
		message.setType(Type.Start);
		message.setFriends(members);
		message.setCode(602);
		synchronized (socket) {
	        try {
				out.writeObject(message);
			} catch (IOException e) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Username getter
	 * @return
	 */
	public String getUser() {
		return user;
	}
	
	/**
	 * id getter
	 */
	public int getID() {
		return id;
	}
	
	/**
	 * returns true if the specified user is a friend of this user
	 * @param friend: the specified user
	 * @return
	 */
	public boolean isFriend(String friend) {
		if (all_users_mode) return true;
		synchronized (friends) {
			return friends.contains(friend);
		}
	}
	
	/**
	 * Handles incoming messages from the client
	 * @throws IOException
	 */
	private void handleConnection() throws IOException {		
        Message message = null;
         
        // Try and read message
        try {
			message = (Message) in.readObject();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return;
		} catch (Exception e2) {
			state = State.LOGOFF;
			return;
		}
        
        if (message == null) return;
                
        // Handle the message. Switch based on it's type
        switch(message.getType()) {
        case Login:
        	message = login(message);
        	break;
        case Logout:
        	message = logout(message);
        	break;
        case Friends:
        	message = friends(message);
        	break;
        case Start:
        	message = startConversation(message);
        	break;
        case Message:
        	message = sendMessage(message);
        	break;
        case Add:
        	message = addToConversation(message);
        	break;
        case Event:
        	message = event(message);
        	break;
        case Status:
        	message = event(message);
        	break;
        default:
        	message = new Message();
        	message.setType(Type.Error);
        	message.setStatus("Invalid Command");
        	message.setCode(505);
        }
        
        // Send back the response message
        if (message == null) return;
        synchronized (socket) {
	        out.writeObject(message);
        }
    }
	
	/**
	 * Handles a login request
	 * @param message
	 * @return response message
	 */
	private Message login(Message message) {
		// Initiate response message with default settings
		Message res = new Message();
		
		// Handle Request
		
		// State must be Begin to log in
		if (state != State.BEGIN) {
			res.setStatus("Already logged in");
			res.setCode(502);
			res.setType(Type.Error);
			return res;
		}
		
		// Missing necessary parameter
		if (message.getUser() == null) {
			return missingError();
		}
		
		// User name must be the proper length
		if (message.getUser().length() > max_user_length || message.getUser().length() < min_user_length) {
			res.setStatus(String.format("Username must be shorter than %d and longer than %d", max_user_length, min_user_length));
			res.setCode(501);
			res.setType(Type.Error);
			return res;
		}
		
		// Log the user in
		String user = message.getUser();
		if (server.login(id, user)) {
			this.user = user;
			res.setStatus("Welcome " + user);
			res.setType(Type.Login);
			res.setCode(101);
			
			state = State.FRIENDS;
			return res;
		}
		
		// Login failed
		res.setStatus(user + " already logged in");
		res.setCode(502);
		res.setType(Type.Error);
		return res;
	}
	
	/**
	 * Handle Logout request
	 * @param message response message
	 * @return
	 */
	private Message logout(Message message) {
		Message res = new Message();
		res.setType(Type.Logout);
		res.setStatus("Good Bye " + user);
		res.setCode(200);
		state = State.LOGOFF;
		return res;
	}
	
	/**
	 * Handles a request to add users to the friends list
	 * @param message
	 * @return response message
	 */
	private Message friends(Message message) {
		// Initiate response message with default settings
		Message res = new Message();
		
		int added = 0;
		
		// Check for correct state
		Message error = checkState();
		if (error != null)
			return error;
		
		// Add a single user
		if (message.getUser() != null && message.getUser().length() > 0) {
			res.setType(Type.Friends);
			String friend = message.getUser();
			synchronized(friends) {
				if (friends.contains(friend)) {
					res.setStatus("0 friends added");
					res.setCode(300);
					return res;
				}
				friends.add(friend);
			}
			server.addFriend(message.getUser(), user);
			res.setStatus("1 friend added");
			res.setUser(friend);
			
			// Check to see if user is logged on
			if (server.loggedOn(friend))
			{
				res.setCode(304);
			}
			else
				res.setCode(301);
			return res;
		} 
		
		// Add a list of users
		if (message.Friends != null && message.Friends.length > 0) {
			ArrayList<String> online = new ArrayList<String>();
			String [] f = message.getFriends();
			
			// Loop through every friend in the list
			for (int i = 0; i < f.length; i++) {
				synchronized(friends) {
					if (friends.contains(f[i])) continue;
					friends.add(f[i]);
					added++;
				}
				server.addFriend(f[i], user);
				
				// Check to see if user is logged on
				if (server.loggedOn(f[i]))
					online.add(f[i]);
			}
			res.setType(Type.Friends);
			res.setStatus(String.format("%d friends added", added));
			
			// Set proper response code if any friends are logged on
			if (online.size() > 0) {
				res.setCode(303);
				res.setFriends(online.toArray(new String[online.size()]));
			}
			else
				res.setCode(302);
			return res;
		}
		
		return missingError();	
	}
	
	/**
	 * Starts a conversation with the requested friends
	 * @param message
	 * @return
	 */
	private Message startConversation(Message message) {
		Message res = new Message();
		
		// Check for the proper state
		Message error = checkState();
		if (error != null)
			return error;
		
		// Start a conversation with the current user
		Integer ID = server.startConversation(user);
		if (ID == null) {
			// Conversation start failed
			res.setStatus("Server Busy. Try again later");
			res.setType(Type.Error);
			res.setCode(507);
			return res;
		}
		addConversation(ID);
		
		// Add the requested friends
		return addToConversation(message, ID);
	}
	
	/**
	 * Sends a text message to the specified conversation
	 * @param message
	 * @return
	 */
	private Message sendMessage(Message message) {
		Message res = new Message();
		
		// Check state
		Message error = checkState();
		if (error != null)
			return error;
		
		// Get info
		int ID = message.getID();
		String text = message.getStatus();
		
		// Check ID
		error = checkConversation(ID);
		if (error != null)
			return error;
		
		// Check for missing fields
		if (text == null || text.length() == 0)
			return missingError();
		
		// Ask server to push the message
		// Server responds with failed recipients list
		ArrayList<String> failed = server.sendMessage(ID, user, text); 
		if (failed == null) {
			res.setStatus("Message Send Failure");
			res.setType(Type.Error);
			res.setCode(510);
			res.setID(ID);
			return res;
		} 
		
		// Push failed list to client
		if (failed.size() > 0) {
			res.setFriends((String[])failed.toArray());
			res.setStatus("Failed to deliver message to the following recipients");
			res.setType(Type.Message);
			res.setCode(401);
			res.setID(ID);
			return res;
		}
		
		// No error
		res.setStatus("Message Received");
		res.setType(Type.Message);
		res.setCode(400);
		res.setID(ID);
		return res;
	}
	
	/**
	 * Handles add to conversation requests
	 * @param message
	 * @return
	 */
	private Message addToConversation(Message message) {		
		Message error = checkState();
		if (error != null)
			return error;
		
		Integer ID = message.getID();
		
		return addToConversation(message, ID);
	}
	
	/**
	 * Handles pushing event messages
	 * @param message
	 * @return
	 */
	private Message event(Message message) {
		
		// Check the state
		Message error = checkState();
		if (error != null)
			return error;

		int code = message.getCode();
		
		// Override user field
		message.setUser(this.user);
		
		// Client should not send event code less than 702
		if (code < 702) {
			return wrongError();
		}
		
		// Codes less than 710 get forwarded to a conversation
		if (code < 710) {
			int ID = message.getID();
			error = checkConversation(ID);
			if (error != null)
				return error;
			
			// Push the message to the members of the conversation
			server.pushMessageToConversation(ID, message);
			
			// If requested, remove the user from the conversation
			if (code == 704) {
				server.leaveConversation(ID, user);
				synchronized (conversations) {
					conversations.remove(ID);
				}
				return message;
			}
			return message;
		}
		
		// All other messages get forwarded to friends
		server.pushMessage(message, friends.toArray(new String[friends.size()]));
		return message;
	}
	
	/**
	 * Finds people who have friended this user and adds them to the friends list
	 */
	private void findFriends() {
		for (String friend:server.findFriends(user))
			addFriend(friend);
		
		// Send Logged in message
		Message message = new Message();
		message.setType(Type.Event);
		message.setUser(user);
		message.setCode(700);
		message.setStatus(user + " has logged on");
		server.pushMessage(message, friends.toArray(new String[friends.size()]));
	}
	
	/**
	 * Runs the logoff sequence
	 * @throws IOException
	 */
	private void logoff() throws IOException {
		
		// Remove user from every conversation they are in
		synchronized (conversations) {
			for (Integer ID:conversations) {
				Message message = new Message();
				message.setType(Type.Event);
				message.setCode(704);
				message.setUser(user);
				message.setID(ID);
				server.pushMessageToConversation(ID, message);
				server.leaveConversation(ID, user);
			}
		}
		
		// Push logoff message to friends
		Message message = new Message();
		message.setType(Type.Event);
		message.setUser(user);
		message.setCode(701);
		message.setStatus(user + " has logged off");
		server.pushMessage(message, friends.toArray(new String[friends.size()]));
		
		server.logoff(this);
		socket.close();
	}
	
	/**
	 * Returns a missing field error message
	 * @return the error Message
	 */
	private Message missingError() {
		Message res = new Message();
		res.setType(Type.Error);
		res.setStatus("Required field is missing");
		res.setCode(503);
		return res;
	}
	
	/**
	 * Returns a sequence error message
	 * @return the error Message
	 */
	private Message wrongError() {
		Message res = new Message();
		res.setType(Type.Error);
		res.setStatus("Command sequence error");
		res.setCode(504);
		return res;
	}
	
	/**
	 * Checks to see is what the state is and returns and error message
	 * if the state is not IDLE, otherwise returns null
	 * @return the error Message, or null
	 */
	private Message checkState() {
		Message res = new Message();
		if (state == State.BEGIN) {
			res.setStatus("Not logged in");
			res.setCode(502);
			res.setType(Type.Error);
			return res;
		} else if (state != State.IDLE) {
			res.setStatus("Command Sequence Error");
			res.setCode(504);
			res.setType(Type.Error);
			return res;
		}
		
		return null;
	}
	
	/**
	 * Checks to see if the user is part of the specified conversation
	 * Returns an error Message if the user is not part of the conversation
	 * Otherwise null
	 * @param ID, the specified conversation
	 * @return The error Message, or null
	 */
	private Message checkConversation(Integer ID) {
		Message res = new Message();

		synchronized (conversations) {
			if (!conversations.contains(ID)) {
				res.setStatus("ID does not match any of your conversations");
				res.setType(Type.Error);
				res.setCode(509);
				return res;
			}
		}
		
		return null;
	}
	
	/**
	 * Adds the requested people to a conversation
	 * @param message, contains the people to add
	 * @param ID, the conversation to add the people to
	 * @return
	 */
	private Message addToConversation(Message message, Integer ID) {
		Message res = new Message();
		Type type = message.getType();
		
		// Add a single user
		if (message.getUser().length() > 0) {
			String friend = message.getUser();
			
			// Check to see if friend is logged on
			if (!server.loggedOn(friend)) {
				res.setStatus(String.format("%s is not logged on", friend));
				res.setCode(506);
				res.setType(Type.Error);
				return res;
			}
			
			// Attempt to add friend and check for fatal error
			if (!server.addToConversation(ID, friend)) {
				res.setStatus("Internal Server Error");
				res.setCode(508);
				res.setType(Type.Error);
				state = State.LOGOFF;
				return res;
			}
			
			// Friend was added. Notify client
			res.setType(type);
			res.setUser(friend);
			res.setID(ID);
			res.setCode(601);
			return res;
		}
		
		// Add multiple friends
		if (message.getFriends().length > 0) {
			String[] contacts = message.getFriends();
			ArrayList<String> added = new ArrayList<String>();	// A list to keep track of who was successfully added
			
			// Loop through list
			for (String contact:contacts) {
				// Check to see if they are logged on
				if (server.loggedOn(contact)) {
					// Attempt to add contact and check for fatal error
					if (!server.addToConversation(ID, contact)) {
						res.setStatus("Internal Server Error");
						res.setCode(508);
						state = State.LOGOFF;
						return res;
					}
					added.add(contact);
				}
			}
			
			// Check to see if anyone was added successfully
			if (added.size() == 0) {
				res.setStatus("None of these people are logged on");
				res.setType(type);
				res.setCode(605);
				return res;
			}
			
			// Respond with the list of successful additions to the conversation
			res.setType(type);
			res.setID(ID);
			res.setCode(602);
			res.setFriends((String[])added.toArray(new String[added.size()]));
			return res;
		}
		return missingError();
	}
}