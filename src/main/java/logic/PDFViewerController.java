package logic;

import org.icepdf.core.pobjects.Document;
import org.icepdf.core.pobjects.PDimension;
import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.views.DocumentViewController;
import org.icepdf.ri.common.views.DocumentViewControllerImpl;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;

public class PDFViewerController {
    private ArrayList<SwingController> controllers;

    private String file;

    public PDFViewerController() {
        this.controllers = new ArrayList<>();
    }

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

    public SwingController getController(int contrID) {
        return controllers.get(contrID);
    }

    public Container getDocContainer(int contrID) {
        return controllers.get(contrID).getDocumentViewController().getViewContainer();
    }

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

//            c.setPageViewMode(DocumentViewControllerImpl.ONE_PAGE_VIEW, true);


//           ((JFrame) SwingUtilities.getWindowAncestor(parent)).pack();

        });
    }

    public void nextPage() {
        controllers.forEach(c -> c.goToDeltaPage(c.getDocumentViewController().getDocumentView().getNextPageIncrement()));
    }

    public int getCurrentPageNumber() {
        return controllers.get(0).getCurrentPageNumber();
    }

    public void gotoFirst() {
        controllers.forEach(c -> c.showPage(0));
    }

    public void loadNewFile(String file) {
        this.file = file;
        controllers.forEach(c -> c.openDocument(file));
    }
}
