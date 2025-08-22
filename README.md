# Quantum-Resistant P2P Image Share

A peer-to-peer (P2P) image transfer application that uses the **NTRU post-quantum algorithm** to provide end-to-end, quantum-resistant encryption.

---

## 📖 Overview

This project is a proof-of-concept for a secure, peer-to-peer media transfer system designed to be resistant to future quantum attacks.  
It allows two users on different devices to establish a direct connection and transfer full-color images, which are encrypted and decrypted using the **NTRU post-quantum cryptography (PQC)** algorithm.

The application is built on a **microservices architecture** and ensures that no unencrypted or encrypted image data is ever stored on a central server, providing maximum user privacy and security.

---

## 🏛 Architecture

The application consists of three core components that work together:

1. **Java Encryption Server**  
   A backend service built with Javalin that exposes an API to perform the heavy-duty NTRU encryption and decryption of image files.

2. **Node.js Signaling Server**  
   A lightweight backend service using Node.js and Socket.IO.  
   Its only job is to act as a "matchmaker," allowing two web clients to find each other and establish a direct WebRTC connection.

3. **Q-Vault Front-End Client**  
   A single-page web application built with HTML, Tailwind CSS, and JavaScript that users interact with.  
   It communicates with both backend servers to facilitate the secure, peer-to-peer transfer.

+----------------+ +----------------+
| User A Browser | | User B Browser|
| (index.html) | | (index.html) |
+-------+---------+ +---------+------+
| |
| (1. Find each other) |
| +-------------------+ |
+------->| Node.js Signaling |<---------+
| Server (Matchmaker) |
+-------------------+
| |
| (2. Encrypt/Decrypt) | (4. Decrypt)
| +-------------------+ |
+------->| Java Encryption |<---------+
| Server (NTRU) |
+-------------------+
| |
---------------------------------------/
(3. Direct P2P Data Transfer via WebRTC)

+----------------+ +----------------+
| User A Browser | | User B Browser |
| (index.html) | | (index.html) |
+-------+--------+ +--------+-------+
| |
| (1. Find each other) |
| +-------------------+ |
+------->| Node.js Signaling |<---------+
| Server (Matchmaker)|
+-------------------+
| |
| (2. Encrypt/Decrypt) | (4. Decrypt)
| +-------------------+ |
+------->| Java Encryption |<---------+
| Server (NTRU) |
+-------------------+
| |
---------------------------------------/
(3. Direct P2P Data Transfer via WebRTC)

## 🚀 Features

- **End-to-End Encryption:** All files are encrypted with the NTRU post-quantum algorithm before being sent.  
- **Peer-to-Peer Transfer:** Uses WebRTC to send files directly between users, with no server in the middle.  
- **Zero Server-Side Storage:** The signaling server only helps users find each other; no image data ever touches our servers.  
- **Full-Color Image Support:** Securely encrypts and decrypts the R, G, and B channels of any color image.  
- **Microservices Architecture:** A robust backend composed of a dedicated Java encryption service and a Node.js signaling service.  

---

## 🛠 Setup and Running the Project

To run this project, you will need to start the two backend servers and then open the front-end client in two separate browsers.

**Prerequisites**

- **Java Development Kit (JDK)** (Version 11 or higher)  
- **Apache Maven** (for building the Java project)  
- **Node.js and npm**

### 1. The Java Encryption Server

This server handles all cryptographic operations.

```bash
# Navigate to the EncryptionServer directory
cd EncryptionServer

# Compile the project and build the executable JAR
# (This may take a moment the first time as it downloads dependencies)
mvn package

# Run the server
java -jar target/ntru-encryption-server-1.0-SNAPSHOT-jar-with-dependencies.jar

The server will start and you will see the message:
✅ Encryption Server started on port 7070.

### 2. The Node.js Signaling Server

This server helps the two clients find each other.

```bash
# Navigate to the SignalingServer directory
cd SignalingServer

# Install the necessary libraries
npm install

# Run the server
node server.js

The server will start and you will see the message:
🚀 Signaling Server is running on http://localhost:8080.

### 3. The Front-End Client

This is the user interface. Before launching, you must configure it to connect to the computer running the servers.

1. **Find the Local IP Address** of the computer that is running the Java and Node.js servers (the "Host" machine).  
   - On Windows: `ipconfig`  
   - On macOS/Linux: `ifconfig`  
   The IP address will look like `192.168.1.5`.

2. **Update the `index.html` file**:
   - Open the `Q-Vault-Client/index.html` file in a text editor.
   - Find these two lines at the top of the `<script>` tag:

     ```javascript
     const JAVA_SERVER_URL = 'http://localhost:7070';
     const SIGNALING_SERVER_URL = 'http://localhost:8080';
     ```

   - Replace `localhost` with the IP address of your Host machine. For example:

     ```javascript
     const JAVA_SERVER_URL = 'http://192.168.1.5:7070';
     const SIGNALING_SERVER_URL = 'http://192.168.1.5:8080';
     ```

3. **Launch the Application**:  
   Open the modified `index.html` file in a web browser on two different devices connected to the same network.

## Operation Procedure
Subsequent to the successful initialization of both server-side processes, the client-side application may be instantiated within two distinct computer systems or, alternatively, within separate browser windows. A visual status indicator is provided to communicate the state of the peer-to-peer connection, which, upon successful establishment, will transition from a "Connecting..." state to one that reads "Connected!". The designated "Sender" may then proceed to select a color image file for transmission by utilizing the provided drag-and-drop functionality. Actuation of the "Encrypt & Send" button will initiate a sequence wherein the image is first transmitted to the Java server for the application of the NTRU encryption algorithm; the resultant encrypted data is subsequently relayed directly to the peer client by means of the established WebRTC data channel. The "Receiver" client is designed to display the transfer progress and, upon receipt of the complete encrypted payload, will automatically dispatch said data to the Java server for decryption. The operational sequence culminates in the rendering of the fully reconstructed, original color image upon the receiver's display.
