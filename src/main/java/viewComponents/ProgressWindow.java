package viewComponents;

import javax.swing.*;
import java.awt.*;

/*
    PechaKuchaTool -- Supports displaying Pecha Kucha-Presentations
    Copyright (C) 2017  Gereon Dusella

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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