#include "DHT.h"

#define DHTPIN 3
#define DHTTYPE DHT22
#define PINOUV 13
#define PINOSENSOR 2

DHT dht(DHTPIN, DHTTYPE);

int velocidade = 100; 

void setup() {
  Serial.begin(9600);
  dht.begin();
  
  pinMode(PINOUV, OUTPUT);
  pinMode(PINOSENSOR, INPUT_PULLUP);
  
  Serial.println("--- Sistema de Conservação UV (DHT22) Ativo ---");
}

void loop() {
  float h = dht.readHumidity();
  float t = dht.readTemperature();

  if (isnan(h) || isnan(t)) {
    Serial.println("Erro: Falha na leitura do DHT22!");
  } else {
    Serial.print("Umidade: "); Serial.print(h); Serial.print("% | ");
    Serial.print("Temp: "); Serial.print(t); Serial.println("°C");
  }

  if (digitalRead(PINOSENSOR) == LOW) {
    digitalWrite(PINOUV, HIGH);
    delay(velocidade);
    digitalWrite(PINOUV, LOW);
    delay(velocidade);
  } else {
    digitalWrite(PINOUV, LOW);
    Serial.println("ALERTA: Porta Aberta! UV Desligado.");
    delay(500); // Evita spam no Serial
  }
}
