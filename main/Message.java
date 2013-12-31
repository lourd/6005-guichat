package main;

import java.io.Serializable;
/**
 * This is the Message class that will be available to both the Client and the Server. An instance of this class will be passed between the
 * client and server whenever they talk to each other by using an ObjectOutputStream.
 * 
 * The class is also made so that the server only has to forward message packets to anyone in a conversation, and doesn't have to reprocess them. This works with message and add packets.
 * 
 * @author Andres
 *
 */
public class Message implements Serializable {

    /**
     * This is to preserve the class ID no matter the java version, so that it can be serialized and de serialized correctly.
     */
    private static final long serialVersionUID = -2590495044050910239L;
    public enum Type {
    	Login,Logout,Friends,Start,Status,Event,Add,Message,Response,Error
    	};
    	
    private Type type;
    public Integer ConversationID = null;
    public String User = "";
    private int code = 0;
    public String Status = ""; // Also used for sending messages, or error
                               // clarifications, or events.
    public String[] Friends = new String[0]; // Used to send entire friend lists
                                             // over (or everyone who is online
                                             // depending on what we decide)

    /**
     * This constructor is for sending messages to the server
     * @param type
     * @param ID
     * @param User
     * @param Status
     */
    public Message() {}
    
    public void setID(Integer ID) {
        this.ConversationID = ID;
    }
    
    public int getID() {
    	return ConversationID;
    }
    
    public void setUser(String User) {
        this.User = User;
    }
    
    public String getUser() {
    	return User;
    }
    
    public void setStatus(String Status) {
        this.Status = Status;
    }
    
    public String getStatus() {
    	return Status;
    }
    
    public void setFriends(String[] Friends) {
        this.Friends = Friends;
    }
    
    public String[] getFriends() {
    	return this.Friends;
    }
    
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
    
    public void setType(String type) {
        this.type = Type.valueOf(type);
    }
    
    public void setCode(int code) {
    	this.code = code;
    }
    
    public int getCode() {
    	return this.code;
    }
    
    public boolean equals(Message message) {
    	boolean res = true;
    	try {
	    	res &= this.type.equals(message.getType());
	    	res &= this.ConversationID.equals(message.getID());
	    	res &= this.User.equals(message.getUser());
	    	res &= this.code == message.getCode();
	    	res &= this.Status.equals(message.getStatus());
	    	res &= this.Friends.equals(message.getFriends());
    	} catch (Exception e) {
    		return false;
    	}
    	return res;
    }
    
    public String toString() {
    	String res = "MESSAGE { Type: " + type;
    	if (ConversationID != null)
    		res += ", Conversation ID: " + ConversationID;
    	if (User.length() > 0)
    		res += ", User: " + User;
    	if (code > 0)
    		res += ", Code: " + code;
    	if (Status.length() > 0)
    		res += ", Status: " + Status;
    	if (Friends.length > 0) {
    		res += ", Friends: {";
    		for (String friend : Friends) {
    			res += friend + ", ";
    		}
    		res += "}";
    	}
    	res += " }";
    	return res;
    }
}
