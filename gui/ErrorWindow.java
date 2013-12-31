package gui;

import javax.swing.JOptionPane;
/**
 * Very simple GUI for showing error messages (can be called before big GUI is started)
 * @author Andres
 *
 */
public class ErrorWindow {

    public static void makeError(String title, String text){
        JOptionPane.showMessageDialog(null, 
                text, title, 
                JOptionPane.ERROR_MESSAGE);
    }
}
