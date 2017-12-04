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
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
 * @see <a href="http://www.icesoft.org/java/projects/ICEpdf/overview.jsf">IcePDF project page</a>
 */
public class IcePDFController implements PDFViewerController {
    /**
     * The {@link SwingController}s of the different displayed renders.
     */
    private ArrayList<SwingController> controllers;

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
        SwingController controller = new SwingController();
        controller.setIsEmbeddedComponent(true);
        controller.setPageViewMode(DocumentViewControllerImpl.ONE_PAGE_VIEW, true);

        Container docContainer = controller.getDocumentViewController().getViewContainer();

        if (docContainer instanceof JScrollPane) {
            ((JScrollPane) docContainer).setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            ((JScrollPane) docContainer).setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        }

        controllers.add(controller);
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
        return controllers.get(contrID).getDocumentViewController().getViewContainer();
    }

    @Override
    public void fitViewers() {
        controllers.forEach(c -> {
            Container pane = c.getDocumentViewController().getViewContainer();
            PDimension pDim = c.getDocument().getPageDimension(0, 0f);


            double frameRatio = ((double) pane.getSize().height / (double) pane.getSize().width);
            double pageRatio = pDim.getHeight() / pDim.getWidth();

//            System.out.printf("FrameRatio: %10.4f, PageRatio: %10.4f\n", frameRatio, pageRatio);

            if (frameRatio < pageRatio) {
                c.getDocumentViewController().setFitMode(DocumentViewController.PAGE_FIT_WINDOW_HEIGHT);
            } else {
                c.getDocumentViewController().setFitMode(DocumentViewController.PAGE_FIT_WINDOW_WIDTH);
            }
        });
    }

    @Override
    public void nextPage() {
        controllers.forEach(c -> c.goToDeltaPage(c.getDocumentViewController().getDocumentView().getNextPageIncrement()));
    }

    @Override
    public void prevPage() {
        controllers.forEach(c -> c.goToDeltaPage(-c.getDocumentViewController().getDocumentView().getPreviousPageIncrement()));
    }

    @Override
    public int getCurrentPageNumber() {
        return controllers.get(0).getCurrentPageNumber();
    }

    @Override
    public void gotoFirst() {
        controllers.forEach(c -> c.showPage(0));
    }

    @Override
    public void loadNewFile(String filePath) throws IOException, PDFException, PDFSecurityException {
        pdf = new Document();
        pdf.setFile(filePath);
        controllers.forEach(c -> c.openDocument(pdf, "PechaKuchaPDF"));

        fitViewers();
    }

    @Override
    public boolean savePresentationToFile(File file) throws IOException {
        return false;
    }

    @Override
    public void loadPresentationFromFile(File file) throws IOException, ClassNotFoundException {
    }
}
