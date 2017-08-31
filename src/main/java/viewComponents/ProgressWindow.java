package viewComponents;

import javax.swing.*;
import java.awt.*;

public class ProgressWindow extends JWindow {

    private final JProgressBar progressBar;

    public ProgressWindow(int max) {
        super();

        setSize(400, 250);

        Container root = getContentPane();
        root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));

        JLabel info = new JLabel("Loading PDF-File");

        Font font = info.getFont();
        double fontSize = font.getSize() * (380.0 / (double) info.getFontMetrics(font).stringWidth(info.getText()));
        info.setFont(info.getFont().deriveFont(Font.BOLD).deriveFont((float) fontSize));

        JLabel wait = new JLabel("Please wait...");

        root.add(info);
        root.add(wait);

        progressBar = new JProgressBar(SwingConstants.HORIZONTAL, 0, max);
        progressBar.setStringPainted(true);

        root.add(progressBar);

        setLocationRelativeTo(null);
        pack();
        setVisible(true);
        toFront();
        repaint();
    }

    public synchronized void increaseProgress() {
        progressBar.setValue(progressBar.getValue() + 1);
    }

    public void close() {
        dispose();
    }

}