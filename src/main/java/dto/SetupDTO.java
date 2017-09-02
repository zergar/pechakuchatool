package dto;

import java.awt.*;

/**
 * A Data Transfer Object containing the Setup-configuration of a launch.
 */
public class SetupDTO {
    /**
     * The {@link GraphicsDevice} the presentation-screen shall be displayed on.
     */
    GraphicsDevice presentationScreen;

    /**
     * The {@link GraphicsDevice} the lookup-screen shall be displayed on.
     */
    GraphicsDevice lookupScreen;

    /**
     * The string of the {@link logic.PDFViewerController#createNewControllersByName(String, int)} to be used.
     */
    String pdfViewer;

    /**
     * A switch which determines whether an {@link arduino.ArduinoSerialConnection} shall be established.
     */
    boolean establishArduinoConn;

    public SetupDTO(GraphicsDevice presentationScreen, GraphicsDevice lookupScreen, String pdfViewer, boolean establishArduinoConn) {
        this.presentationScreen = presentationScreen;
        this.lookupScreen = lookupScreen;
        this.pdfViewer = pdfViewer;
        this.establishArduinoConn = establishArduinoConn;
    }

    public GraphicsDevice getPresentationScreen() {
        return presentationScreen;
    }

    public void setPresentationScreen(GraphicsDevice presentationScreen) {
        this.presentationScreen = presentationScreen;
    }

    public GraphicsDevice getLookupScreen() {
        return lookupScreen;
    }

    public void setLookupScreen(GraphicsDevice lookupScreen) {
        this.lookupScreen = lookupScreen;
    }

    public String getPdfViewer() {
        return pdfViewer;
    }

    public void setPdfViewer(String pdfViewer) {
        this.pdfViewer = pdfViewer;
    }

    public boolean isEstablishArduinoConn() {
        return establishArduinoConn;
    }

    public void setEstablishArduinoConn(boolean establishArduinoConn) {
        this.establishArduinoConn = establishArduinoConn;
    }
}
