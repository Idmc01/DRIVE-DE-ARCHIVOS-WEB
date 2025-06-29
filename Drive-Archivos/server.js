const net = require('net');
const express = require('express');
const bodyParser = require('body-parser');

const JAVA_SERVER_HOST = 'localhost';
const JAVA_SERVER_PORT = 12345;

const app = express();
const PORT = 3000;

app.use(bodyParser.json());

app.use((req, res, next) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type');
  next();
});

app.post('/sendCommand', (req, res) => {
  const commandJson = JSON.stringify(req.body);

  const client = new net.Socket();
  let responseBuffer = '';

  client.connect(JAVA_SERVER_PORT, JAVA_SERVER_HOST, () => {
    console.log('✅ Conectado al servidor Java');
    client.write(commandJson + '\n');
  });

  client.on('data', (data) => {
  const respuesta = data.toString();
  console.log('📥 Respuesta cruda recibida desde Java:', respuesta);
  try {
    const parsed = JSON.parse(respuesta);
    console.log('✅ Respuesta parseada:', parsed);
    res.json(parsed);
  } catch (error) {
    console.error('❌ Error al parsear JSON:', error);
    res.status(500).send('Error en formato de respuesta del servidor Java');
  }
  client.write(commandJson + '\n');
  client.end(); // Cerrar la conexión después de responder
});

  client.on('error', (err) => {
    console.error('❌ Error al conectar al servidor Java:', err.message);
    res.status(500).send('No se pudo conectar al servidor Java');
  });
});

app.listen(PORT, () => {
  console.log(`🌐 Node Proxy escuchando en http://localhost:${PORT}`);
});
