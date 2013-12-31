package main;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.Test;

/**
 * Unit tests for Client
 * @author descioli
 */
public class ClientModelTest {

    @Test
    public void testLogin() throws UnknownHostException, IOException, InterruptedException {
        ObjectInputStream in;
        ObjectOutputStream out = null;
        
        ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<Message>(); 
        ClientModel model = new ClientModel("Andres", messages);
        model.Debug = true;
        
        
        ServerSocket dummyServer = new ServerSocket(4445);
        System.out.println("Server Started");        
        Thread client = new Thread(new ClientSocketThread(new Socket("localhost",4445), messages, model, true));
        System.out.println("Starting Dealer");
        client.start();
        System.out.println("Dealer Started");
        Socket dummy = dummyServer.accept();
        
        synchronized (dummy) {
            try {
                out = new ObjectOutputStream(dummy.getOutputStream());
                in = new ObjectInputStream(dummy.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        Message n = ClientMessageSender.CreateLoginMessage("Andres");        
        out.writeObject(n);
        n = ClientMessageSender.CreateLogoutMessage("Andres");        
        out.writeObject(n);
        
        int count = 0;
        while (model.Debug) {
           if (count == 10) {
               assert(false);
           }
           count++;
           Thread.sleep(100);
        }
        assertEquals("[Login, Logout]",model.Tester.toString());
    }

    @Test
    public void testMessages() throws UnknownHostException, IOException, InterruptedException {
        ObjectInputStream in;
        ObjectOutputStream out = null;
        
        ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<Message>(); 
        ClientModel model = new ClientModel("Andres", messages);
        model.Debug = true;
        
        
        ServerSocket dummyServer = new ServerSocket(4446);
        System.out.println("Server Started");        
        Thread client = new Thread(new ClientSocketThread(new Socket("localhost",4446), messages, model, true));
        System.out.println("Starting Dealer");
        client.start();
        System.out.println("Dealer Started");
        Socket dummy = dummyServer.accept();
        
        synchronized (dummy) {
            try {
                out = new ObjectOutputStream(dummy.getOutputStream());
                in = new ObjectInputStream(dummy.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        Message n = ClientMessageSender.CreateLoginMessage("Andres");        
        out.writeObject(n);
        
        n = ClientMessageSender.CreateFriendAddMessage("Charles");
        n.setCode(303);
        out.writeObject(n);
        
        n = ClientMessageSender.CreateStartMessage("Charles");
        n.setCode(601);
        n.setID(10);
        out.writeObject(n);
        
        n = ClientMessageSender.CreateSentMessage(10, "Charles", "Hi Bro");
        out.writeObject(n);
        
        n = ClientMessageSender.CreateFriendAddMessage("Louis");
        n.setCode(303);
        out.writeObject(n);
        
        n = ClientMessageSender.CreateLogoutMessage("Andres");        
        out.writeObject(n);
        
        int count = 0;
        while (model.Debug) {
           if (count == 10) {
               assert(false);
           }
           count++;
           Thread.sleep(100);
        }
        assertEquals("[Login, Friends: Charles, Start: 10, Update: Hi Bro, Friends: Louis, Logout]",model.Tester.toString());
    }
    
    @Test
    public void testMoreMessages() throws UnknownHostException, IOException, InterruptedException {
        ObjectInputStream in;
        ObjectOutputStream out = null;
        
        ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<Message>(); 
        ClientModel model = new ClientModel("Andres", messages);
        model.Debug = true;
        
        
        ServerSocket dummyServer = new ServerSocket(4447);
        System.out.println("Server Started");        
        Thread client = new Thread(new ClientSocketThread(new Socket("localhost",4447), messages, model, true));
        System.out.println("Starting Dealer");
        client.start();
        System.out.println("Dealer Started");
        Socket dummy = dummyServer.accept();
        
        synchronized (dummy) {
            try {
                out = new ObjectOutputStream(dummy.getOutputStream());
                in = new ObjectInputStream(dummy.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        Message n = ClientMessageSender.CreateLoginMessage("Andres");        
        out.writeObject(n);
        
        n = ClientMessageSender.CreateFriendAddMessage("Charles");
        n.setCode(303);
        out.writeObject(n);
        
        n = ClientMessageSender.CreateFriendAddMessage("Louis");
        n.setCode(303);
        out.writeObject(n);
        
        n = ClientMessageSender.CreateStartMessage("Charles");
        n.setCode(601);
        n.setID(10);
        out.writeObject(n);
        
        n = ClientMessageSender.CreateAddMessage(10, "Louis");
        out.writeObject(n);
        
        n = ClientMessageSender.CreateTypingTextMessage(10, "Charles");
        out.writeObject(n);
        
        n = ClientMessageSender.CreateEnteredTextMessage(10, "Charles");
        out.writeObject(n);
        
        n = ClientMessageSender.CreateSentMessage(10, "Charles", "Hi Bro");
        out.writeObject(n);
        
        n = ClientMessageSender.CreateSentMessage(10, "Louis", "Sorry I'm late");
        out.writeObject(n);
        
        n = ClientMessageSender.CreateSentMessage(10, "Louis", "Sorry I'm late");
        out.writeObject(n);
        
        n = ClientMessageSender.CreateLogoutMessage("Andres");        
        out.writeObject(n);
        
        int count = 0;
        while (model.Debug) {
           if (count == 10) {
               assert(false);
           }
           count++;
           Thread.sleep(100);
        }
        assertEquals("[Login, Friends: Charles, Friends: Louis, Start: 10, Event: Charles, Event: Charles, Update: Hi Bro, Update: Sorry I'm late, Update: Sorry I'm late, Logout]",model.Tester.toString());
    }
    
    @Test
    public void testMessageFromAWrongConversation() throws UnknownHostException, IOException, InterruptedException {
        ObjectInputStream in;
        ObjectOutputStream out = null;
        
        ConcurrentLinkedQueue<Message> messages = new ConcurrentLinkedQueue<Message>(); 
        ClientModel model = new ClientModel("Andres", messages);
        model.Debug = true;
        
        
        ServerSocket dummyServer = new ServerSocket(4448);
        System.out.println("Server Started");        
        Thread client = new Thread(new ClientSocketThread(new Socket("localhost",4448), messages, model, true));
        System.out.println("Starting Dealer");
        client.start();
        System.out.println("Dealer Started");
        Socket dummy = dummyServer.accept();
        
        synchronized (dummy) {
            try {
                out = new ObjectOutputStream(dummy.getOutputStream());
                in = new ObjectInputStream(dummy.getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        Message n = ClientMessageSender.CreateLoginMessage("Andres");        
        out.writeObject(n);
        
        n = ClientMessageSender.CreateTypingTextMessage(10, "Charles");
        out.writeObject(n);
        
        n = ClientMessageSender.CreateSentMessage(10, "Louis", "Hi!");
        out.writeObject(n);
        
        n = ClientMessageSender.CreateLogoutMessage("Andres");        
        out.writeObject(n);
        
        int count = 0;
        while (model.Debug) {
           if (count == 10) {
               assert(false);
           }
           count++;
           Thread.sleep(100);
        }
        assertEquals("[Login, Logout]",model.Tester.toString());
    }
}

