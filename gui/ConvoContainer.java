package gui;

import java.awt.GridLayout;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * Container for tabs
 * @author descioli
 */
public class ConvoContainer extends JPanel {
    
    public JTabbedPane tabPane;
    
    public ConvoContainer(JFrame frame, JTabbedPane tabPane) {
        super(new GridLayout(1, 1));
        this.add(tabPane);
    }
}
