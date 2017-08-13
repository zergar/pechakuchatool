package main;

import org.icepdf.ri.common.SwingController;
import org.icepdf.ri.common.SwingViewBuilder;

import javax.swing.*;

public class ViewerComponentExample {
    public static void main(String[] args) {
        // Get a file from the command line to open
        String filePath = "/home/gereon/UNI/SoSe 2017/IG/Folien/UE_01_WissArb1.pdf";

        // build a component controller
        SwingController controller = new SwingController();

        SwingViewBuilder factory = new SwingViewBuilder(controller);

        JPanel viewerComponentPanel = factory.buildViewerPanel();

        // add interactive mouse link annotation support via callback
        controller.getDocumentViewController().setAnnotationCallback(
                new org.icepdf.ri.common.MyAnnotationCallback(
                        controller.getDocumentViewController()));

        JFrame applicationFrame = new JFrame();
        applicationFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        applicationFrame.getContentPane().add(viewerComponentPanel);

        // Now that the GUI is all in place, we can try opening a PDF
        controller.openDocument(filePath);

        // show the component
        applicationFrame.pack();
        applicationFrame.setVisible(true);
    }
}