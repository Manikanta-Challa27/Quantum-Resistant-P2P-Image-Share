const express = require('express');
const http = require('http');
const socketIo = require('socket.io');

const app = express();
const server = http.createServer(app);
const io = socketIo(server, {
  cors: {
    origin: "*",
    methods: ["GET", "POST"]
  }
});

const PORT = 8080;

let clients = [];

io.on('connection', (socket) => {
  console.log(`✅ User connected: ${socket.id}`);
  clients.push(socket.id);

  if (clients.length === 2) {
    console.log(`Two users connected. Designating initiator: ${clients[0]}`);
    io.to(clients[0]).emit('start-webrtc');
  }

  socket.on('signal', (data) => {
    // Find the OTHER client and send the signal directly to them.
    const otherClientId = clients.find(id => id !== socket.id);
    if (otherClientId) {
      console.log(`Relaying signal from ${socket.id} to ${otherClientId}`);
      io.to(otherClientId).emit('signal', data);
    }
  });

  socket.on('disconnect', () => {
    console.log(`❌ User disconnected: ${socket.id}`);
    clients = clients.filter(id => id !== socket.id);
    // If a client disconnects, you may want to reset the state for a new pair.
    if (clients.length < 2) {
        console.log("Waiting for more users to connect.");
    }
  });
});

server.listen(PORT, () => {
  console.log(`🚀 Signaling Server is running on http://localhost:${PORT}`);
});
