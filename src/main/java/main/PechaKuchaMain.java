package main;

import arduino.ArduinoSerialConnection;
import logic.PDFViewerController;
import logic.ScreenSetupFrame;
import org.icepdf.core.util.Defs;

import javax.swing.*;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;


public class PechaKuchaMain {
    private static JFileChooser chooser = new JFileChooser();

    private static ArduinoSerialConnection arduinoConn;

    private static final String PORT_NAMES[] = {
            "/dev/tty.usbmodem", // Mac OS X
            "/dev/usbdev", // Linux
            "/dev/tty", // Linux
            "/dev/serial", // Linux
            "COM3", // Windows
    };
    private static JLabel timeRemainingLabel;

    public static void main(String[] args) {
        // Setup
        String filePath = "/home/gereon/UNI/SoSe 2017/IG/Folien/UE_01_WissArb1.pdf";
        Defs.setSystemProperty("org.icepdf.core.views.background.color", "000000");

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "PDF-Documents", "pdf");
        chooser.setFileFilter(filter);

        PDFViewerController pdfViewerController = new PDFViewerController();

        pdfViewerController.createNewController();
        pdfViewerController.createNewController();

        GraphicsDevice[] graphicsSettings = ScreenSetupFrame.getSettings();


        // arduino
        arduinoConn = new ArduinoSerialConnection();
        arduinoConn.initialize();
        arduinoConn.send("20");


        // presentation-frame
        JFrame presFrame = new JFrame();
        presFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        presFrame.getContentPane().add(pdfViewerController.getDocContainer(0));


        presFrame.pack();
        showOnScreen(graphicsSettings[0], presFrame, true);


        // lookup-frame
        JFrame lookupFrame = new JFrame("PechaKucha");
        Container lookupPane = lookupFrame.getContentPane();
        lookupPane.setLayout(new BoxLayout(lookupPane, BoxLayout.X_AXIS));

        timeRemainingLabel = new JLabel("20");
        timeRemainingLabel.setForeground(Color.WHITE);
        timeRemainingLabel.setBackground(Color.BLACK);

        JPanel timePanel = new JPanel();
        timePanel.setBackground(Color.BLACK);
        timePanel.add(timeRemainingLabel);

        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Files");
        menuBar.add(fileMenu);

        JMenuItem openFile = new JMenuItem("Open File");
        openFile.addActionListener(e -> {
            pdfViewerController.loadNewFile(openDocument(lookupFrame).getAbsolutePath());
            pdfViewerController.fitViewers();
        });
        openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        fileMenu.add(openFile);

        fileMenu.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> exit());
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
        fileMenu.add(exit);


        JMenu presMenu = new JMenu("Presentation");
        menuBar.add(presMenu);

        JMenuItem startPres = new JMenuItem("Start Presentation");
        startPres.setMnemonic(KeyEvent.VK_SPACE);
        presMenu.add(startPres);

        JMenuItem resetPres = new JMenuItem("Reset Presentation");
        resetPres.setMnemonic(KeyEvent.VK_R);
        presMenu.add(resetPres);

        lookupFrame.setJMenuBar(menuBar);


        lookupPane.add(pdfViewerController.getDocContainer(1));
        lookupPane.add(timePanel);

        lookupFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        lookupFrame.pack();
        showOnScreen(graphicsSettings[1], lookupFrame, false);

        presFrame.setVisible(true);
        lookupFrame.setVisible(true);


        Font font = timeRemainingLabel.getFont();
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        double fontSize = font.getSize() * ((double) screenWidth * 0.2 / (double) timeRemainingLabel.getFontMetrics(font).stringWidth("00"));
        timeRemainingLabel.setFont(timeRemainingLabel.getFont().deriveFont(Font.BOLD).deriveFont((float) fontSize));

        timePanel.setPreferredSize(new Dimension((int) (screenWidth * 0.2), lookupFrame.getHeight()));
        pdfViewerController.getDocContainer(1).setPreferredSize(new Dimension((int) (screenWidth * 0.75), lookupFrame.getHeight()));


        // timer
        final long[] presStartTime = {-1L},
                nextPageTime = {-1L},
                nextSecTime = {-1L},
                timeRemaining = {20};

        Timer t = new Timer(3, e -> {
            if (System.currentTimeMillis() >= nextPageTime[0]) {
                pdfViewerController.nextPage();
                nextPageTime[0] = presStartTime[0] + ((pdfViewerController.getCurrentPageNumber() + 1) * 20_000L);
                timeRemaining[0] = 20;
            }

            if (System.currentTimeMillis() >= nextSecTime[0]) {
                String timeString = String.format("%02d", timeRemaining[0]);
                updateSeconds(timeString);
                timeRemaining[0]--;
                nextSecTime[0] = nextPageTime[0] - (timeRemaining[0] * 1_000);
            }

        });


        // keyboard
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher((KeyEvent e) -> {
            if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_SPACE && !t.isRunning()) {
                t.start();
                presStartTime[0] = System.currentTimeMillis();
                nextPageTime[0] = presStartTime[0] + 20_000;
                nextSecTime[0] = presStartTime[0] + 1_000;
            } else if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_R && t.isRunning()) {
                t.stop();
                pdfViewerController.gotoFirst();
                timeRemainingLabel.setText("20");
                arduinoConn.send("20");
                timeRemaining[0] = 20;
            }

            return false;
        });
    }

    private static void showOnScreen(GraphicsDevice graphicsDevice, JFrame frame, boolean fullscreen) {
        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(frame);
        } else {
            frame.setLocation(graphicsDevice.getDefaultConfiguration().getBounds().x,
                    graphicsDevice.getDefaultConfiguration().getBounds().y + frame.getY());
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    private static File openDocument(JFrame parent) {
        int returnVal = chooser.showOpenDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    private static void exit() {
        arduinoConn.close();
        System.exit(0);
    }

    private static void updateSeconds(String sec) {
        EventQueue.invokeLater(() -> timeRemainingLabel.setText(sec));
        arduinoConn.send(sec);
    }
}