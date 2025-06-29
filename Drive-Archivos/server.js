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
    console.log('📥 Respuesta cruda recibida desde Java:', data.toString());
    responseBuffer += data.toString();
  });

  client.on('end', () => {
    console.log('Socket cerrado. Respuesta completa:', responseBuffer);
    try {
        const parsedResponse = JSON.parse(responseBuffer);
        console.log('✅ Respuesta parseada:', parsedResponse);
        res.json(parsedResponse);
    } catch (error) {
        console.error('❌ Error al parsear JSON:', error);
        res.status(500).send('Error al parsear respuesta del servidor Java');
    }
});

  client.on('error', (err) => {
    console.error('❌ Error al conectar al servidor Java:', err.message);
    res.status(500).send('No se pudo conectar al servidor Java');
  });
});

app.listen(PORT, () => {
  console.log(`🌐 Node Proxy escuchando en http://localhost:${PORT}`);
});
