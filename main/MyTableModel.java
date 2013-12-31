package main;

import java.util.Vector;

import javax.swing.table.DefaultTableModel;

/**
 * A subclass of DefaultTableModel to make it uneditable
 * 
 * @author LDeSci
 * 
 */
public class MyTableModel extends DefaultTableModel {

    /**
     * Constructs a default DefaultTableModel which is a table of zero columns
     * and zero rows.
     */
    public MyTableModel() {
        super();
    }

    /**
     * Constructs a DefaultTableModel with rowCount and colCount of null object
     * values.
     * 
     * @param rowCount
     *            - the number of rows the table holds
     * @param colCount
     *            - the number of columns the table holds
     */
    public MyTableModel(int rowCount, int colCount) {
        super(rowCount, colCount);
    }

    /**
     * Constructs a DefaultTableModel with as many columns as there are elements
     * in columnNames and rowCount of null object values. Each column's name
     * will be taken from the columnNames vector.
     * 
     * @param columnNames
     *            - vector containing the names of the new columns; if this is
     *            null then the model has no columns
     * @param rowCount
     *            - the number of rows the table holds
     */
    public MyTableModel(Vector<?> columnNames, int rowCount) {
        super(columnNames, rowCount);
    }

    /**
     * Constructs a DefaultTableModel and initializes the table by passing data
     * and columnNames to the setDataVector method.
     * 
     * @param data
     *            - the data of the table, a Vector of Vectors of Object values
     * @param columnNames
     *            - vector containing the names of the new columns
     */
    public MyTableModel(Vector<?> data, Vector<?> columnNames) {
        super(data, columnNames);
    }

    /**
     * Constructs a DefaultTableMOdel and initializes the table by passing data
     * and columnNames to the setDataVector method. The first index in the
     * Object[][] array is the row index and the second is the column index.
     * 
     * @param data - the data of the table
     * @param columnNames - the names of the columns 
     */
    public MyTableModel(Object[][] data, Object[] columnNames) {
        super(data, columnNames);
    }

    @Override
    public boolean isCellEditable(int col, int row) {
        return false;
    }
}
