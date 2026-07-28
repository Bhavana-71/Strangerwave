# StrangerWave 🌐

A real-time random stranger chat and video calling application, built with Spring Boot, WebSockets, and WebRTC.

## Features

- **Real-time text chat** between randomly matched strangers
- **Video calling** using WebRTC peer-to-peer connections
- **Mode selection** — users choose Text Chat or Video Call before matching, ensuring compatible pairing
- **"Next Stranger"** — instantly disconnect and get matched with someone new
- **Session logging** — every chat session (start time, end time, whether video was used) is logged to a MySQL database via JDBC
- **Thread-safe matching engine** — handles multiple simultaneous users using `ConcurrentLinkedQueue` and `ConcurrentHashMap`

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java, Spring Boot 4.1.0 |
| Real-time communication | WebSocket (Spring `TextWebSocketHandler`) |
| Video/Audio calling | WebRTC (peer-to-peer, signaled through the Java backend) |
| Database | MySQL, raw JDBC (`DriverManager`, `PreparedStatement`) |
| Frontend | HTML, CSS, vanilla JavaScript |
| Concurrency | `ConcurrentLinkedQueue`, `ConcurrentHashMap`, thread-safe synchronized pairing logic |

## How It Works

1. A user connects via WebSocket and picks a mode: **Text Chat** or **Video Call**
2. The server places them in a mode-specific waiting queue
3. As soon as two users in the same mode are both waiting, they're paired together
4. Messages are relayed directly between the paired sessions only (never broadcast)
5. For video calls, the Java server acts purely as a **signaling relay** — passing WebRTC offer/answer/ICE candidate messages between the two browsers. The actual video/audio stream flows directly peer-to-peer, never through the server
6. Every session's start time, end time, and whether video was used gets logged to MySQL via JDBC

## Architecture