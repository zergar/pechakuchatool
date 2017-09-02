# PechaKuchaTool

This program can be used for _Pecha Kucha_-Sessions by displaying the next slide every 20 seconds.
It consists of a presentation-screen which shows the slides to the audience as well as a lookup-screen which shows the current slide as well as a timer showing the seconds until the next slide shows up.
This time can also be shown on an Arduino with a NeoPixel-Shield.

## Running the Program

Jar available at the *releases*-section.

### Prerequisites

Install [poppler-utils](https://poppler.freedesktop.org/) if you want to use the PDFToPPM-Local-renderer.
If you want to establish a Arduino-connection, you need to install [rxtx](http://rxtx.qbang.org/wiki/index.php/Download).
Write down, where rxtx has been installed.

### Starting the program

Start the program by calling `java -jar pechakuchatool-0.9-all.jar`.
A setup-screen should appear.
If you want to also use an Arduino, run `java -Djava.library.path=§rxtx-location§:§jni-location§ -jar pechakuchatool-0.9-all.jar`, where `§rxtx-location§` is the location of your rxtx-lib and `§jni-location§` is the location of your jni.
The latter one is `/usr/lib/jni` by default when using ubuntu.

If you mark the jar as executable, you might be able to run it from your desktop right away.

### Setting up the program

The setup-screen the program launches with allows you to make some settings before starting a presentation.
The first two options allow you to set the location of the presentation-screen as well as the lookup-screen.
By default, the lookup-screen is set to the internal monitor or primary display, while the presentation-screen is set to the first external monitor. 
If there is just one monitor, both screens are shown on the same display.
Next, there is an option to choose the [PDF-renderer](#renderers) to use.
By default, the "IcePDF"-renderer is selected.
Note, that the "PDFToPPM-Local"-renderer should only be used if _poppler-utils_ has been installed.
Last it can be chosen, whether an Arduino-connection shall be established or not.

### Using the Program

After the setup has been completed, a file to present has to be picked.
To do so, click `File -> Open File` to open a PDF-Document.
Alternatively, press `Strg + O` on your keyboard.
The presentation should now load.
Depending on the renderer, this can take up to 30 seconds.
After loading the file, a couple of things can be done:

+ Skipping through the slides by pressing `Up, Down, Left, Right, Page-Up, Page-Down`
+ Starting the Presentation by clicking `Presentation -> Start Presentation` or pressing `Space`

While the presentation is running, skipping through the slides is prohibited.
 
You can reset the running presentation by clicking `Presentation -> Reset Presentation` or pressing `R`.
This will also bring you to the first slide of your presentation.

To close the program, press `File -> Exit`.

### Using an Arduino

To use an Arduino, first the Arduino-sketch has to be loaded to the arduino itself.
It can be found inside the `arduino`-folder at the projects' root.
Note, that the pin used to connect to your shield might differ depending on your NeoPixel-shield.
When connected to power, the shield should display `00` on its LED-matrix.
Also don't forget to load the rxtx-library as mentioned above.

**Please don't depend on the time shown on the Arduino as it is currently quite buggy.** 
The connection might crash during your presentation, leading the arduino to show the last time before the crash happened.
Also at the moment, the connection can only be reestablished by restarting the program.

## <a name="renderers"></a>Available PDF-Renderers

### IcePDF-renderer

This is the default PDF-renderer of this program, requiring no prerequisites regarding third party programmes.
Advantages of this renderer are that it is fast on startup, but might have some issues regarding correct rendering as well as being slow at page changes.

### poppler-utils renderer

This is the second renderer, requiring [poppler-utils](https://poppler.freedesktop.org/) to be installed and accessible on the system (e.g. by adding it to the path).
Advantages are correct rendering and quick page changes, while requiring a long waiting period during the initial loading phase of up to 30 seconds.

## Building the program

For building the program, type `./gradlew run` on UNIX-based systems or `gradlew.bat run` if using Windows.
For building the jar type `./gradlew fatJar` on UNIX-based systems or `gradlew.bat fatJar` if using Windows.


## Feedback / Improvements

Feel free to write an issue or set a pull request if you encounter any problems.


Gereon Dusella for ISE @ TU Berlin