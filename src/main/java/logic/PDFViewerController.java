package logic;

import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import viewComponents.ScreenSetupFrame;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

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
     * @return The number of the current page.
     */
    int getCurrentPageNumber();

    /**
     * Displays the first page of a document on every content-container.
     */
    void gotoFirst();

    /**
     * Set whether the slide-panels should be visible.
     * Credits: https://stackoverflow.com/questions/1984071/how-to-hide-cursor-in-a-swing-application
     * @param visible the visibility
     */
    void setScreenVisibility(boolean visible);

    /**
     * Returns the visibility of the slide-panels.
     * Returns false if at least one slide-panel is not visible.
     * @return
     */
    boolean isScreenVisible();

    /**
     * Set whether the cursor should be visible on the slide-panels.
     * @param visible the visibility
     */
    void setCursorVisibility(boolean visible);

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

    /**
     * Writes the currently loaded Presentation to a location within the filesystem.
     * @param file the path to be written to
     * @throws IOException
     */
    boolean savePresentationToFile(File file) throws IOException;

    /**
     * Loads a Presentation from a location within the filesystem.
     * @param file the path to be loaded from
     * @throws IOException
     */
    void loadPresentationFromFile(File file) throws IOException, ClassNotFoundException;
}
