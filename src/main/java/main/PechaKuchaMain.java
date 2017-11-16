package main;

import arduino.ArduinoSerialConnection;
import dto.SetupDTO;
import logic.NoGraphicsDevice;
import logic.PDFViewerController;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.util.Defs;
import org.jnativehook.GlobalScreen;
import org.jnativehook.NativeHookException;
import org.jnativehook.keyboard.NativeKeyEvent;
import org.jnativehook.keyboard.NativeKeyListener;
import viewComponents.ScreenSetupFrame;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

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

/**
 * This is the main class of the PechaKuchaTool. It contains the startup-routines as well as the logic for building the frames.
 */
public class PechaKuchaMain {
    /**
     * A {@link JFileChooser} for picking the files to load.
     */
    private JFileChooser chooser = new JFileChooser();

    /**
     * The {@link ArduinoSerialConnection} for connecting to an Arduino.
     */
    private ArduinoSerialConnection arduinoConn;

    /**
     * The {@link JLabel} which contains the remaining seconds.
     */
    private JLabel timeRemainingLabel;

    /**
     * The {@link JFrame} which contains the Presentation Screen-Components.
     */
    private JFrame presFrame;

    /**
     * The {@link JFrame} which contains the Lookup Screen-Components.
     */
    private JFrame lookupFrame;

    /**
     * The {@link System#currentTimeMillis()} of the presentations' start.
     */
    private AtomicLong presStartTime;

    /**
     * The UNIX-Time of the next page-change.
     */
    private AtomicLong nextPageTime;

    /**
     * The UNIX-Time of the next seconds-change on the timerRemainingLabel.
     */
    private AtomicLong nextSecTime;

    /**
     * The current displayed value on the timeRemainingLabel.
     */
    private AtomicInteger timeRemaining;

    /**
     * A thread-pool for asynchronous tasks.
     */
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    /**
     * The settings of the current launch, usually fetched by {@link ScreenSetupFrame#getSettings()} and wrapped inside a {@link SetupDTO}.
     */
    private SetupDTO settings;

    /**
     * The {@link PDFViewerController}.
     */
    private PDFViewerController pdfViewerController;

    /**
     * The timer for managing the slide change and remaining seconds-times.
     */
    private Timer t;

    /**
     * A Keyboard-Hook for controlling the program.
     */
    private NativeKeyListener nkl;

    /**
     * The Logger.
     */
    private Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());

    /**
     * Default constructor for running the PechaKuchaTool.
     *
     * @param settings The settings of the current launch.
     */
    public PechaKuchaMain(SetupDTO settings) {
        this.presStartTime = new AtomicLong(-1);
        this.nextPageTime = new AtomicLong(-1);
        this.nextSecTime = new AtomicLong(-1);
        this.timeRemaining = new AtomicInteger(20);

        this.settings = settings;

        setup();
    }

    /**
     * Constructor allowing to set an initial file which is then loaded after the program has been started successfully.
     *
     * @param settings The settings of the current launch.
     * @param path     The path of a to-load file.
     */
    public PechaKuchaMain(SetupDTO settings, String path) {
        this.presStartTime = new AtomicLong(-1);
        this.nextPageTime = new AtomicLong(-1);
        this.nextSecTime = new AtomicLong(-1);
        this.timeRemaining = new AtomicInteger(20);

        this.settings = settings;

        setup(path);
    }

    /**
     * the setup-routine
     */
    private void setup() {
        // Setup
        Defs.setSystemProperty("org.icepdf.core.views.background.color", "000000");

        FileNameExtensionFilter filter = new FileNameExtensionFilter(
                "PDF-Documents", "pdf");
        chooser.setFileFilter(filter);


        pdfViewerController = PDFViewerController.createNewControllersByName(settings.getPdfViewer(), 2);


        // arduino
        arduinoConn = new ArduinoSerialConnection(settings.isEstablishArduinoConn());
        arduinoConn.initialize();
        arduinoConn.send("20");


        // presentation-frame
        presFrame = new JFrame("PechaKucha Presentation-Window");
        presFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        presFrame.getContentPane().add(pdfViewerController.getDocContainer(0));

        if (!(settings.getPresentationScreen() instanceof NoGraphicsDevice)) {
            presFrame.pack();
            showOnScreen(settings.getPresentationScreen(), presFrame, true);
            presFrame.setVisible(true);
        }


        // lookup-frame
        lookupFrame = new JFrame("PechaKucha Presentation-Tool");
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
        });
        openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        fileMenu.add(openFile);

        fileMenu.addSeparator();

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e -> exit());
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
        fileMenu.add(exit);


        JMenu presMenu = new JMenu("Presentation");
        menuBar.add(presMenu);

        JMenuItem startPres = new JMenuItem("Start Presentation");
        startPres.setMnemonic(KeyEvent.VK_SPACE);
        presMenu.add(startPres);

        JMenuItem resetPres = new JMenuItem("Reset Presentation");
        resetPres.setMnemonic(KeyEvent.VK_R);
        presMenu.add(resetPres);

        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);

        JMenuItem fitPage = new JMenuItem("Fit Page");
        fitPage.setMnemonic(KeyEvent.VK_F);
        fitPage.addActionListener(e -> pdfViewerController.fitViewers());
        viewMenu.add(fitPage);

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e -> showAboutText());
        helpMenu.add(about);


        lookupFrame.setJMenuBar(menuBar);


        lookupPane.add(pdfViewerController.getDocContainer(1));
        lookupPane.add(timePanel);

        lookupFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        lookupFrame.pack();
        showOnScreen(settings.getLookupScreen(), lookupFrame, false);

        lookupFrame.setVisible(true);

        lookupFrame.requestFocus();


        Font font = timeRemainingLabel.getFont();
        int screenWidth = Toolkit.getDefaultToolkit().getScreenSize().width;
        double fontSize = font.getSize() * ((double) screenWidth * 0.2 / (double) timeRemainingLabel.getFontMetrics(font).stringWidth("00"));
        timeRemainingLabel.setFont(timeRemainingLabel.getFont().deriveFont(Font.BOLD).deriveFont((float) fontSize));

        timePanel.setPreferredSize(new Dimension((int) (screenWidth * 0.2), lookupFrame.getHeight()));
        pdfViewerController.getDocContainer(1).setPreferredSize(new Dimension((int) (screenWidth * 0.75), lookupFrame.getHeight()));

        // timer

        t = new Timer(4, e -> {
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

        nkl = new NativeKeyListener() {
            @Override
            public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
                int kc = nativeKeyEvent.getKeyCode();

                if (pdfViewerController.isDocumentSelected() && isFocussedApplication()) {
                    if (kc == NativeKeyEvent.VC_SPACE && !t.isRunning()) {
                        startPresentation();
                    } else if (kc == NativeKeyEvent.VC_R && t.isRunning()) {
                        resetPresentation();
                    } else if ((kc == NativeKeyEvent.VC_RIGHT || kc == NativeKeyEvent.VC_DOWN || kc == NativeKeyEvent.VC_PAGE_DOWN) && !t.isRunning()) {
                        pdfViewerController.nextPage();
                    } else if ((kc == NativeKeyEvent.VC_LEFT || kc == NativeKeyEvent.VC_UP || kc == NativeKeyEvent.VC_PAGE_UP) && !t.isRunning()) {
                        pdfViewerController.prevPage();
                    } else if (kc == NativeKeyEvent.VC_F) {
                        pdfViewerController.fitViewers();
                    }
                }
            }

            @Override
            public void nativeKeyPressed(NativeKeyEvent nativeKeyEvent) {
                nativeKeyTyped(nativeKeyEvent);
            }

            @Override
            public void nativeKeyReleased(NativeKeyEvent nativeKeyEvent) {

            }
        };


        logger.setLevel(Level.WARNING);

        try {
            GlobalScreen.registerNativeHook();
            GlobalScreen.addNativeKeyListener(nkl);
        } catch (NativeHookException ex) {
            System.err.println("There was a problem registering the native hook. Using default hooks with reduced functionality instead.");
            System.err.println(ex.getMessage());

            KeyEventDispatcher keyEventDispatcher = e -> {
                int kc = e.getKeyCode();

                if (e.getID() == KeyEvent.KEY_PRESSED && pdfViewerController.isDocumentSelected()) {
                    if (kc == KeyEvent.VK_SPACE && !t.isRunning()) {
                        startPresentation();
                    } else if (kc == KeyEvent.VK_R && t.isRunning()) {
                        resetPresentation();
                    } else if ((kc == KeyEvent.VK_RIGHT || kc == KeyEvent.VK_DOWN || kc == KeyEvent.VK_PAGE_DOWN) && !t.isRunning()) {
                        pdfViewerController.nextPage();
                    } else if ((kc == KeyEvent.VK_LEFT || kc == KeyEvent.VK_UP || kc == KeyEvent.VK_PAGE_UP) && !t.isRunning()) {
                        pdfViewerController.prevPage();
                    } else if (kc == KeyEvent.VK_F) {
                        pdfViewerController.fitViewers();
                    }
                }

                return false;
            };

            KeyboardFocusManager kfm = KeyboardFocusManager.getCurrentKeyboardFocusManager();

            kfm.addKeyEventDispatcher(keyEventDispatcher);
        }
    }

    /**
     * Overlaoded setup-method which additionally loads a file right at the end.
     *
     * @param path The path to the file to load.
     */
    private void setup(String path) {
        setup();

        try {
            pdfViewerController.loadNewFile(path);
        } catch (IOException | PDFException | PDFSecurityException | InterruptedException | ExecutionException e) {
            e.printStackTrace();

        }
    }

    /**
     * Sets the screen on which a frame should be shown.
     *
     * @param graphicsDevice the screen
     * @param frame          the frame to operate on.
     * @param fullscreen     Should teh frame be displayed in fullscreen-mode or borderless fullscreen-mode.
     */
    private static void showOnScreen(GraphicsDevice graphicsDevice, JFrame frame, boolean fullscreen) {
        if (fullscreen) {
            graphicsDevice.setFullScreenWindow(frame);
        } else {
            frame.setLocation(graphicsDevice.getDefaultConfiguration().getBounds().x,
                    graphicsDevice.getDefaultConfiguration().getBounds().y + frame.getY());
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
        }
    }

    /**
     * Start a presentation.
     */
    private void startPresentation() {
        pdfViewerController.gotoFirst();

        long pStart = System.currentTimeMillis();
        presStartTime.set(pStart);
        nextPageTime.set(pStart + 20_000);
        nextSecTime.set(pStart + 1_000);

        t.start();
    }

    /**
     * Reset the presentation.
     */
    private void resetPresentation() {
        t.stop();
        pdfViewerController.gotoFirst();
        timeRemainingLabel.setText("20");
        arduinoConn.send("20");
        timeRemaining.set(20);
    }

    /**
     * Select a file via the {@link JFileChooser}
     *
     * @param parent the frame of the {@link JFileChooser}
     * @return the File
     */
    private File openDocument(JFrame parent) {
        int returnVal = chooser.showOpenDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        } else {
            return null;
        }
    }

    /**
     * Close this program
     */
    private void exit() {
        arduinoConn.close();
        try {
            GlobalScreen.unregisterNativeHook();
        } catch (NativeHookException e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    /**
     * Update the remaining seconds
     *
     * @param sec the seconds remaining as {@link String}
     */
    private void updateSeconds(final String sec) {
//        System.out.printf("%d, %d, %d\n", System.currentTimeMillis(), nextSecTime.get(), presStartTime.get());

        threadPool.submit(() -> {
            timeRemainingLabel.setText(sec);
            timeRemainingLabel.paintImmediately(timeRemainingLabel.getVisibleRect());
        });

        threadPool.submit(() -> arduinoConn.send(sec));
    }

    private boolean isFocussedApplication() {
        boolean presFrameFocus = false;

        if (!(settings.getPresentationScreen() instanceof NoGraphicsDevice)) {
            presFrameFocus = presFrame.isFocused();
        }

        return lookupFrame.isFocused() || presFrameFocus;
    }

    /**
     * The main-method
     *
     * @param args a probably chosen file at position 0
     */
    public static void main(String[] args) {
        SetupDTO settings = ScreenSetupFrame.getSettings();

        if (args.length == 0) {
            new PechaKuchaMain(settings);
        } else {
            new PechaKuchaMain(settings, args[0]);
        }
    }

    /**
     * Displays an "About"-text
     */
    private void showAboutText() {
        StringBuilder msg = new StringBuilder();
        msg.append("<html>")
                .append("<h1>PechaKuchaTool</h1><h2>Supports displaying Pecha Kucha-Presentations</h2>")
                .append("<h3>Copyright (C) 2017  Gereon Dusella</h3>")
                .append("<p>This program is free software: you can redistribute it and/or modify<br>")
                .append("it under the terms of the GNU General Public License as published by<br>")
                .append("the Free Software Foundation, either version 3 of the License, or<br>")
                .append("(at your option) any later version.</p>")
                .append("<p>This program is distributed in the hope that it will be useful,<br>")
                .append("but WITHOUT ANY WARRANTY; without even the implied warranty of<br>")
                .append("MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the<br>")
                .append("GNU General Public License for more details.</p>")
                .append("You should have received a copy of the GNU General Public License<br>")
                .append("along with this program. If not, see <a href=\"http://www.gnu.org/licenses/\">http://www.gnu.org/licenses/</a>.")
                .append("</html>");

        JEditorPane aboutPane = new JEditorPane("text/html", msg.toString());

        aboutPane.addHyperlinkListener(e -> {
            if (e.getEventType().equals(HyperlinkEvent.EventType.ACTIVATED) && Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().browse(e.getURL().toURI());
                } catch (URISyntaxException | IOException ex) {
                    ex.printStackTrace();
                }
            }
        });

        aboutPane.setEditable(false);

        JOptionPane.showMessageDialog(lookupFrame, aboutPane,
                "About PechaKuchaTool", JOptionPane.PLAIN_MESSAGE);
    }
}