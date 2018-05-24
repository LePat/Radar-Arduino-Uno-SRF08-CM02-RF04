/**
 * Test programm for onboard wireless RADAR system
 */
#include <Servo.h>
#include <Wire.h>

Servo myservo;  // create servo object to control a servo

bool play = false;
byte command = 0;
bool requeteEnCours = false;
int pos = 10;    // variable to store the servo position
int pasAngle = 4;    // pas de l'angle
int reading = 0; // lecture de la réponse du SRF08
int ledPin =  LED_BUILTIN; // 13

void setup() {
  Wire.begin(8);                // join i2c bus with address #8
  Wire.onRequest(requestEvent); // register event
  Wire.onReceive(receiveEvent); // register event
//  Serial.begin(9600);          // start serial communication at 9600bps
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(ledPin, OUTPUT);
  myservo.attach(9);  // attaches the servo on pin 9 to the servo object
  // range au moins une fois
  getRange();
}

void loop() {
  // =============================
  // partie scanner, radar actif
  // =============================
  if (play) { // si radar actif alors scanner
    digitalWrite(ledPin, HIGH);   // turn the LED on (HIGH is the voltage level)
    // repart de la dernière position connue (l'indice de la boucle n'est pas réinitialisé
    for (; pos < 170; pos += pasAngle) { // goes from 10 degrees to 170 degrees
      if (!play) { // si radar stopper alors quitter la boucle de scan
        break;
      }
      getRange();
      // envoyer la distance mesurée lorsque le capteur est droit
      if (pos <= (90 + pasAngle / 2) && pos >= (90 - pasAngle / 2)) {
//        Serial.println(reading);
      }
      myservo.write(pos);              // tell servo to go to position in variable 'pos'
      delay(10);                       // waits 15ms for the servo to reach the position
    }
  }
  if (play) { // si radar actif alors scanner
    digitalWrite(ledPin, LOW);
    for (; pos > 10; pos -= pasAngle) { // goes from 170 degrees to 10 degrees
      if (!play) { // si radar stopper alors quitter la boucle de scan
        break;
      }
      getRange();
      // envoyer la distance mesurée lorsque le capteur est droit
      if (pos <= (90 + pasAngle / 2) && pos >= (90 - pasAngle / 2)) {
//        Serial.println(reading);
      }
      myservo.write(pos);              // tell servo to go to position in variable 'pos'
      delay(10);                       // waits 15ms for the servo to reach the position
    }
  }
  
  // =============================
  // partie attente, radar inactif
  // =============================
  if (!play) { // si radar à l'arrêt alors attendre
    digitalWrite(ledPin, HIGH);   // turn the LED on (HIGH is the voltage level)
    for (int i = 0; i < 50; i++) {
      if (play) { // si démarrer le radar alors quitter la boucle d'attente
        break;
      }
      delay(10);
    }
  }
  if (!play) { // si radar à l'arrêt alors attendre
    digitalWrite(ledPin, LOW);   // turn the LED on (HIGH is the voltage level)
    for (int i = 0; i < 500; i++) {
      if (play) { // si démarrer le radar alors quitter la boucle d'attente
        break;
      }
      delay(10);
    }
  }
}

// function that executes whenever data is requested by master
// this function is registered as an event, see setup()
void requestEvent() {
  switch (command) {
    case 0x00:
      play = false;
      Wire.write(0); 
      break;
    case 0x01:
      play = true;
      Wire.write(1); 
      break;
    case 0x02:
      Wire.write(reading); // retourne la dernière valeur mesurée as expected by master
      Wire.write(pos); // retourne la dernière valeur mesurée as expected by master
      break;
    case 0x03:
      Wire.write("Pong");
      break;
  }
//  Serial.println("requestEvent");
//  Serial.println(command);
  requeteEnCours = false;
}

// function that executes whenever data is received from master
// this function is registered as an event, see setup()
void receiveEvent(int nbBytes) {
  requeteEnCours = true;
  command = Wire.read();
//  Serial.println("receiveEvent");
//  Serial.println(nbBytes);
//  Serial.println(command);
}

void getRange() {
  if (!requeteEnCours) {
    // step 1: instruct sensor to read echoes
    Wire.beginTransmission(112); // transmit to device #112 (0xE0 >> 2)
    // the address specified in the datasheet is 224 (0xE0)
    // but i2c adressing uses the high 7 bits so it's 112
    Wire.write(byte(0x00));      // sets register pointer to the command register (0x00)
    Wire.write(byte(0x51));      // command sensor to measure in "inches" (0x50) 
    // use 0x51 for centimeters
    // use 0x52 for ping microseconds
    Wire.endTransmission();      // stop transmitting
  
    // step 2: wait for readings to happen
    delay(30);                   // datasheet suggests at least 65 milliseconds
  
    // step 3: instruct sensor to return a particular echo reading
    Wire.beginTransmission(112); // transmit to device #112
    Wire.write(byte(0x02));      // sets register pointer to echo #1 register (0x02)
    Wire.endTransmission();      // stop transmitting
  
    // step 4: request reading from sensor
    Wire.requestFrom(112, 2);    // request 2 bytes from slave device #112
  
    // step 5: receive reading from sensor
    if (2 <= Wire.available()) { // if two bytes were received
      reading = Wire.read();  // receive high byte (overwrites previous reading)
      reading = reading << 8;    // shift high byte to be high 8 bits
      reading |= Wire.read(); // receive low byte as lower 8 bits
      //Serial.println(reading);   // print the reading
    }
  
    //delay(250);                  // wait a bit since people have to read the output :)
  }
}
