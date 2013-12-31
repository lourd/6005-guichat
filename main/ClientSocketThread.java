package main;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;
import main.Message.Type;
import gui.ErrorWindow;
/**
 * This is the Client Model end that talks to the server, it is contact with the rest of the client through the ArrayDeque that gets filled with
 * outgoing messages.
 * @author Andres
 *
 */
public class ClientSocketThread implements Runnable{
    private ConcurrentLinkedQueue<Message> sendables;
    public final Socket socket;
    public final ClientModel model;
    public boolean Debug = false;
    
    public ClientSocketThread(Socket port, ConcurrentLinkedQueue<Message> deque, ClientModel model, boolean Debug) {
        this.socket = port;
        this.sendables = deque;
        this.model = model;
        this.Debug = Debug;
    }
    
    /**
     * Deals with sending messages from the deque and receiving messages from the server.
     */
    @Override
    public void run() {
        ObjectInputStream in = null;
        ObjectOutputStream out = null;
        try {
            in = new ObjectInputStream(this.socket.getInputStream());
            out = new ObjectOutputStream(this.socket.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            while (true) {
                // If there is something to send, send it.
                if (!this.sendables.isEmpty()) {
                    Message output = this.sendables.remove();
                    if (this.Debug) {
                       System.out.println("Sending" + output);
                    }
                    out.writeObject(output);
                    out.flush();
                }
                // If there is something to read, read it, then go to the HandleRequest function
                if (this.socket.getInputStream().available() > 0) {
                    Message line = (Message) in.readObject();   
                    if (line.equals(null)) {
                        break;
                    }
                    else {
                        HandleRequest(line);
                    }
                    if (this.Debug) {
                        System.out.println("Received" + line);
                    }
                }
            }
            out.close();
            in.close();
            this.socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Throwable e) {
                e.printStackTrace();
            }
    }
    
    private void HandleRequest(Message input) {
        //Call the appropriate model function for each case.
        switch(input.getType()) {
        case Login:
            model.login();
            break;
        case Logout:
            model.logout();
            break;
        case Friends:
            model.friends(input);
            break;
        case Start:
            model.startConversation(input);
            break;
        case Message:
            model.updateConversation(input);
            break;
        case Add:
            model.addToConversation(input);
            break;
        case Event:
            model.event(input);
            break;
        case Status:
            model.status(input);
            break;
        case Error:
            if (input.getCode() != 509) {
                ErrorWindow.makeError("Error", input.Status);   
            }
            // If they are login errors, close the entire application to stop the background thread.
            if (input.getCode() == 501 | input.getCode() == 502) {
                System.exit(0);
            }
            break;
        case Response:
            break;
        default:
            break;
        }
    }
}
