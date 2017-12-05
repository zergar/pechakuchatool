package main;

import dto.SetupDTO;
import org.apache.commons.cli.*;
import viewComponents.ScreenSetupFrame;

import java.util.logging.Logger;

public class Main {

    public static final Logger LOG = Logger.getLogger(Main.class.getName());

    /**
     * The main-method
     *
     * @param args The arguments
     */
    public static void main(String[] args) {
        Options cliOptions = new Options();

        Option loadPDF = new Option("pdf", "loadPDF", true, "Set a PDF-File to load directly after launch.");
        cliOptions.addOption(loadPDF);

        Option loadPktool = new Option("pktool", "loadPktool", true, "UNSUPPORTED: Set a pktool-File to load directly after launch.");
        cliOptions.addOption(loadPktool);

        Option prerenderPresentations = new Option("prerender", true, "Prerender all PDF-Files of a given folder into pktool-Files.");
        cliOptions.addOption(prerenderPresentations);

        Option showHelp = new Option("h", "help", false, "Display this help.");
        cliOptions.addOption(showHelp);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(cliOptions, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("pechaKuchaTool.jar", cliOptions);

            System.exit(1);
            return;
        }


        if (cmd.hasOption("prerender")) {
            new PrerenderMain(cmd.getOptionValue("prerender"));
        } else if (cmd.hasOption("h")) {
            formatter.printHelp("pechaKuchaTool.jar", cliOptions);
        } else {

            SetupDTO settings = ScreenSetupFrame.getSettings();

            if (cmd.hasOption("pdf")) {
                new PechaKuchaMain(settings, cmd.getOptionValue("pdf"));
            } else {
                new PechaKuchaMain(settings);
            }
        }
    }
}
