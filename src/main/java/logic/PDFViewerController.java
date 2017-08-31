package logic;

import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;

import java.awt.*;
import java.io.IOException;
import java.util.concurrent.ExecutionException;

public interface PDFViewerController {
    public static PDFViewerController createNewControllersByName(String name, int amount) {
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


    public void createNewController();

    public void createNewControllers(int amount);

    public boolean isDocumentSelected();

    public Container getDocContainer(int contrID);

    public void fitViewers();

    public void nextPage();

    public void prevPage();

    public int getCurrentPageNumber();

    public void gotoFirst();

    public void loadNewFile(String filePath) throws IOException, PDFException, PDFSecurityException, InterruptedException, ExecutionException;


}
