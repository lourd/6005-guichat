package main;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps track of the text, Users and status of a conversation.
 * @author Andres
 *
 */
public class ClientConversation {
    public StringBuffer buff;
    public ConcurrentHashMap<String,String> UserStatus;
    
    public ClientConversation(String[] Users) {
        this.buff = new StringBuffer();
        this.UserStatus = new ConcurrentHashMap<String,String>();
        for (String i : Users) {
            this.UserStatus.put(i,"");
        }
        
    }
    
    public void AddUser(String User) {
        if (!this.UserStatus.containsKey(User)) {
            this.UserStatus.put(User, "");   
        }
    }
    
    public void RemoveUser(String User) {
        this.UserStatus.remove(User);
    }
    
    public void UpdateConversation(String User, String Message) {
        this.buff.append("\n" + User + ": " + Message);
    }
    
    public void ChangeStatus(String User, String Status) {
        this.UserStatus.put(User, Status);
    }
}