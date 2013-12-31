package main;

import main.Message.Type;

/**
 * Easily accessed output message constructors for the GUI and model to use to add messages to the shared deque.
 * @author Andres
 *
 */
public class ClientMessageSender {
    public static Message CreateSentMessage(int ID, String User, String Status) {
        Message n = new Message();
        n.setID(ID);
        n.setStatus(Status);
        n.setUser(User);
        n.setType(Type.Message);
        return n;
    }
    
    public static Message CreateStatusMessage(String User, String Status) {
        Message n = new Message();
        n.setStatus(Status);
        n.setUser(User);
        n.setType(Type.Status);
        return n;
    }
    
    public static Message CreateFriendAddMessage(String User) {
        Message n = new Message();
        n.setUser(User);
        n.setType(Type.Friends);
        return n;
    }
    
    public static Message CreateLoginMessage(String User) {
        Message n = new Message();
        n.setUser(User);
        n.setType(Type.Login);
        return n;
    }
    
    public static Message CreateLogoutMessage(String User) {
        Message n = new Message();
        n.setUser(User);
        n.setType(Type.Logout);
        return n;
    }
    
    public static Message CreateStartMessage(String User) {
        Message n = new Message();
        n.setUser(User);
        n.setType(Type.Start);
        return n;
    }
    
    public static Message CreateStartMultipleMessage(String[] User) {
        Message n = new Message();
        n.setFriends(User);
        n.setType(Type.Start);
        return n;
    }
    
    public static Message CreateAddMessage(int ID,String User) {
        Message n = new Message();
        n.setID(ID);
        n.setUser(User);
        n.setType(Type.Add);
        return n;
    }
    
    public static Message CreateEnteredTextMessage(int ID,String User) {
        Message n = new Message();
        n.setID(ID);
        n.setUser(User);
        n.setCode(702);
        n.setType(Type.Event);
        return n;
    }
    
    public static Message CreateTypingTextMessage(int ID,String User) {
        Message n = new Message();
        n.setID(ID);
        n.setUser(User);
        n.setCode(703);
        n.setType(Type.Event);
        return n;
    }
    
    public static Message CreateLeftConversationMessage(int ID,String User) {
        Message n = new Message();
        n.setID(ID);
        n.setUser(User);
        n.setCode(704);
        n.setType(Type.Event);
        return n;
    }
    
    public static Message CreateClearedTextMessage(int ID,String User) {
        Message n = new Message();
        n.setID(ID);
        n.setUser(User);
        n.setCode(705);
        n.setType(Type.Event);
        return n;
    }
    
    public static Message CreateUserIdleMessage(String User) {
        Message n = new Message();
        n.setUser(User);
        n.setCode(710);
        n.setType(Type.Event);
        return n;
    }
    
    public static Message CreateUserActiveMessage(String User) {
        Message n = new Message();
        n.setUser(User);
        n.setCode(711);
        n.setType(Type.Event);
        return n;
    }
    
    public static Message CreateLeftMessage(int ID, String User) {
        Message n = new Message();
        n.setID(ID);
        n.setUser(User);
        n.setCode(704);
        n.setType(Type.Event);
        return n;
    }
}
