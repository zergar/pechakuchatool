package main;

import arduino.ArduinoSerialConnection;
import dto.SetupDTO;
import logic.NoGraphicsDevice;
import logic.PDFViewerController;
import logic.Saveable;
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
import java.awt.event.ActionEvent;
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
     * A {@link JFileChooser} for picking the new file to load.
     */
    private JFileChooser newPresChooser = new JFileChooser();

    /**
     * A {@link JFileChooser} for picking a pre-rendered file to load.
     */
    private JFileChooser renderedPresChooser = new JFileChooser();

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
     * The GlobalScreen-Logger.
     */
    private Logger logger = Logger.getLogger(GlobalScreen.class.getPackage().getName());

    private static final Logger LOG = Logger.getLogger(PechaKuchaMain.class.getName());

    /**
     * Default constructor for running the PechaKuchaTool.
     *
     * @param settings The settings of the current launch.
     */
    public PechaKuchaMain(SetupDTO settings) {
        this.presStartTime = new AtomicLong(-1);
        this.nextPageTime = new AtomicLong(-1);
        this.nextSecTime = new AtomicLong(-1);
        this.timeRemaining = new AtomicInteger(settings.getTimePerSlide());

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
        this.timeRemaining = new AtomicInteger(settings.getTimePerSlide());

        this.settings = settings;

        setup(path);
    }

    /**
     * the setup-routine
     */
    private void setup() {
        // Setup
        Defs.setSystemProperty("org.icepdf.core.views.background.color", "000000");

        newPresChooser.setFileFilter(new FileNameExtensionFilter("PDF-Documents", "pdf"));
        renderedPresChooser.setFileFilter(new FileNameExtensionFilter("PechaKuchaTool-Files", "pktool"));


        pdfViewerController = PDFViewerController.createNewControllersByName(settings.getPdfViewer(), 2);


        // arduino
        arduinoConn = new ArduinoSerialConnection(settings.isEstablishArduinoConn());
        arduinoConn.initialize();
        arduinoConn.send(String.format("%02d", settings.getTimePerSlide()));


        // presentation-frame
        presFrame = new JFrame("PechaKucha Presentation-Window");
        presFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        presFrame.getContentPane().add(pdfViewerController.getDocContainer(0));
        presFrame.getContentPane().setBackground(Color.BLACK);

        if (!(settings.getPresentationScreen() instanceof NoGraphicsDevice)) {
            presFrame.pack();
            showOnScreen(settings.getPresentationScreen(), presFrame, true);
            presFrame.setVisible(true);
        }


        // lookup-frame
        lookupFrame = new JFrame("PechaKucha Presentation-Tool");
        Container lookupPane = lookupFrame.getContentPane();
        lookupPane.setLayout(new BoxLayout(lookupPane, BoxLayout.X_AXIS));
        lookupPane.setBackground(Color.BLACK);

        timeRemainingLabel = new JLabel(String.format("%02d", settings.getTimePerSlide()));
        timeRemainingLabel.setForeground(Color.WHITE);
        timeRemainingLabel.setBackground(Color.BLACK);

        JPanel timePanel = new JPanel();
        timePanel.setBackground(Color.BLACK);
        timePanel.add(timeRemainingLabel);


        JMenuBar menuBar = createLookupMenuBar();
        lookupFrame.setJMenuBar(menuBar);


        lookupPane.add(pdfViewerController.getDocContainer(1));
        lookupPane.add(timePanel);

        lookupFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        lookupFrame.pack();
        showOnScreen(settings.getLookupScreen(), lookupFrame, false);

        lookupFrame.setVisible(true);

        lookupFrame.requestFocus();


        Font font = timeRemainingLabel.getFont();
        int screenWidth = settings.getLookupScreen().getDisplayMode().getWidth();
        double fontSize = font.getSize() * ((double) screenWidth * 0.2 / (double) timeRemainingLabel.getFontMetrics(font).stringWidth("00"));
        timeRemainingLabel.setFont(timeRemainingLabel.getFont().deriveFont(Font.BOLD).deriveFont((float) fontSize));

        timePanel.setPreferredSize(new Dimension((int) (screenWidth * 0.2), lookupFrame.getHeight()));
        pdfViewerController.getDocContainer(1).setPreferredSize(new Dimension((int) (screenWidth * 0.75), lookupFrame.getHeight()));

        // timer

        t = new Timer(4, this::setTimer);


        // keyboard

        nkl = new NativeKeyListener() {
            @Override
            public void nativeKeyTyped(NativeKeyEvent nativeKeyEvent) {
                int kc = nativeKeyEvent.getKeyCode();

                if (pdfViewerController.isDocumentSelected() && isFocusedApplication()) {
                    if (kc == NativeKeyEvent.VC_SPACE && !t.isRunning()) {
                        startPresentation(true);
                    } else if (kc == NativeKeyEvent.VC_R && t.isRunning()) {
                        resetPresentation(true);
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
                        startPresentation(true);
                    } else if (kc == KeyEvent.VK_R && t.isRunning()) {
                        resetPresentation(true);
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
     * Creates the menu-mar of the lookup-frame.
     *
     * @return the menu-bar
     */
    private JMenuBar createLookupMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Files");
        menuBar.add(fileMenu);

        JMenuItem openFile = new JMenuItem("Open File");
        openFile.addActionListener(e -> openDocument(lookupFrame));
        openFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
        fileMenu.add(openFile);

        fileMenu.addSeparator();

        if (pdfViewerController instanceof Saveable)

        {
            JMenuItem openPreRenderedFile = new JMenuItem("Open pre-rendered Presentation");
            openPreRenderedFile.addActionListener(e -> {
                try {
                    loadRenderedPresentation(lookupFrame);
                } catch (IOException | ClassNotFoundException err) {
                    err.printStackTrace();
                }
            });
            openPreRenderedFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.ALT_MASK));
            fileMenu.add(openPreRenderedFile);

            JMenuItem savePreRenderedFile = new JMenuItem("Save pre-rendered Presentation");
            savePreRenderedFile.addActionListener(e -> {
                try {
                    saveRenderedPresentation(lookupFrame);
                } catch (IOException err) {
                    err.printStackTrace();
                }
            });
            savePreRenderedFile.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
            fileMenu.add(savePreRenderedFile);

            fileMenu.addSeparator();
        }

        JMenuItem exit = new JMenuItem("Exit");
        exit.addActionListener(e ->

                exit());
        exit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_MASK));
        fileMenu.add(exit);


        JMenu presMenu = new JMenu("Presentation");
        menuBar.add(presMenu);

        JMenuItem startPres = new JMenuItem("Start Presentation");
        startPres.addActionListener(e ->

                startPresentation(true));
        startPres.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0));
        presMenu.add(startPres);

        JMenuItem startFromCurrSlide = new JMenuItem("Start Presentation from current Slide");
        startFromCurrSlide.addActionListener(e ->

                startPresentation(false));
        startFromCurrSlide.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
        presMenu.add(startFromCurrSlide);

        JMenuItem resetPres = new JMenuItem("Reset Presentation");
        resetPres.addActionListener(e ->

                resetPresentation(true));
        resetPres.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, 0));
        presMenu.add(resetPres);

        JMenu viewMenu = new JMenu("View");
        menuBar.add(viewMenu);

        JMenuItem fitPage = new JMenuItem("Fit Page");
        fitPage.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, 0));
        fitPage.addActionListener(e -> pdfViewerController.fitViewers());
        viewMenu.add(fitPage);

        JMenuItem toggleBlack = new JMenuItem("Toggle Black Screen");
        toggleBlack.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_B, InputEvent.ALT_MASK));
        toggleBlack.addActionListener(e -> pdfViewerController.setScreenVisibility(!pdfViewerController.isScreenVisible()));
        viewMenu.add(toggleBlack);

        JMenu helpMenu = new JMenu("Help");
        menuBar.add(helpMenu);

        JMenuItem about = new JMenuItem("About");
        about.addActionListener(e ->

                showAboutText());
        helpMenu.add(about);
        return menuBar;
    }

    /**
     * Setting up the {@link Timer}
     *
     * @param e the Timer-Event.
     */
    private void setTimer(ActionEvent e) {
        if (System.currentTimeMillis() >= nextPageTime.get()) {
            if (pdfViewerController.getCurrentPageNumber() + 1 >= settings.getMaxSlides() && settings.getMaxSlides() <= pdfViewerController.getPageCount()) {
                pdfViewerController.setCursorVisibility(true);
                resetPresentation(false);

                return;
            } else if (pdfViewerController.getCurrentPageNumber() + 1 >= pdfViewerController.getPageCount() && settings.getMaxSlides() > pdfViewerController.getPageCount()) {
                t.stop();
                pdfViewerController.setCursorVisibility(true);

                LOG.info("reached page count");

                return;
            } else {
                pdfViewerController.nextPage();
                nextPageTime.set(presStartTime.get() + ((pdfViewerController.getCurrentPageNumber() + 1) * settings.getTimePerSlide() * 1_000L));
                timeRemaining.set(settings.getTimePerSlide());
            }
        }

        if (System.currentTimeMillis() >= nextSecTime.get()) {
            String timeString = String.format("%02d", timeRemaining.get());
            updateSeconds(timeString);
            timeRemaining.decrementAndGet();
            nextSecTime.set(nextPageTime.get() - (timeRemaining.get() * 1_000));
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
    private void startPresentation(boolean gotoFirst) {
        if (gotoFirst) {
            pdfViewerController.gotoFirst();
        }

        pdfViewerController.setScreenVisibility(true);
        pdfViewerController.setCursorVisibility(false);

        long pStart = System.currentTimeMillis();
        presStartTime.set(pStart);
        nextPageTime.set(pStart + settings.getTimePerSlide() * 1_000);
        nextSecTime.set(pStart + 1_000);

        t.start();
    }

    /**
     * Reset the presentation.
     */
    private void resetPresentation(boolean setVisible) {
        t.stop();
        pdfViewerController.gotoFirst();
        initSeconds();

        pdfViewerController.setScreenVisibility(setVisible);
        pdfViewerController.setCursorVisibility(true);
    }

    /**
     *
     */
    private void initSeconds() {
        timeRemainingLabel.setText(String.format("%02d", settings.getTimePerSlide()));
        arduinoConn.send(String.format("%02d", settings.getTimePerSlide()));
        timeRemaining.set(settings.getTimePerSlide());
    }


    /**
     * Select a file via the {@link JFileChooser}
     *
     * @param parent the frame of the {@link JFileChooser}
     * @return the File
     */
    private void openDocument(JFrame parent) {
        int returnVal = newPresChooser.showOpenDialog(parent);
        if (returnVal == JFileChooser.APPROVE_OPTION) {

            try {
                pdfViewerController.loadNewFile(newPresChooser.getSelectedFile().getAbsolutePath());

                parent.setTitle("PechaKucha Presentation-Tool - " + newPresChooser.getSelectedFile().getName());
                initSeconds();
            } catch (IOException | PDFException | PDFSecurityException | InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        } else {

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

    /**
     * Checks if this application is currently focused.
     *
     * @return the focus
     */
    private boolean isFocusedApplication() {
        boolean presFrameFocus = false;

        if (!(settings.getPresentationScreen() instanceof NoGraphicsDevice)) {
            presFrameFocus = presFrame.isFocused();
        }

        return lookupFrame.isFocused() || presFrameFocus;
    }

    /**
     * Method which can save a loaded presentation.
     *
     * @param parent the parent of the JFileChooser
     * @throws IOException
     */
    private void saveRenderedPresentation(JFrame parent) throws IOException {
        if (pdfViewerController instanceof Saveable) {

            String fileName;
            if (newPresChooser.getSelectedFile() == null) {
                fileName = "RenderedPresentation";
            } else {
                fileName = newPresChooser.getSelectedFile().getName();
            }

            renderedPresChooser.setSelectedFile(new File(fileName + ".pktool"));
            int returnVal = renderedPresChooser.showSaveDialog(parent);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                pdfViewerController.savePresentationToFile(renderedPresChooser.getSelectedFile());
            } else {
                JOptionPane.showMessageDialog(parent, "An error occured while saving the current presentation.",
                        "Error while saving", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(parent, "This renderer does not support this feature.",
                    "Unsupported Feature", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    /**
     * Method which can load a saved pre-rendered presentation.
     *
     * @param parent the parent of the JFileChooser
     * @throws IOException
     */
    private void loadRenderedPresentation(JFrame parent) throws IOException, ClassNotFoundException {
        if (pdfViewerController instanceof Saveable) {

            int returnVal = renderedPresChooser.showOpenDialog(parent);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                pdfViewerController.loadPresentationFromFile(renderedPresChooser.getSelectedFile());

                parent.setTitle("PechaKucha Presentation-Tool - " + renderedPresChooser.getSelectedFile().getName());
                initSeconds();
            } else {
                JOptionPane.showMessageDialog(parent, "An error occured while loading the given pre-rendered file.",
                        "Error while loading", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            JOptionPane.showMessageDialog(parent, "This renderer does not support this feature.",
                    "Unsupported Feature", JOptionPane.INFORMATION_MESSAGE);
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