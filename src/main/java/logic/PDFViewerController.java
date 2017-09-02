package logic;

import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import viewComponents.ScreenSetupFrame;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * This interface defines the base structure for PDF-viewer-implementations used by {@link main.PechaKuchaMain}.
 */
public interface PDFViewerController {

    /**
     * This method creates a {@link PDFViewerController} for a given string. Note, that upon implementation of another
     * controller the name of the controller needs to be equal to the name of a controller at {@link ScreenSetupFrame#getSettings()}
     *
     * @param name the name of the {@link PDFViewerController}
     * @param amount the amount of Panels containing controllers should be created
     * @return the controller
     */
    static PDFViewerController createNewControllersByName(String name, int amount) {
        PDFViewerController pdfvc;

        switch (name.toLowerCase()) {
            case "icepdf":
                pdfvc = new IcePDFController();
                break;

            case "pdftoppm-local":
                pdfvc = new PDFToPPMLocalController();
                break;

            default:
                pdfvc = new IcePDFController();

        }

        pdfvc.createNewControllers(amount);
        return pdfvc;
    }

    /**
     * Creates a new content container instance within the controller.
     */
    void createNewController();

    /**
     * Creates multiple content-containers by iterating over createNewController.
     * @param amount The amount of controllers to be created.
     */
    void createNewControllers(int amount);

    /**
     * Checks whether a PDF-document is selected.
     *
     * @return If a PDF-document is selected.
     */
    boolean isDocumentSelected();

    /**
     * Returns the AWT container containing the displayed content belonging to the given controllerID.
     * @param contrID The internal ID of the controller.
     * @return The {@link Container}.
     */
    Container getDocContainer(int contrID);

    /**
     * Fits the contents of the container to the current viewport.
     */
    void fitViewers();

    /**
     * Displays the next page of a document on every content-container.
     */
    void nextPage();

    /**
     * Displays the previous page of a document on every content-container.
     */
    void prevPage();

    /**
     * Returns the number of the current page.
     * @return The number of te current page.
     */
    int getCurrentPageNumber();

    /**
     * Displays the first page of a document on every content-container.
     */
    void gotoFirst();

    /**
     * Loads a new PDF-Document into the controller.
     * @param filePath The absolute path to the file which shall be loaded.
     * @throws IOException
     * @throws PDFException
     * @throws PDFSecurityException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    void loadNewFile(String filePath) throws IOException, PDFException, PDFSecurityException, InterruptedException, ExecutionException;
}
