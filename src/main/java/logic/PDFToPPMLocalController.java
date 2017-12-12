package logic;

import logic.pdftoppm.PDFToPPMConverter;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
 * A concrete implementation of a {@link PDFViewerController} using poppler-utils for PDF rendering.
 */
public class PDFToPPMLocalController implements PDFViewerController, Saveable {

    private static final Logger LOG = Logger.getLogger(PDFToPPMLocalController.class.getName());

    /**
     * The originally converted and loaded images.
     */
    private ArrayList<ImageIcon> originalImages;

    /**
     * A list containing the rendered images and panels currently in use.
     */
    private ArrayList<PDFPrerenderedImageWrapper> imageWrapper;

    /**
     * The number of the currently displayed page.
     */
    private int currImageNumber = 0;

    /**
     * The default constructor.
     */
    public PDFToPPMLocalController() {
        this.originalImages = new ArrayList<>();
        this.imageWrapper = new ArrayList<>();
    }


    @Override
    public void createNewController() {
        imageWrapper.add(new PDFPrerenderedImageWrapper());
    }

    @Override
    public void createNewControllers(int amount) {
        for (int i = 0; i < amount; i++) {
            this.createNewController();
        }
    }

    @Override
    public boolean isDocumentSelected() {
        return originalImages.size() > 0;
    }

    @Override
    public Container getDocContainer(int contrID) {
        return imageWrapper.get(contrID).getImagePanel();
    }

    @Override
    public void fitViewers() {
        refreshImage();
    }

    @Override
    public void nextPage() {
        if (currImageNumber < originalImages.size() - 1) {
            currImageNumber++;

            refreshImage();
        }
    }

    @Override
    public void prevPage() {
        if (currImageNumber > 0) {
            currImageNumber--;

            refreshImage();
        }
    }

    @Override
    public int getCurrentPageNumber() {
        return currImageNumber;
    }

    @Override
    public void gotoFirst() {
        currImageNumber = 0;

        refreshImage();
    }

    @Override
    public void setScreenVisibility(boolean visible) {
        imageWrapper.forEach(w -> w.getImageLabel().setVisible(visible));
    }

    @Override
    public boolean isScreenVisible() {
        return imageWrapper.stream().map(PDFPrerenderedImageWrapper::getImageLabel).allMatch(Component::isVisible);
    }

    @Override
    public void setCursorVisibility(boolean visible) {
        // Transparent 16 x 16 pixel cursor image.
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        // Create a new blank cursor.
        Cursor blankCursor = Toolkit.getDefaultToolkit().createCustomCursor(
                cursorImg, new Point(0, 0), "blank cursor");

        if (visible) {
            imageWrapper.forEach(w -> w.getImagePanel().setCursor(Cursor.getDefaultCursor()));
        } else {
            imageWrapper.forEach(w -> w.getImagePanel().setCursor(blankCursor));
        }
    }

    @Override
    public void loadNewFile(String filePath) throws IOException, PDFException, PDFSecurityException, InterruptedException, ExecutionException {
        loadNewFile(filePath, false);
    }

    /**
     * Superfunction used by {@link PDFToPPMLocalController#loadNewFile(String)}, additional Parameter to prevent scaling after conversion.
     *
     * @param filePath   the File-path
     * @param scaleAfter whether the conversion should trigger the scaling-routine.
     * @throws IOException
     * @throws PDFException
     * @throws PDFSecurityException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public void loadNewFile(String filePath, boolean scaleAfter) throws IOException, PDFException, PDFSecurityException, InterruptedException, ExecutionException {
        ProcessBuilder numberOfPagesBuilder = new ProcessBuilder("pdfinfo", filePath);
        numberOfPagesBuilder.redirectErrorStream(true);
        Process numberOfPagesProcess = numberOfPagesBuilder.start();
        numberOfPagesProcess.waitFor();

        BufferedReader br = new BufferedReader(new InputStreamReader(numberOfPagesProcess.getInputStream()));
        String nopString = "0";
        String line;

        while ((line = br.readLine()) != null) {
            if (line.contains("Pages:")) {
                String[] split = line.split(" ");
                nopString = split[split.length - 1];
                break;
            }
        }


        int nop = Integer.parseInt(nopString);

        originalImages = new ArrayList<>();
        ArrayList<Future<BufferedImage>> converters = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(nop);
        ExecutorService pool = Executors.newCachedThreadPool();


        LOG.info("Begin rendering images of file " + filePath);

//        ProgressWindow progress = new ProgressWindow(nop);

        imageWrapper.forEach(w -> w.getImagePanel().getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR)));

        for (int i = 0; i < nop; i++) {
            PDFToPPMConverter convertTask = new PDFToPPMConverter(i + 1, filePath, latch);
            Future<BufferedImage> result = pool.submit(convertTask);
            converters.add(result);
        }

        latch.await();


        for (Future<BufferedImage> img : converters) {
            originalImages.add(new ImageIcon(img.get()));
        }


        if (scaleAfter) {
            initScaling();
        }
    }

    /**
     * Calls {@link PDFPrerenderedImageWrapper#refreshImage()} on every wrapper.
     */
    private void refreshImage() {
        imageWrapper.forEach(PDFPrerenderedImageWrapper::refreshImage);
    }

    /**
     * Wrapper for starting the image-scaling-process
     */
    private void initScaling() {
        imageWrapper.forEach(PDFPrerenderedImageWrapper::prescaleImages);

        imageWrapper.forEach(w -> w.getImagePanel().getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)));
//        progress.close();


        this.gotoFirst();

        this.setScreenVisibility(true);
    }

    @Override
    public boolean savePresentationToFile(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        GZIPOutputStream gos = new GZIPOutputStream(fos);
        ObjectOutputStream oos = new ObjectOutputStream(gos);

        LOG.info("Starting saving pre-rendered file " + file.getAbsolutePath());

        oos.writeObject(originalImages);

        LOG.info("Finished saving pre-rendered file " + file.getAbsolutePath());

        oos.close();
        gos.close();
        fos.close();

        LOG.info("Finished writing currently loaded Presentation to " + file.getAbsolutePath());
        return true;
    }

    @Override
    public void loadPresentationFromFile(File file) throws IOException, ClassNotFoundException {
        FileInputStream fis = new FileInputStream(file);
        GZIPInputStream gis = new GZIPInputStream(fis);
        ObjectInputStream ois = new ObjectInputStream(gis);

        LOG.info("Starting loading pre-rendered file " + file.getAbsolutePath());

        this.originalImages = (ArrayList<ImageIcon>) ois.readObject();

        LOG.info("Finished loading pre-rendered file " + file.getAbsolutePath());

        ois.close();
        gis.close();
        fis.close();

        initScaling();
    }


    /**
     * A wrapper containing pre-rendered images and panels for the different display-locations.
     */
    private class PDFPrerenderedImageWrapper {
        /**
         * The to the correct size prerendered images for each wrapper.
         */
        private ArrayList<ImageIcon> images;

        /**
         * The {@link JPanel} wrapping the displayed images.
         */
        private JPanel imagePanel;

        /**
         * The {@link JLabel} containing the currently displayed image.
         */
        private JLabel imageLabel;

        /**
         * The Constructor.
         */
        PDFPrerenderedImageWrapper() {
            imageLabel = new JLabel();
            imageLabel.setBackground(Color.BLACK);

            imagePanel = new JPanel();
            imagePanel.add(imageLabel);
            imagePanel.setBackground(Color.BLACK);

            images = new ArrayList<>();
        }

        /**
         * Re-renders the displayed versions of the images to the currently used size.
         */
        private void prescaleImages() {
            LOG.info("Begin scaling images of panel " + imagePanel);

            images = originalImages.parallelStream().map(imageIcon -> {
                Image i = imageIcon.getImage();

                Dimension panelDim = imageLabel.getParent().getSize();
                Dimension imageDim = new Dimension(i.getWidth(null), i.getHeight(null));

                if (panelDim.width <= 0 || panelDim.height <= 0) {
                    return imageIcon;
                }


                double panelRatio = panelDim.getHeight() / panelDim.getWidth();
                double imageRatio = imageDim.getHeight() / imageDim.getWidth();

                Image imageScaled;

                if (panelRatio < imageRatio) {
                    imageScaled = i.getScaledInstance((int) (panelDim.height / imageRatio), panelDim.height, Image.SCALE_SMOOTH);
                } else {
                    imageScaled = i.getScaledInstance(panelDim.width, (int) (panelDim.width * imageRatio), Image.SCALE_SMOOTH);
                }

                return new ImageIcon(imageScaled);
            }).collect(Collectors.toCollection(ArrayList::new));

            LOG.info("Finished scaling images of panel " + imagePanel);
        }

        public JPanel getImagePanel() {
            return imagePanel;
        }

        public JLabel getImageLabel() {
            return imageLabel;
        }

        /**
         * Refreshes the currently used image on the imageLabel, usually used in conjunction with a page change.
         */
        public void refreshImage() {
            imageLabel.setIcon(images.get(currImageNumber));
        }
    }
}
