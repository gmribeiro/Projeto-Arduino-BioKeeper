const express = require('express');
const pool = require('../db');
const router = express.Router();

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
