package main;

import arduino.ArduinoSerialConnection;
import dto.SetupDTO;
import logic.PDFViewerController;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.util.Defs;
import viewComponents.ScreenSetupFrame;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;


public class PechaKuchaMain {
    private JFileChooser chooser = new JFileChooser();

    private ArduinoSerialConnection arduinoConn;

    private JLabel timeRemainingLabel;

    private AtomicLong presStartTime,
            nextPageTime,
            nextSecTime,
            timeRemaining;

    private ExecutorService threadPool = Executors.newCachedThreadPool();

    private String pdfViewerType;


    public PechaKuchaMain(String pdfViewerType) {
        this.presStartTime = new AtomicLong(-1);
        this.nextPageTime = new AtomicLong(-1);
        this.nextSecTime = new AtomicLong(-1);
        this.timeRemaining = new AtomicLong(20);

        this.pdfViewerType = pdfViewerType;

        setup();
    }

    public PechaKuchaMain() {
        this("");
    }

    private void setup() {
        // Setup
        Defs.setSystemProperty("org.icepdf.core.views.background.color", "000000");

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "PDF-Documents", "pdf");
        chooser.setFileFilter(filter);

        SetupDTO settings = ScreenSetupFrame.getSettings();
        pdfViewerType = settings.getPdfViewer();

        System.out.println(pdfViewerType);

        PDFViewerController pdfViewerController = PDFViewerController.createNewControllersByName(pdfViewerType, 2);


        // arduino
        arduinoConn = new ArduinoSerialConnection();
        arduinoConn.initialize();
        arduinoConn.send("20");


        // presentation-frame
        JFrame presFrame = new JFrame("PechaKucha Presentation-Window");
        presFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        presFrame.getContentPane().add(pdfViewerController.getDocContainer(0));


        presFrame.pack();
        showOnScreen(settings.getPresentationScreen(), presFrame, true);


        // lookup-frame
        JFrame lookupFrame = new JFrame("PechaKucha Presentation-Tool");
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
            try {
                pdfViewerController.loadNewFile(openDocument(lookupFrame).getAbsolutePath());
            } catch (IOException | PDFException | PDFSecurityException | InterruptedException | ExecutionException err) {
                err.printStackTrace();
            }
            pdfViewerController.fitViewers();
        });
        openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        fileMenu.add(openFile);

        fileMenu.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> exit());
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
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
        showOnScreen(settings.getLookupScreen(), lookupFrame, false);

        presFrame.setVisible(true);
        lookupFrame.setVisible(true);


        Font font = timeRemainingLabel.getFont();
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        double fontSize = font.getSize() * ((double) screenWidth * 0.2 / (double) timeRemainingLabel.getFontMetrics(font).stringWidth("00"));
        timeRemainingLabel.setFont(timeRemainingLabel.getFont().deriveFont(Font.BOLD).deriveFont((float) fontSize));

        timePanel.setPreferredSize(new Dimension((int) (screenWidth * 0.2), lookupFrame.getHeight()));
        pdfViewerController.getDocContainer(1).setPreferredSize(new Dimension((int) (screenWidth * 0.75), lookupFrame.getHeight()));


        // timer
//        final long[] presStartTime = {-1L},
//                nextPageTime = {-1L},
//                nextSecTime = {-1L},
//                timeRemaining = {20};

        Timer t = new Timer(4, e -> {
            if (System.currentTimeMillis() >= nextPageTime.get()) {
                pdfViewerController.nextPage();
                nextPageTime.set(presStartTime.get() + ((pdfViewerController.getCurrentPageNumber() + 1) * 20_000L));
                timeRemaining.set(20);
            }

            if (System.currentTimeMillis() >= nextSecTime.get()) {
                String timeString = String.format("%02d", timeRemaining.get());
                updateSeconds(timeString);
//                timeRemaining[0]--;
                timeRemaining.decrementAndGet();
//                nextSecTime[0] = nextPageTime[0] - (timeRemaining[0] * 1_000);
                nextSecTime.set(nextPageTime.get() - (timeRemaining.get() * 1_000));
            }
        });


        // keyboard
        KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        kfm.addKeyEventDispatcher((KeyEvent e) -> {
            int kc = e.getKeyCode();

            if (e.getID() == KeyEvent.KEY_PRESSED && pdfViewerController.isDocumentSelected()) {
                if (kc == KeyEvent.VK_SPACE && !t.isRunning()) {
                    pdfViewerController.gotoFirst();

                    long pStart = System.currentTimeMillis();
                    presStartTime.set(pStart);
                    nextPageTime.set(pStart + 20_000);
                    nextSecTime.set(pStart + 1_000);

                    t.start();

                } else if (kc == KeyEvent.VK_R && t.isRunning()) {
                    t.stop();
                    pdfViewerController.gotoFirst();
                    timeRemainingLabel.setText("20");
                    arduinoConn.send("20");
                    timeRemaining.set(20);
                } else if ((kc == KeyEvent.VK_RIGHT || kc == KeyEvent.VK_DOWN || kc == KeyEvent.VK_PAGE_DOWN) && !t.isRunning()) {
                    pdfViewerController.nextPage();
                } else if ((kc == KeyEvent.VK_LEFT || kc == KeyEvent.VK_UP || kc == KeyEvent.VK_PAGE_UP) && !t.isRunning()) {
                    pdfViewerController.prevPage();
                }
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

    private File openDocument(JFrame parent) {
        int returnVal = chooser.showOpenDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    private void exit() {
        arduinoConn.close();
        System.exit(0);
    }

    private void updateSeconds(final String sec) {
//        System.out.printf("%d, %d, %d\n", System.currentTimeMillis(), nextSecTime.get(), presStartTime.get());

        threadPool.submit(() -> {
            timeRemainingLabel.setText(sec);
            timeRemainingLabel.paintImmediately(timeRemainingLabel.getVisibleRect());
        });

        threadPool.submit(() -> arduinoConn.send(sec));
    }

    public static void main(String[] args) {
        new PechaKuchaMain("pdftoppm-local");
    }
}