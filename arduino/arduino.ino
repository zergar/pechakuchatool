#include <Adafruit_NeoPixel.h>
#ifdef __AVR__
#include <avr/power.h>
#endif
#define PIN 13
//Grundeinstellung (Anzahl der LEDs, usw.)
Adafruit_NeoPixel strip = Adafruit_NeoPixel(40, PIN, NEO_GRB + NEO_KHZ800);


int ciphers[10][15] = {
  {
    1, 1, 1,
    1, 0, 1,
    1, 0, 1,
    1, 0, 1,
    1, 1, 1
  },
  { 0, 0, 1,
    0, 1, 1,
    1, 0, 1,
    0, 0, 1,
    0, 0, 1
  },
  { 1, 1, 1,
    0, 0, 1,
    0, 1, 1,
    1, 0, 0,
    1, 1, 1
  },
  { 1, 1, 1,
    0, 0, 1,
    1, 1, 1,
    0, 0, 1,
    1, 1, 1
  },
  { 1, 0, 1,
    1, 0, 1,
    1, 1, 1,
    0, 0, 1,
    0, 0, 1
  },
  { 1, 1, 1,
    1, 0, 0,
    1, 1, 1,
    0, 0, 1,
    1, 1, 0
  },
  { 0, 1, 1,
    1, 0, 0,
    1, 1, 1,
    1, 0, 1,
    1, 1, 1
  },
  { 1, 1, 1,
    0, 0, 1,
    0, 1, 0,
    0, 1, 0,
    0, 1, 0
  },
  { 1, 1, 1,
    1, 0, 1,
    1, 1, 1,
    1, 0, 1,
    1, 1, 1
  },
  { 1, 1, 1,
    1, 0, 1,
    1, 1, 1,
    0, 0, 1,
    1, 1, 0
  }
};




void setup() {
  strip.begin();
  strip.show(); // Alle LEDs initialisieren
  Serial.begin(9600);

  showCiphers(1, 0);
}

void loop() {
  String readString;
  while (Serial.available()) {
    delay(3);  //delay to allow buffer to fill
    if (Serial.available() > 0) {
      char c = Serial.read();  //gets one byte from serial buffer
      readString += c; //makes the string readString
    }
  }
  if (readString.length() > 0) {
    Serial.println(readString); //see what was received

    int c10 = readString.substring(0, 1).toInt();
    int c1 = readString.substring(1, 2).toInt();

    showCiphers(c10, c1);

  }
}



int *combineCiphers(int * combine, int c1[], int c2[]) {
  for (int i = 0; i < 5; i++) {
    int rowComb = i * 8;
    int rowC = i * 3;

    combine[rowComb + 0] = 0;
    combine[rowComb + 1] = c1[rowC + 0];
    combine[rowComb + 2] = c1[rowC + 1];
    combine[rowComb + 3] = c1[rowC + 2];
    combine[rowComb + 4] = 0;
    combine[rowComb + 5] = c2[rowC + 0];
    combine[rowComb + 6] = c2[rowC + 1];
    combine[rowComb + 7] = c2[rowC + 2];
  }
}

void showCiphers(int ciph1, int ciph2) {
  int combine[40];
  combineCiphers(combine, ciphers[ciph1], ciphers[ciph2]);

  for (int i = 0; i < 40; i++) {
//    Serial.print(combine[i]);
    strip.setPixelColor(i, combine[i] * strip.Color(255, 255, 255));
  }
  strip.setBrightness(1);
  strip.show();
}


