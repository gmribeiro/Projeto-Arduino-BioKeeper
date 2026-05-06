const express = require('express');
const cors = require('cors');
const sensorRoutes = require('./routes/sensors');
const app = express();
const port = process.env.PORT || 4000;

app.use(cors());
app.use(express.json());
app.use('/api/sensors', sensorRoutes);

app.get('/', (req, res) => {
  res.send({ message: 'Biokeeper backend is running.' });
});

app.listen(port, () => {
  console.log(`Server listening on port ${port}`);
});
