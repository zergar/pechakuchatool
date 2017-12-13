package main;

import logic.PDFToPPMLocalController;
import logic.Saveable;
import org.icepdf.core.exceptions.PDFException;
import org.icepdf.core.exceptions.PDFSecurityException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;
import java.util.stream.Collectors;

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
 *
 */
public class PrerenderMain {

    /**
     * The {@link Logger}.
     */
    private static final Logger LOG = Logger.getLogger(PrerenderMain.class.getName());

    /**
     * The path of the folder containing the presentations to prerender.
     */
    private File inputPath;

    /**
     * The constructor.
     * @param path The inputPath
     */
    public PrerenderMain(String path) {
        this.inputPath = new File(path);

        setup();
    }

    /**
     * The setup-method.
     */
    private void setup() {
        ArrayList<File> files = new ArrayList<>(Arrays.asList((inputPath.listFiles())));

        files.stream().filter(f -> f.getName().endsWith(".pdf")).collect(Collectors.toList())
                .parallelStream().forEach(this::convertPresentation);
    }

    /**
     * The method which manages the conversion.
     * @param file The path of the presentation to be rendered.
     */
    private void convertPresentation(File file) {
        LOG.info("Starting rendering " + file.getName());

        PDFToPPMLocalController controller = new PDFToPPMLocalController();
        try {
            controller.loadNewFile(file.getAbsolutePath(), false);
            controller.savePresentationToFile(new File(file.getAbsolutePath().concat(".pktool")));

            LOG.info("Finished rendering " + file.getName());

        } catch (IOException | PDFException | PDFSecurityException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}
