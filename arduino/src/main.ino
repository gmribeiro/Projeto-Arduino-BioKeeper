#define BLYNK_TEMPLATE_ID "TMPL24qIxDkjB"
#define BLYNK_TEMPLATE_NAME "Refrigerador BioKeeper"
#define BLYNK_AUTH_TOKEN "uNoxiV-1Hjx_mTuDaZu1MH7tBJm1xURw"

#define BLYNK_PRINT Serial

#include <WiFi.h>
#include <WiFiClient.h>
#include <BlynkSimpleEsp32.h>
#include "DHT.h"
#include <SPI.h>
#include <SD.h>

// --- CONFIGURAÇÕES DE REDE ---
char ssid[] = "POCO";
char pass[] = "notebook";

// --- CONFIGURAÇÕES DE PINOS ---
#define DHTPIN 4
#define LED_ECO_PIN 2
#define SD_CS_PIN 5         // Pino Chip Select do SD
#define BOTAO_PORTA_PIN 14  // Pino do Botão da Porta (Alterado para não conflitar com SD)

// --- OBJETOS E SENSORES ---
#define DHTTYPE DHT22
DHT dht(DHTPIN, DHTTYPE);
BlynkTimer timer;
int timerLedId;

// --- VARIÁVEIS GLOBAIS DE CONTROLE ---
unsigned long tempoUltimoAvisoPorta = 0;
bool portaEstavaFechada = true;
String estadoPortaAtual = "FECHADA";

const unsigned long INTERVALO_AVISO_PORTA = 10000;

bool avisoImediatoEnviado = false;
bool avisoMaximoEnviado = false;
unsigned long tempoInicioAlta = 0;
const unsigned long TEMPO_LIMITE_ALERTA = 15000;

// Variáveis do Cartão SD
bool sdConectado = false; 
unsigned long tempoUltimaGravacaoSD = 0;
bool primeiraGravacao = true; // Garante que a primeira gravação ocorra assim que ligar

// --- FUNÇÃO DO MODO ECO ---
void piscarLedEco() {
  digitalWrite(LED_ECO_PIN, !digitalRead(LED_ECO_PIN));
}

BLYNK_WRITE(V2) {
  int statusBotao = param.asInt();
  if (statusBotao == 1) {
    Serial.println(">>> Modo Eco ATIVADO! Iniciando pisca-pisca...");
    timer.enable(timerLedId);
    Blynk.logEvent("modo_eco_ativado", "O sistema BioKeeper entrou no Modo Eco!");
  } else {
    Serial.println(">>> Modo Eco DESATIVADO. Apagando LED...");
    timer.disable(timerLedId);
    digitalWrite(LED_ECO_PIN, LOW);
  }
}

// --- FUNÇÃO DE VERIFICAÇÃO DA PORTA ---
void verificarPorta() {
  int estadoBotao = digitalRead(BOTAO_PORTA_PIN);

  if (estadoBotao == HIGH) {
    // PORTA ABERTA
    estadoPortaAtual = "ABERTA";

    if (portaEstavaFechada) {
      Serial.println("ALERTA: A porta do refrigerador foi aberta!");
      Blynk.logEvent("porta_aberta", "Atenção: A porta do BioKeeper foi aberta!");
      tempoUltimoAvisoPorta = millis();
      portaEstavaFechada = false;
    }
    else if (millis() - tempoUltimoAvisoPorta >= INTERVALO_AVISO_PORTA) {
      Serial.println("ALERTA REPETIDO: A porta continua aberta!");
      Blynk.logEvent("porta_aberta", "Aviso: A porta CONTINUA aberta!");
      tempoUltimoAvisoPorta = millis();
    }
  }
  else {
    // PORTA FECHADA
    estadoPortaAtual = "FECHADA";
    
    if (!portaEstavaFechada) {
      Serial.println("Porta fechada. Sistema normalizado.");
      portaEstavaFechada = true;
    }
  }
}

// --- FUNÇÃO DE LEITURA E GRAVAÇÃO ---
void lerEEnviarDados() {
  float temperatura = dht.readTemperature();
  float umidade = dht.readHumidity();

  if (isnan(temperatura) || isnan(umidade)) {
    Serial.println("Erro: Falha na leitura do sensor DHT!");
    return;
  }

  // --- ATUALIZAÇÃO EM TEMPO REAL NO SERIAL E BLYNK (A cada 2 segundos) ---
  Serial.print("Temp: ");
  Serial.print(temperatura);
  Serial.print(" °C  |  Umid: ");
  Serial.print(umidade);
  Serial.println(" %");

  Blynk.virtualWrite(V0, temperatura);
  Blynk.virtualWrite(V1, umidade);

  // --- GRAVAÇÃO NO CARTÃO SD INDEPENDENTE (A cada 5 minutos ou ao ligar) ---
  if (sdConectado && (primeiraGravacao || (millis() - tempoUltimaGravacaoSD >= 300000L))) {
    File logFile = SD.open("/log_teste.csv", FILE_APPEND);
    if (logFile) {
      logFile.print(millis() / 1000);
      logFile.print("s;");
      logFile.print(temperatura);
      logFile.print(";");
      logFile.print(umidade);
      logFile.print(";");
      logFile.println(estadoPortaAtual);
      logFile.close(); // Salva fisicamente na hora contra quedas de energia!
      
      Serial.println(">>> Dados salvos com sucesso no Cartão SD!");
      tempoUltimaGravacaoSD = millis(); // Reseta o cronômetro de 5 minutos
      primeiraGravacao = false;         // Desativa a flag de primeira gravação
    } else {
      Serial.println("Erro ao abrir /log_teste.csv para gravação!");
    }
  }

  // --- LÓGICA DE ALERTAS DE TEMPERATURA ---
  if (temperatura > 10.0) {
    if (!avisoImediatoEnviado) {
      Serial.println("ALERTA: Temperatura Alta!");
      Blynk.logEvent("alerta_temperatura", "Temperatura alta: " + String(temperatura) + "°C");
      avisoImediatoEnviado = true;
      tempoInicioAlta = millis();
    }
    if (!avisoMaximoEnviado && (millis() - tempoInicioAlta >= TEMPO_LIMITE_ALERTA)) {
      Serial.println("ALERTA MÁXIMO: Quente há muito tempo!");
      Blynk.logEvent("alerta_maximo", "CRÍTICO: Temperatura alta há mais de 15 segundos!");
      avisoMaximoEnviado = true;
    }
  } else {
    avisoImediatoEnviado = false;
    avisoMaximoEnviado = false;
    tempoInicioAlta = 0;
  }
}

void setup() {
  Serial.begin(115200);
  delay(1000);
  Serial.println("\n--- Iniciando Refrigerador BioKeeper ---");

  // Configuração dos Pinos
  pinMode(LED_ECO_PIN, OUTPUT);
  digitalWrite(LED_ECO_PIN, LOW);
  pinMode(BOTAO_PORTA_PIN, INPUT_PULLUP); // Fio do botão no pino 14!

  // Inicializa o Sensor DHT
  dht.begin();

  // Inicializa o Cartão SD
  Serial.print("Inicializando SD Card... ");
  if (!SD.begin(SD_CS_PIN)) {
    Serial.println("FALHOU! Os dados não serão salvos.");
    sdConectado = false;
  } else {
    Serial.println("SUCESSO!");
    sdConectado = true;
    
    // Cria um cabeçalho no arquivo ao reiniciar o sistema
    File logFile = SD.open("/log_teste.csv", FILE_APPEND);
    if (logFile) {
      logFile.println("\n--- SISTEMA REINICIADO ---");
      logFile.println("Tempo;Temperatura;Umidade;Porta");
      logFile.close();
    }
  }

  // Inicializa o WiFi e o Blynk
  Serial.println("Conectando ao WiFi e Blynk...");
  Blynk.begin(BLYNK_AUTH_TOKEN, ssid, pass);

  // Configura os Timers
  timer.setInterval(2000L, lerEEnviarDados);   // Lê o sensor e atualiza Blynk a cada 2s
  timer.setInterval(500L, verificarPorta);     // Verifica a porta a cada 0.5s
  
  timerLedId = timer.setInterval(1000L, piscarLedEco);
  timer.disable(timerLedId);                   // O LED Eco começa desligado
}

void loop() {
  Blynk.run();
  timer.run();
}
