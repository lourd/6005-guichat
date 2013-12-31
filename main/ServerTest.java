package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

import org.junit.Test;

import main.Message.*;

/**
 * Unit tests for Server
 * @author descioli
 */
public class ServerTest {
	
	private boolean debug = false;
	
	/**
	 * This test logs in three different users and runs the following sequence of events and expects the specified server responses
	 * -Each user connects in order
	 * -User1 logs in
	 * -User1 adds the other two users to the friend list
	 * -User2 and user3 log in
	 * -User3 adds user2 to friends list
	 * -User1 starts a new conversation with user1 and user2
	 * -User1 sends a message to the conversation
	 * -User1 sends an away status message
	 * -User1 logs out
	 * -User2 sends a message to the conversation
	 * -User2 disconnects
	 * -User3 disconnects
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void testSequence1() throws IOException, ClassNotFoundException {		
		Server server = new Server(4445, debug);
		server.start();
		
		String address = "localhost";
		
		// Connect first user
		Socket user1 = new Socket(address, 4445);
		ObjectInputStream in1 = new ObjectInputStream(user1.getInputStream());
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Response, Code: 100, Status: Welcome }"));
		ObjectOutputStream out1 = new ObjectOutputStream(user1.getOutputStream());

		// Connect second user
		Socket user2 = new Socket(address, 4445);
		ObjectInputStream in2 = new ObjectInputStream(user2.getInputStream());
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Response, Code: 100, Status: Welcome }"));
		ObjectOutputStream out2 = new ObjectOutputStream(user2.getOutputStream());

		// Connect third user
		Socket user3 = new Socket(address, 4445);
		ObjectInputStream in3 = new ObjectInputStream(user3.getInputStream());
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Response, Code: 100, Status: Welcome }"));
		ObjectOutputStream out3 = new ObjectOutputStream(user3.getOutputStream());
		
		// Login first user
		Message message = new Message();
		message.setType(Type.Login);
		message.setUser("cjfman");
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Login, Code: 101, Status: Welcome cjfman }"));
		
		// Add friends to first user
		message = new Message();
		message.setType(Type.Friends);
		message.setFriends(new String[] {"dhrosa", "kemus"});
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Friends, Code: 302, Status: 2 friends added }"));
		
		// Login second user
		message = new Message();
		message.setType(Type.Login);
		message.setUser("dhrosa");
		out2.writeObject(message);
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Login, Code: 101, Status: Welcome dhrosa }"));
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Friends, User: cjfman, Code: 305 }"));
		
		// Make sure user1 gets friend logged in message
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Event, User: dhrosa, Code: 700, Status: dhrosa has logged on }"));
		
		// Login third user
		message = new Message();
		message.setType(Type.Login);
		message.setUser("kemus");
		out3.writeObject(message);
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Login, Code: 101, Status: Welcome kemus }"));
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Friends, User: cjfman, Code: 305 }"));
		
		// Make sure user1 gets friend logged in message
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Event, User: kemus, Code: 700, Status: kemus has logged on }"));
		
		// Add second user to third user's friend list
		message = new Message();
		message.setType(Type.Friends);
		message.setUser("dhrosa");
		out3.writeObject(message);
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Friends, User: dhrosa, Code: 304, Status: 1 friend added }"));
		
		// Check for pushed friend message
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Friends, User: kemus, Code: 305 }"));
		
		// Start a conversation
		message = new Message();
		message.setType(Type.Start);
		message.setFriends(new String[] {"dhrosa", "kemus"});
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Add, Conversation ID: 0, User: dhrosa, Code: 601 }"));
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Add, Conversation ID: 0, User: kemus, Code: 601 }"));
		checkMessage(message = (Message)in1.readObject(), new Message());
		Integer ID = message.getID();
		assert(ID.equals(0));
		
		// Start message should be pushed to conversation members
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Start, Conversation ID: 0, Code: 602, Friends: {cjfman, } }"));
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Start, Conversation ID: 0, Code: 602, Friends: {dhrosa, cjfman, } }"));
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Add, Conversation ID: 0, User: kemus, Code: 601 }"));

		
		// Send a text message from user1 to the other two users
		message = new Message();
		message.setType(Type.Message);
		message.setID(ID);
		message.setStatus("Hello World");
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Message, Conversation ID: 0, Code: 400, Status: Message Received }"));
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Message, Conversation ID: 0, User: cjfman, Status: Hello World }"));
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Message, Conversation ID: 0, User: cjfman, Status: Hello World }"));
		
		// Send status message from cjfman
		message = new Message();
		message.setType(Type.Status);
		message.setCode(801);
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Status, User: cjfman, Code: 801 }"));
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Status, User: cjfman, Code: 801 }"));
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Status, User: cjfman, Code: 801 }"));

		// Logout first user
		message = new Message();
		message.setType(Type.Logout);
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Logout, Code: 200, Status: Good Bye cjfman }"));
		
		out1.close();
		in1.close();
		
		// Other users should be notified that the first user has left conversation and logged off
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Event, Conversation ID: 0, User: cjfman, Code: 704 }"));
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Event, User: cjfman, Code: 701, Status: cjfman has logged off }"));
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Event, Conversation ID: 0, User: cjfman, Code: 704 }"));
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Event, User: cjfman, Code: 701, Status: cjfman has logged off }"));
		
		// User2 sends message to same conversation
		// If user1 has been removed from the recipient list, code 400 should be returned
		// to user 2 and user3 should receive the message
		message = new Message();
		message.setType(Type.Message);
		message.setID(ID);
		message.setStatus("Hello World, Again!");
		out2.writeObject(message);
		assert(((Message)in2.readObject()).getCode() == 400);
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Message, Conversation ID: 0, User: dhrosa, Status: Hello World, Again! }"));
		
		// Abruptly disconnect user2
		out2.close();
		in2.close();
		
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Event, Conversation ID: 0, User: dhrosa, Code: 704 }"));
			
		// Abruptly disconnect user3
		out3.close();
		in3.close();
		
		server.kill();
	}
	
	/**
	 * This test logs on three different users, who then start three simultaneous conversations
	 * The three conversations should have unique IDs and messages should go to the appropriate recipients
	 * -Each user connects in order
	 * -Each user logs on in order
	 * -User1 starts a conversation with User2
	 * -User2 starts a conversation with User3
	 * -User3 starts a conversation with User1
	 * -User1 sends a message to User3
	 * -User2 sends a message to User1
	 * -User3 sends a message to User2
	 * -User1 sends an event message
	 * -All three users disconnect
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void testSequence2() throws IOException, ClassNotFoundException {
		Server server = new Server(4445, debug);
		server.start();
		
		String address = "localhost";
		
		// Connect first user
		Socket user1 = new Socket(address, 4445);
		ObjectInputStream in1 = new ObjectInputStream(user1.getInputStream());
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Response, Code: 100, Status: Welcome }"));
		ObjectOutputStream out1 = new ObjectOutputStream(user1.getOutputStream());

		// Connect second user
		Socket user2 = new Socket(address, 4445);
		ObjectInputStream in2 = new ObjectInputStream(user2.getInputStream());
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Response, Code: 100, Status: Welcome }"));
		ObjectOutputStream out2 = new ObjectOutputStream(user2.getOutputStream());

		// Connect third user
		Socket user3 = new Socket(address, 4445);
		ObjectInputStream in3 = new ObjectInputStream(user3.getInputStream());
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Response, Code: 100, Status: Welcome }"));
		ObjectOutputStream out3 = new ObjectOutputStream(user3.getOutputStream());	
		
		// Login first user
		Message message = new Message();
		message.setType(Type.Login);
		message.setUser("cjfman");
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Login, Code: 101, Status: Welcome cjfman }"));
		
		// Login second user
		message = new Message();
		message.setType(Type.Login);
		message.setUser("dhrosa");
		out2.writeObject(message);
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Login, Code: 101, Status: Welcome dhrosa }"));

		// Login third user
		message = new Message();
		message.setType(Type.Login);
		message.setUser("kemus");
		out3.writeObject(message);
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Login, Code: 101, Status: Welcome kemus }"));
		
		// Start a conversation cjfman >> dhrosa
		message = new Message();
		message.setType(Type.Start);
		message.setUser("dhrosa");
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Add, Conversation ID: 0, User: dhrosa, Code: 601 }"));
		checkMessage(message = (Message)in1.readObject(), new Message());
		Integer ID0 = message.getID();
		assert(ID0.equals(0));
		
		// Start message should be pushed to conversation member
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Start, Conversation ID: 0, Code: 602, Friends: {cjfman, } }"));

		
		// Start a conversation dhrosa >> kemus
		message = new Message();
		message.setType(Type.Start);
		message.setUser("kemus");
		out2.writeObject(message);
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Add, Conversation ID: 1, User: kemus, Code: 601 }"));
		checkMessage(message = (Message)in2.readObject(), new Message());
		Integer ID1 = message.getID();
		assert(ID1.equals(1));
		
		// Start message should be pushed to conversation members
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Start, Conversation ID: 1, Code: 602, Friends: {dhrosa, } }"));
		
		// Start a conversation kemus >> cjfman
		message = new Message();
		message.setType(Type.Start);
		message.setUser("cjfman");
		out3.writeObject(message);
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Add, Conversation ID: 2, User: cjfman, Code: 601 }"));
		checkMessage(message = (Message)in3.readObject(), new Message());
		Integer ID3 = message.getID();
		assert(ID3.equals(2));
		
		// Start message should be pushed to conversation members
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Start, Conversation ID: 2, Code: 602, Friends: {kemus, } }"));
		
		// Send a text message cjfman >> kemus
		message = new Message();
		message.setType(Type.Message);
		message.setID(ID3);
		message.setStatus("Hello World");
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Message, Conversation ID: 2, Code: 400, Status: Message Received }"));
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Message, Conversation ID: 2, User: cjfman, Status: Hello World }"));
		
		// Send a text message dhrosa >> cjfman
		message = new Message();
		message.setType(Type.Message);
		message.setID(ID0);
		message.setStatus("Hello MIT");
		out2.writeObject(message);
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Message, Conversation ID: 0, Code: 400, Status: Message Received }"));
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Message, Conversation ID: 0, User: dhrosa, Status: Hello MIT }"));
					
		// Send a text message kemus >> dhrosa
		message = new Message();
		message.setType(Type.Message);
		message.setID(ID1);
		message.setStatus("Hello Diony");
		out3.writeObject(message);
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Message, Conversation ID: 1, Code: 400, Status: Message Received }"));
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Message, Conversation ID: 1, User: kemus, Status: Hello Diony }"));
		
		// Send an event message to conversation ID0
		message = new Message();
		message.setType(Type.Event);
		message.setCode(703);
		message.setUser("Wrong User");
		message.setID(ID0);
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Event, Conversation ID: 0, User: cjfman, Code: 703 }"));
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Event, Conversation ID: 0, User: cjfman, Code: 703 }"));
		
		// Abruptly disconnect all users
		out1.close();
		in1.close();
		out2.close();
		in2.close();
		out3.close();
		in3.close();
				
		server.kill();
	}
	
	/**
	 * This test suite checks to make sure curtain types of illegal actions cannot be performed
	 * -All three users connect
	 * -User1 logs on
	 * -User2 tries to log on with User1's username
	 * -User2 logs on
	 * -User3 trys to send a message without logging on first
	 * -User3 logs on
	 * -User1 tries to log in again
	 * -User1 starts a conversation with User2
	 * -User3 attempts to send a message to a conversation he has not been added to
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	@Test
	public void testIllegalActions() throws IOException, ClassNotFoundException {
		Server server = new Server(4445, debug);
		server.start();
		
		String address = "localhost";
		
		// Connect first user
		Socket user1 = new Socket(address, 4445);
		ObjectInputStream in1 = new ObjectInputStream(user1.getInputStream());
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Response, Code: 100, Status: Welcome }"));
		ObjectOutputStream out1 = new ObjectOutputStream(user1.getOutputStream());

		// Connect second user
		Socket user2 = new Socket(address, 4445);
		ObjectInputStream in2 = new ObjectInputStream(user2.getInputStream());
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Response, Code: 100, Status: Welcome }"));
		ObjectOutputStream out2 = new ObjectOutputStream(user2.getOutputStream());
		
		// Connect third user
		Socket user3 = new Socket(address, 4445);
		ObjectInputStream in3 = new ObjectInputStream(user3.getInputStream());
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Response, Code: 100, Status: Welcome }"));
		ObjectOutputStream out3 = new ObjectOutputStream(user3.getOutputStream());
		
		// Login first user
		Message message = new Message();
		message.setType(Type.Login);
		message.setUser("cjfman");
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Login, Code: 101, Status: Welcome cjfman }"));
		
		// Login second user with username that is in use
		message = new Message();
		message.setType(Type.Login);
		message.setUser("cjfman");
		out2.writeObject(message);
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Error, Code: 502, Status: cjfman already logged in }"));
		
		// Login second user
		message = new Message();
		message.setType(Type.Login);
		message.setUser("dhrosa");
		out2.writeObject(message);
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Login, Code: 101, Status: Welcome dhrosa }"));
		
		// Third user has not logged in, tries to send event message
		message = new Message();
		message.setType(Type.Event);
		message.setCode(703);
		message.setID(0);
		out3.writeObject(message);
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Error, Code: 502, Status: Not logged in }"));

		// Login third user
		message = new Message();
		message.setType(Type.Login);
		message.setUser("kemus");
		out3.writeObject(message);
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Login, Code: 101, Status: Welcome kemus }"));
		
		// Login first user again
		message = new Message();
		message.setType(Type.Login);
		message.setUser("cjfman");
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Error, Code: 502, Status: Already logged in }"));
		
		// Start a conversation cjfman >> dhrosa
		message = new Message();
		message.setType(Type.Start);
		message.setUser("dhrosa");
		out1.writeObject(message);
		assert(checkMessage((Message)in1.readObject(), "MESSAGE { Type: Add, Conversation ID: 0, User: dhrosa, Code: 601 }"));
		checkMessage(message = (Message)in1.readObject(), new Message());
		Integer ID0 = message.getID();
		assert(ID0.equals(0));
		
		// Start message should be pushed to conversation member
		assert(checkMessage((Message)in2.readObject(), "MESSAGE { Type: Start, Conversation ID: 0, Code: 602, Friends: {cjfman, } }"));
		
		// Third user attempts to access a conversation that he does not have access to
		message = new Message();
		message.setType(Type.Message);
		message.setID(ID0);
		message.setStatus("Hello Diony");
		out3.writeObject(message);
		assert(checkMessage((Message)in3.readObject(), "MESSAGE { Type: Error, Code: 509, Status: ID does not match any of your conversations }"));
		
		server.kill();
	}
	
	/**
	 * This test attempts to connect a large number of people to the server
	 * Anyone after the first 500 connections will be rejected
	 * The connect users log on and every 5th user friends the following four users
	 */
	@Test
	public void stessTest() {
		try {
			Server server = new Server(4445, debug);
			server.start();
			
			String address = "localhost";
			
			int number = 1000;
			
			Socket user[] = new Socket[number];
			ObjectInputStream in[] = new ObjectInputStream[number];
			ObjectOutputStream out[] = new ObjectOutputStream[number];
			
			for (int i = 0; i < number; i++) {
				user[i] = new Socket(address, 4445);
				if (i < 500) {
					in[i] = new ObjectInputStream(user[i].getInputStream());
					assert(checkMessage((Message)in[i].readObject(), "MESSAGE { Type: Response, Code: 100, Status: Welcome }"));
					out[i] = new ObjectOutputStream(user[i].getOutputStream());
				} else
					try {
						in[i] = new ObjectInputStream(user[i].getInputStream());
						assert(false);
					} catch (Exception e2) {
					}
			}
			
			for (int i = 0; i < 500; i++) {
				Message message = new Message();
				message.setType(Type.Login);
				message.setUser(String.format("user%d", i));
				out[i].writeObject(message);
				assert(checkMessage((Message)in[i].readObject(), String.format("MESSAGE { Type: Login, Code: 101, Status: Welcome user%d }", i)));
			}
			
			for (int i = 0; i < 500; i = i + 5) {
				Message message = new Message();
				message.setType(Type.Friends);
				String[] friends = new String[4];
				for (int j = 0; j < 4; j++) {
					friends[j] = String.format("user%d", j+1);
				}
				message.setFriends(friends);
				out[i].writeObject(message);
				message = (Message)in[i].readObject();
				checkMessage(message, "");
				assert(message.getCode() == 303);
				for (int j = 1; j < 5; j++) {
					assert(checkMessage((Message)in[j].readObject(), String.format("MESSAGE { Type: Friends, User: user%d, Code: 305 }", i)));
				}
			}
			
			for (int i = 0; i < 500; i = i + 5) {
				
			}
			
			server.kill();
		} catch (Exception e) {
			System.out.println("Marco");
			assert(false);
		}
	}
	
	private boolean checkMessage(Message message, String check) {
		if (debug) System.out.println(message);
		return message.toString().equals(check);
	}
	
	private boolean checkMessage(Message message, Message check) {
		if (debug) System.out.println(message);
		return message.equals(check);
	}
}
