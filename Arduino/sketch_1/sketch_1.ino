/**
 * Generating a Tone 
 */
const int ledPin = 13;
const int outPin = 11;
const int freq = 2000;     // tone frequency
const int duration = 1000; // tone duration
const int pause = 500;     // pause between plays 

void setup() { 
  pinMode(ledPin, OUTPUT);
  pinMode(outPin, OUTPUT);
}

void loop() {    // until power is removed
  digitalWrite(ledPin, HIGH); 
  tone(outPin, freq);   
  delay(duration);     
  noTone(outPin);
  digitalWrite(ledPin, LOW); 
  delay(pause);
} 
