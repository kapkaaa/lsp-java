package view;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import config.DatabaseConfig;
import utils.FormatterUtils;

public class CurrencyRenderer extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table, Object value,
            boolean isSelected, boolean hasFocus,
            int row, int column) {

        super.getTableCellRendererComponent(
            table, value, isSelected, hasFocus, row, column
        );

        if (value instanceof Double) {
            setText(FormatterUtils.formatCurrency((Double) value));
            setHorizontalAlignment(SwingConstants.RIGHT);
        }
        return this;
    }
}
