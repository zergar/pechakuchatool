package logic;

import java.awt.*;

/**
 * This class is a placeholder which enables choosing a "None"-Display, disabling the Presentation-Screen.
 */
public class NoGraphicsDevice extends GraphicsDevice{
    @Override
    public int getType() {
        return 0;
    }

    @Override
    public String getIDstring() {
        return null;
    }

    @Override
    public GraphicsConfiguration[] getConfigurations() {
        return new GraphicsConfiguration[0];
    }

    @Override
    public GraphicsConfiguration getDefaultConfiguration() {
        return null;
    }

    @Override
    public String toString() {
        return "None";
    }
}
