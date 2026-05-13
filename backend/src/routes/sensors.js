const express = require('express');
const pool = require('../config/database');
const router = express.Router();

// POST - Receber dados do Arduino
router.post('/data', async (req, res) => {
  try {
    const { temperature, humidity, soil_moisture, uv_index } = req.body;

    // Validação básica
    if (temperature === undefined || humidity === undefined) {
      return res.status(400).json({ message: 'Temperature and humidity are required.' });
    }

    const query = `
      INSERT INTO sensor_readings (temperature, humidity, soil_moisture, uv_index)
      VALUES (?, ?, ?, ?)
    `;

    await pool.query(query, [
      temperature,
      humidity,
      soil_moisture || null,
      uv_index || null
    ]);

    console.log(`[${new Date().toISOString()}] Data received: Temp=${temperature}°C, Humidity=${humidity}%`);
    res.status(201).json({ message: 'Data saved successfully.' });
  } catch (error) {
    console.error('Error saving sensor data:', error);
    res.status(500).json({ message: 'Error saving sensor data.' });
  }
});

// GET - Último dado do sensor
router.get('/latest', async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT * FROM sensor_readings ORDER BY reading_time DESC LIMIT 1'
    );
    if (!rows.length) {
      return res.status(404).json({ message: 'No sensor data available.' });
    }
    res.json(rows[0]);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Error fetching latest sensor data.' });
  }
});

// GET - Histórico dos sensores (últimas 24 leituras)
router.get('/history', async (req, res) => {
  try {
    const [rows] = await pool.query(
      'SELECT temperature, humidity, soil_moisture, uv_index, reading_time FROM sensor_readings ORDER BY reading_time DESC LIMIT 24'
    );
    res.json(rows.reverse());
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Error fetching sensor history.' });
  }
});

module.exports = router;