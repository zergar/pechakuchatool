package logic;

import java.awt.*;

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
