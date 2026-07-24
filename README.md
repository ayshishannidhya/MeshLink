<!--
  =============================================================================
  MeshLink
  Secure Offline Mesh Communication Platform

  Copyright (c) 2026 Ayshi Shannidhya Panda.
  All Rights Reserved.

  MeshLink, the MeshLink Protocol, associated software, source code,
  documentation, algorithms, and design architecture are proprietary
  intellectual property of Ayshi Shannidhya Panda.

  Unauthorized reproduction, modification, distribution, or commercial
  exploitation of any part of this software or protocol is prohibited
  without prior written permission.

  Author  : Ayshi Shannidhya Panda
  =============================================================================
-->

# MeshLink

**Next-Generation Offline Mesh Communication Platform**

[![CI](https://github.com/ayshishannidhya/MeshLink/actions/workflows/ci.yml/badge.svg)](https://github.com/ayshishannidhya/MeshLink/actions)
[![API](https://img.shields.io/badge/API-29%2B-brightgreen.svg)](https://android-arsenal.com/api?level=29)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE)

MeshLink enables secure, decentralized peer-to-peer messaging **without internet, SIM cards, or cloud servers**. Every device automatically becomes a node in an intelligent mesh network, routing encrypted messages through nearby peers.

---

## Key Features

| Feature | Description |
|---------|-------------|
| **Multi-Transport Mesh** | BLE + Wi-Fi Direct + LAN with automatic failover |
| **End-to-End Encryption** | Noise XX handshake, ChaCha20-Poly1305, X25519 |
| **Store & Forward** | Delay-tolerant networking - messages hop through couriers |
| **Intelligent Routing** | 8-metric weighted scoring (RSSI, battery, latency, reputation) |
| **Emergency SOS** | Priority broadcasts that bypass all queues |
| **Mesh Map** | Real-time topology visualization |
| **Anti-Spam** | Per-peer rate limiting + reputation system |
| **Battery Optimized** | Adaptive duty cycling, low-power scanning modes |
| **Privacy First** | No phone number, no email, no read receipts |

---

## Architecture

```
+---------------------------------------------+
|              Jetpack Compose UI              |
|         Material 3 + Dynamic Color           |
+---------------------------------------------+
|               ViewModels (MVVM)              |
+---------------------------------------------+
|          Use Cases (Clean Architecture)      |
+---------------------------------------------+
|     Repositories + Room/SQLCipher            |
+----------+----------+-----------------------+
|  Crypto   |  Router  |   Store & Forward     |
|  Engine   |  Engine  |       (DTN)           |
+----------+----------+-----------------------+
|              Transport Manager               |
+--------+--------------+---------------------+
|  BLE   |  Wi-Fi Direct |       LAN           |
+--------+--------------+---------------------+
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose, Material 3 |
| DI | Hilt |
| Database | Room + SQLCipher |
| Async | Coroutines + Flow |
| Crypto | JCA (X25519, Ed25519), ChaCha20-Poly1305 |
| Transport | BLE GATT, Wi-Fi P2P, NSD/mDNS |
| Testing | JUnit, Google Truth, MockK |
| CI/CD | GitHub Actions |

---

## Project Structure

```
MeshLink/
|-- app/                    # Application entry, DI, navigation
|-- core/
|   |-- common/             # Extensions, constants, result types
|   |-- crypto/             # Noise XX, X25519, ChaCha20, HKDF
|   |-- network/            # Packet protocol, transports (BLE/WiFi/LAN)
|   |-- mesh/               # Routing, flooding, store-forward, anti-spam
|   |-- database/           # Room entities, DAOs, SQLCipher
|   |-- domain/             # Use cases, domain models
|   +-- ui/                 # Theme, shared composables
|-- feature/
|   |-- chat/               # 1:1 and group messaging
|   |-- contacts/           # Identity management, QR pairing
|   |-- settings/           # Transport config, backup, security
|   |-- meshmap/            # Topology visualization
|   +-- devtools/           # Packet logs, routing table, diagnostics
|-- docs/                   # Architecture docs, protocol spec
+-- .github/workflows/      # CI/CD
```

---

## Security Model

### Encryption Stack

```
Identity:     Ed25519 (signing) + X25519 (key exchange)
Handshake:    Noise XX - mutual auth + identity hiding
Session:      ChaCha20-Poly1305 AEAD with nonce counters
Key Derive:   HKDF-SHA256
Database:     SQLCipher (256-bit AES)
Replay:       2048-bit sliding window
```

### Privacy

- **No phone number** - identity is a public key pair
- **No read receipts** - only Queued / Relayed / Delivered
- **No typing indicators**
- **Ephemeral IDs rotate** every hour
- **TTL excluded from signatures** - relay nodes can't trace origin

---

## Wire Protocol

```
+------------------------------------------------------+
| Ver(1) | Type(1) | Priority(1) | TTL(1)              |
| Flags(1) | FragIdx(1) | TotalFrag(1) | Pad(1)        |
| PacketID (8)                                          |
| SenderID (8)                                          |
| RecipientID (8) - 0x00 for broadcast                  |
| Timestamp (8)                                         |
| PayloadLen (2) | Payload (variable)                   |
| Ed25519 Signature (64)                                |
+------------------------------------------------------+
```

**Priority Classes:**

| Priority | Class | Behavior |
|----------|-------|----------|
| 0 | Emergency (SOS) | Bypasses all queues and jitter |
| 1 | Coordinator | Reduced jitter |
| 2 | Messages | Normal routing |
| 3 | Media | Lowest priority, highest jitter |

---

## Getting Started

### Prerequisites

- Android Studio Ladybug or newer
- JDK 17
- Android device with BLE (API 29+)

### Build

```bash
git clone https://github.com/ayshishannidhya/MeshLink.git
cd MeshLink
./gradlew assembleDebug
```

### Run Tests

```bash
./gradlew test          # Unit tests
./gradlew lint          # Lint checks
```

### Install

```bash
./gradlew installDebug
```

---

## Roadmap

- [x] BLE mesh transport
- [x] Wi-Fi Direct transport
- [x] LAN/mDNS transport
- [x] Noise XX encrypted sessions
- [x] Store-and-forward (DTN)
- [x] 8-metric weighted routing
- [x] Anti-spam rate limiting
- [x] Reputation system
- [x] Mesh topology map
- [x] Developer console
- [ ] Voice notes (compressed, encrypted)
- [ ] Chunked file transfer with resume
- [ ] On-device AI (translation, summarization)
- [ ] Leader election for large meshes
- [ ] LoRa transport plugin
- [ ] iOS companion app

---

## Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please follow the existing code style and add tests for new features.

---

## License

Copyright (c) 2026 Ayshi Shannidhya Panda. All Rights Reserved.

This software is proprietary. See the copyright notice in each source file.

---

## Acknowledgments

- [Noise Protocol Framework](https://noiseprotocol.org/) - handshake design
- [RFC 6479](https://tools.ietf.org/html/rfc6479) - replay protection
- The mesh networking research community
