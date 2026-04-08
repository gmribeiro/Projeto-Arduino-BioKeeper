# Dashboard Biokeeper

Dashboard moderno para manutenção de vegetais com ESP32, telemetria de sensores e controle de UV.

## Recursos
- Painéis de temperatura, umidade, umidade do solo e índice UV
- API em tempo real para ingestão de dados do ESP32
- Frontend em React + Tailwind
- Backend em Node.js + Express
- Armazenamento em MySQL

## Configuração
1. Instale as dependências do backend:
   ```bash
   npm install
   ```
2. Instale as dependências do frontend:
   ```bash
   cd frontend
   npm install
   ```
3. Crie um arquivo `.env` na raiz:
   ```ini
   DB_HOST=localhost
   DB_USER=root
   DB_PASSWORD=seu_password
   DB_NAME=biokeeper
   ```
4. Inicie o backend:
   ```bash
   npm run dev
   ```
5. Inicie o frontend:
   ```bash
   cd frontend
   npm run dev
   ```

## Esquema do Banco de Dados
```sql
CREATE DATABASE biokeeper;
USE biokeeper;

CREATE TABLE sensor_readings (
  id INT AUTO_INCREMENT PRIMARY KEY,
  temperature DECIMAL(5,2) NOT NULL,
  humidity DECIMAL(5,2) NOT NULL,
  soil_moisture DECIMAL(5,2) NOT NULL,
  uv_index DECIMAL(5,2) NOT NULL,
  fan_power BOOLEAN DEFAULT FALSE,
  uv_active BOOLEAN DEFAULT FALSE,
  reading_time DATETIME DEFAULT CURRENT_TIMESTAMP
);
```

## API
- `GET /api/sensors/latest` - snapshot mais recente do sensor
- `GET /api/sensors/history` - histórico recente para gráficos

## Observações
Este projeto foi desenvolvido para um cooler contendo vegetais usando sensores ESP32 e controle de iluminação UV. O frontend é construído com React e Tailwind para uma experiência de dashboard polida.
