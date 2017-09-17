package logic;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * A concrete implementation of a {@link PDFViewerController} using poppler-utils for PDF rendering.
 */
public class PDFToPPMLocalController implements PDFViewerController {

    /**
     * The originally converted and loaded images.
     */
    private ArrayList<ImageIcon> originalImages;

    /**
     * A list containing the rendered images and panels currently in use.
     */
    private ArrayList<PDFToPPMImageWrapper> imageWrapper;

    /**
     * The number of the currently displayed page.
     */
    private int currImageNumber = 0;

    /**
     * The default constructor.
     */
    PDFToPPMLocalController() {
        this.originalImages = new ArrayList<>();
        this.imageWrapper = new ArrayList<>();
    }


    @Override
    public void createNewController() {
        imageWrapper.add(new PDFToPPMImageWrapper());
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
    public void loadNewFile(String filePath) throws IOException, PDFException, PDFSecurityException, InterruptedException, ExecutionException {
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

        System.out.println("start " + System.currentTimeMillis());

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

        imageWrapper.forEach(PDFToPPMImageWrapper::prerenderImages);

        imageWrapper.forEach(w -> w.getImagePanel().getRootPane().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)));
//        progress.close();


        this.gotoFirst();
    }

    /**
     * Calls {@link PDFToPPMConverter#refreshImage()} on every wrapper.
     */
    private void refreshImage() {
        imageWrapper.forEach(PDFToPPMImageWrapper::refreshImage);
    }

    /**
     * A wrapper containing pre-rendered images and panels for the different display-locations.
     */
    private class PDFToPPMImageWrapper {
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
        PDFToPPMImageWrapper() {
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
        private void prerenderImages() {
            System.out.println("rb " + System.currentTimeMillis());

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

            System.out.println("re " + System.currentTimeMillis());
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

    /**
     * This class converts a page of a PDF-document to an image using {@link Callable}s for parallel execution.
     */
    private class PDFToPPMConverter implements Callable<BufferedImage> {

        /**
         * The number of the page to be converted.
         */
        private final int page;

        /**
         * The path of the document to be converted.
         */
        private final String filePath;

        /**
         * A {@link CountDownLatch} managing the convert-process.
         */
        private final CountDownLatch latch;

//        private final ProgressWindow progress;

        /**
         * The Constructor.
         *
         * @param page     The page-number.
         * @param filePath The path to the PDF-document.
         * @param latch    The {@link CountDownLatch}.
         */
        PDFToPPMConverter(int page, String filePath, CountDownLatch latch) {
            this.page = page;
            this.filePath = filePath;
            this.latch = latch;
//            this.progress = progress;
        }

        @Override
        public BufferedImage call() throws Exception {
            ProcessBuilder convertProcessBuilder = new ProcessBuilder("pdftoppm", "-f", Integer.toString(page), "-l", Integer.toString(page), "-rx", "200", "-ry", "200", "-png", filePath);
            Process convertProcess;
            BufferedImage image = null;

            try {
                convertProcess = convertProcessBuilder.start();

                ArrayList<Byte> imageBytes = new ArrayList<>();

                for (byte b : IOUtils.toByteArray(convertProcess.getInputStream())) {
                    imageBytes.add(b);
                }
                ByteArrayInputStream bais = new ByteArrayInputStream(ArrayUtils.toPrimitive(imageBytes.toArray(new Byte[imageBytes.size()])));

                image = ImageIO.read(bais);

                latch.countDown();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println(page + " " + System.currentTimeMillis());
//            progress.increaseProgress();

            return image;
        }
    }
}
