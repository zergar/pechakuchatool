package logic;

import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;
import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.views.DocumentViewController;
import org.icepdf.ri.common.views.DocumentViewControllerImpl;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
 * A concrete implementation of a {@link PDFViewerController} using the IcePDF-library for PDF rendering.
 *
 * @see <a href="http://www.icesoft.org/java/projects/ICEpdf/overview.jsf">IcePDF project page</a>
 */
public class IcePDFController implements PDFViewerController {

    /**
     * A {@link Logger}.
     */
    private static final Logger LOG = Logger.getLogger(IcePDFController.class.getName());

    /**
     * The {@link SwingController}s of the different displayed renders.
     */
    private ArrayList<SwingControllerWrapper> controllers;

    /**
     * The PDF-document to be displayed.
     */
    private Document pdf;

    /**
     * The constructor.
     */
    IcePDFController() {
        this.controllers = new ArrayList<>();
    }

    @Override
    public void createNewController() {
        controllers.add(new SwingControllerWrapper());
    }

    @Override
    public void createNewControllers(int amount) {
        for (int i = 0; i < amount; i++) {
            this.createNewController();
        }
    }

    @Override
    public boolean isDocumentSelected() {
        return pdf != null;
    }

    @Override
    public Container getDocContainer(int contrID) {
        return controllers.get(contrID).getViewerPane();
    }

    @Override
    public void fitViewers() {
        controllers.forEach(SwingControllerWrapper::fitViewer);
    }

    @Override
    public void nextPage() {
        controllers.forEach(SwingControllerWrapper::nextPage);
    }

    @Override
    public void prevPage() {
        controllers.forEach(SwingControllerWrapper::prevPage);
    }

    @Override
    public int getCurrentPageNumber() {
        return controllers.get(0).getController().getCurrentPageNumber();
    }

    @Override
    public int getPageCount() {
        return controllers.get(0).getController().getPageTree().getNumberOfPages();
    }

    @Override
    public void gotoFirst() {
        controllers.forEach(c -> c.getController().showPage(0));
    }

    @Override
    public void setScreenVisibility(boolean visible) {
        controllers.forEach(c -> c.getDocContainer().setVisible(visible));
    }

    @Override
    public boolean isScreenVisible() {
        return controllers.stream().map(SwingControllerWrapper::getDocContainer).allMatch(Component::isVisible);
    }

    @Override
    public void setCursorVisibility(boolean visible) {
        // Transparent 16 x 16 pixel cursor image.
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        // Create a new blank cursor.
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");

        if (visible) {
            controllers.forEach(c -> c.getViewerPane().setCursor(Cursor.getDefaultCursor()));
        } else {
            controllers.forEach(c -> c.getViewerPane().setCursor(blankCursor));
        }
    }

    @Override
    public void loadNewFile(String filePath) throws IOException, PDFException, PDFSecurityException {
        LOG.info("Starting loading file " + filePath);

        pdf = new Document();
        pdf.setFile(filePath);
        controllers.forEach(c -> c.loadNewFile(pdf));

        LOG.info("Finished loading file " + filePath);


        fitViewers();
    }

    @Override
    public boolean savePresentationToFile(File file) throws IOException {
        return false;
    }

    @Override
    public void loadPresentationFromFile(File file) throws IOException, ClassNotFoundException {
    }


    /**
     * This class wraps a {@link SwingController}, potentially to wrap its Swing-{@link Container} within another JPanel.
     */
    private class SwingControllerWrapper {
        /**
         * The {@link SwingController}.
         */
        SwingController controller;

        /**
         * The constructor.
         */
        SwingControllerWrapper() {
            this.controller = new SwingController();
            this.controller.setIsEmbeddedComponent(true);
            this.controller.setPageViewMode(DocumentViewControllerImpl.ONE_PAGE_VIEW, true);

            Container docContainer = controller.getDocumentViewController().getViewContainer();
            docContainer.setBackground(Color.BLACK);

            if (docContainer instanceof JScrollPane) {
                ((JScrollPane) docContainer).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                ((JScrollPane) docContainer).setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
            }
        }

        SwingController getController() {
            return controller;
        }

        Container getViewerPane() {
            return getDocContainer();
        }

        Container getDocContainer() {
            return controller.getDocumentViewController().getViewContainer();
        }

        Document getDocument() {
            return controller.getDocument();
        }

        DocumentViewController getDocumentViewController() {
            return controller.getDocumentViewController();
        }

        void loadNewFile(Document pdf) {
            controller.openDocument(pdf, "PechaKuchaPDF");
        }

        void prevPage() {
            controller.goToDeltaPage(-controller.getDocumentViewController().getDocumentView().getNextPageIncrement());
        }

        void nextPage() {
            controller.goToDeltaPage(controller.getDocumentViewController().getDocumentView().getNextPageIncrement());
        }

        void fitViewer() {
            Container pane = getDocContainer();
            PDimension pDim = controller.getDocument().getPageDimension(0, 0f);


            double frameRatio = ((double) pane.getSize().height / (double) pane.getSize().width);
            double pageRatio = pDim.getHeight() / pDim.getWidth();

//            System.out.printf("FrameRatio: %10.4f, PageRatio: %10.4f\n", frameRatio, pageRatio);

            if (frameRatio < pageRatio) {
                controller.getDocumentViewController().setFitMode(DocumentViewController.PAGE_FIT_WINDOW_HEIGHT);
            } else {
                controller.getDocumentViewController().setFitMode(DocumentViewController.PAGE_FIT_WINDOW_WIDTH);
            }

            LOG.info("Fitted Doc-Container");
        }
    }
}
