package com.dabomstew.pkrandom.gui;

import javax.swing.*;
import java.awt.*;

public class PrefixTextField extends JTextField {

    private final String prefixText;

    public PrefixTextField(String prefixText) {
        this.prefixText = prefixText;
        int prefixWidth = getFontMetrics(getFont()).stringWidth(prefixText);
        setMargin(new Insets(2, prefixWidth + 2, 2, 2)); // Set the left margin for user text
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Draw the prefix text
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setColor(Color.GRAY);  // Gray color for the prefix
        g2.setFont(getFont());    // Use the same font as the text field

        // Draw the prefix at the left side of the text field (without offset)
        g2.drawString(prefixText, 2, g.getFontMetrics().getMaxAscent() + getInsets().top);
        g2.dispose();
    }

    @Override
    public void setText(String t) {
        super.setText(t);  // Directly set the text without the prefix
    }

    @Override
    public String getText() {
        return super.getText();  // Return the user input text
    }

}
