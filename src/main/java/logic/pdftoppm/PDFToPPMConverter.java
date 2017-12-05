package logic.pdftoppm;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
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
 * This class converts a page of a PDF-document to an image using {@link Callable}s for parallel execution.
 */
public class PDFToPPMConverter implements Callable<BufferedImage> {

    /**
     * The java.util.{@link java.util.logging.Logger}
     */
    private static final Logger LOG = Logger.getLogger(PDFToPPMConverter.class.getName());

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
    public PDFToPPMConverter(int page, String filePath, CountDownLatch latch) {
        this.page = page;
        this.filePath = filePath;
        this.latch = latch;
//            this.progress = progress;
    }

    @Override
    public BufferedImage call() throws Exception {
        ProcessBuilder convertProcessBuilder = new ProcessBuilder("pdftoppm", "-f", Integer.toString(page),
                "-l", Integer.toString(page), "-r", "220", "-png", filePath);
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

        LOG.info("Finished rendering page " + page);
//            progress.increaseProgress();

        return image;
    }
}