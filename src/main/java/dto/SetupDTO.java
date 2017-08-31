package dto;

import java.awt.*;

public class SetupDTO {
    GraphicsDevice presentationScreen;
    GraphicsDevice lookupScreen;
    String pdfViewer;

    public SetupDTO(GraphicsDevice presentationScreen, GraphicsDevice lookupScreen, String pdfViewer) {
        this.presentationScreen = presentationScreen;
        this.lookupScreen = lookupScreen;
        this.pdfViewer = pdfViewer;
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
}
