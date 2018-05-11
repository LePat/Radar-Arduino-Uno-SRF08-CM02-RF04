/**
 * Test programm for onboard wireless RADAR system
 */
#include <Servo.h>
#include <Wire.h>

Servo myservo;  // create servo object to control a servo

bool play = false;
int pos = 0;    // variable to store the servo position
int reading = 0; // lecture de la réponse du SRF08
int ledPin =  LED_BUILTIN; // 13

void setup() {
  Wire.begin(8);                // join i2c bus with address #8
  Wire.onRequest(requestEvent); // register event
  Serial.begin(9600);          // start serial communication at 9600bps
  // initialize digital pin LED_BUILTIN as an output.
  pinMode(ledPin, OUTPUT);
  myservo.attach(9);  // attaches the servo on pin 9 to the servo object
}

void loop() {
  if (play) {
    digitalWrite(ledPin, HIGH);   // turn the LED on (HIGH is the voltage level)
    for (pos = 10; pos <= 170; pos += 6) { // goes from 0 degrees to 180 degrees
      getRange();
      myservo.write(pos);              // tell servo to go to position in variable 'pos'
      delay(15);                       // waits 15ms for the servo to reach the position
    }
    digitalWrite(ledPin, LOW);
    for (pos = 170; pos >= 10; pos -= 6) { // goes from 180 degrees to 0 degrees
      getRange();
      myservo.write(pos);              // tell servo to go to position in variable 'pos'
      delay(15);                       // waits 15ms for the servo to reach the position
    }
  } else {
    digitalWrite(ledPin, HIGH);   // turn the LED on (HIGH is the voltage level)
    delay(500);
    digitalWrite(ledPin, LOW);   // turn the LED on (HIGH is the voltage level)
    delay(5000);
  }
}

// function that executes whenever data is requested by master
// this function is registered as an event, see setup()
void requestEvent() {
  Serial.println("requestEvent");   // print the reading
  Wire.write(reading); // retourne la dernière valeur mesurée as expected by master
}

void getRange() {
  // step 1: instruct sensor to read echoes
  Wire.beginTransmission(112); // transmit to device #112 (0x70)
  // the address specified in the datasheet is 224 (0xE0)
  // but i2c adressing uses the high 7 bits so it's 112
  Wire.write(byte(0x00));      // sets register pointer to the command register (0x00)
  Wire.write(byte(0x50));      // command sensor to measure in "inches" (0x50)
  // use 0x51 for centimeters
  // use 0x52 for ping microseconds
  Wire.endTransmission();      // stop transmitting

  // step 2: wait for readings to happen
  delay(70);                   // datasheet suggests at least 65 milliseconds

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
    Serial.println(reading);   // print the reading
  }

  //delay(250);                  // wait a bit since people have to read the output :)
}
