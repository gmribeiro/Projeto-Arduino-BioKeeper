#include "DHT.h"
#define DHTPIN 4 //PINO DO SENSOR DE TEMPERATURA E UMIDADE
#define DHTTYPE DHT22
#define LED_PIN 2 //PINO DO LED UV 

DHT dht (DHTPIN, DHTTYPE);

unsigned long lastTimeSensor = 0;
unsigned long lastTimeLed = 0;
const long intervalSensor = 1000; //INTERVALO DO SENSOR
const long intervalLed = 100; //INTERVALO DO LED

void setup() {
  Serial.begin(115200);
  pinMode(LED_PIN, OUTPUT);
  Serial.println("Iniciando sensor DHT...");
  dht.begin(); 
}
void loop() {
  unsigned long currentTime = millis();
  if (currentTime - lastTimeLed >= intervalLed) {
    lastTimeLed = currentTime;
    digitalWrite(LED_PIN, !digitalRead(LED_PIN)); // Liga o LED
  }
  if (currentTime - lastTimeSensor >= intervalSensor) {
    lastTimeSensor = currentTime;
    float umidade = dht.readHumidity();
    float temperatura = dht.readTemperature();

    if (isnan(umidade) || isnan(temperatura)) {
    Serial.println("Falha ao ler o sensor!");
    return;
    }
    Serial.print("Umidade: ");
    Serial.print(umidade);
    Serial.print("%  |  Temperatura: ");
    Serial.print(temperatura);
    Serial.println("°C");
  }
}
