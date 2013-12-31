package gui;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

/**
 * This will be needed if we want to update the text for when people
 * are away.
 * @author descioli
 *
 */
public class MyListRenderer extends JLabel implements ListCellRenderer{

    public MyListRenderer(){
        setOpaque(true);
    }
    
    @Override
    public Component getListCellRendererComponent(JList list, Object value,
            int index, boolean isSelected, boolean cellHasFocus) {
        // TODO Auto-generated method stub
        return null;
    }

}
