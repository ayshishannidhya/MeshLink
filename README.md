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

[![CI](https://github.com/your-org/meshlink/actions/workflows/ci.yml/badge.svg)](https://github.com/your-org/meshlink/actions)
[![API](https://img.shields.io/badge/API-29%2B-brightgreen.svg)](https://android-arsenal.com/api?level=29)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

MeshLink enables secure, decentralized peer-to-peer messaging **without internet, SIM cards, or cloud servers**. Every device automatically becomes a node in an intelligent mesh network, routing encrypted messages through nearby peers.

## âœ¨ Key Features

| Feature | Description |
|---------|-------------|
| ðŸ”— **Multi-Transport Mesh** | BLE + Wi-Fi Direct + LAN â€” automatic failover |
| ðŸ” **End-to-End Encryption** | Noise XX handshake, ChaCha20-Poly1305, X25519 |
| ðŸ“¦ **Store & Forward** | Delay-tolerant networking â€” messages hop through couriers |
| ðŸ§  **Intelligent Routing** | 8-metric weighted scoring (RSSI, battery, latency, reputation...) |
| ðŸš¨ **Emergency SOS** | Priority broadcasts that bypass all queues |
| ðŸ—ºï¸ **Mesh Map** | Real-time topology visualization |
| ðŸ›¡ï¸ **Anti-Spam** | Per-peer rate limiting + reputation system |
| ðŸ”‹ **Battery Optimized** | Adaptive duty cycling, low-power scanning modes |
| ðŸ•µï¸ **Privacy First** | No phone number, no email, no read receipts |

## ðŸ“ Architecture

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚              Jetpack Compose UI             â”‚
â”‚         Material 3 + Dynamic Color          â”‚
â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
â”‚               ViewModels (MVVM)             â”‚
â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
â”‚          Use Cases (Clean Architecture)     â”‚
â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
â”‚     Repositories + Room/SQLCipher           â”‚
â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
â”‚  Crypto  â”‚  Router  â”‚   Store & Forward     â”‚
â”‚  Engine  â”‚  Engine  â”‚       (DTN)           â”‚
â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
â”‚              Transport Manager              â”‚
â”œâ”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¤
â”‚  BLE   â”‚  Wi-Fi Direct â”‚       LAN         â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

## ðŸ› ï¸ Tech Stack

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

## ðŸ“‚ Project Structure

```
MeshLink/
â”œâ”€â”€ app/                    # Application entry, DI, navigation
â”œâ”€â”€ core/
â”‚   â”œâ”€â”€ common/             # Extensions, constants, result types
â”‚   â”œâ”€â”€ crypto/             # Noise XX, X25519, ChaCha20, HKDF
â”‚   â”œâ”€â”€ network/            # Packet protocol, transports (BLE/WiFi/LAN)
â”‚   â”œâ”€â”€ mesh/               # Routing, flooding, store-forward, anti-spam
â”‚   â”œâ”€â”€ database/           # Room entities, DAOs, SQLCipher
â”‚   â”œâ”€â”€ domain/             # Use cases, domain models
â”‚   â””â”€â”€ ui/                 # Theme, shared composables
â”œâ”€â”€ feature/
â”‚   â”œâ”€â”€ chat/               # 1:1 and group messaging
â”‚   â”œâ”€â”€ contacts/           # Identity management, QR pairing
â”‚   â”œâ”€â”€ settings/           # Transport config, backup, security
â”‚   â”œâ”€â”€ meshmap/            # Topology visualization
â”‚   â””â”€â”€ devtools/           # Packet logs, routing table, diagnostics
â”œâ”€â”€ docs/                   # Architecture docs, protocol spec
â””â”€â”€ .github/workflows/      # CI/CD
```

## ðŸ”’ Security Model

### Encryption Stack
```
Identity:     Ed25519 (signing) + X25519 (key exchange)
Handshake:    Noise XX â€” mutual auth + identity hiding
Session:      ChaCha20-Poly1305 AEAD with nonce counters
Key Derive:   HKDF-SHA256
Database:     SQLCipher (256-bit AES)
Replay:       2048-bit sliding window
```

### Privacy
- **No phone number** â€” identity is a public key pair
- **No read receipts** â€” only Queued / Relayed / Delivered
- **No typing indicators**
- **Ephemeral IDs rotate** every hour
- **TTL excluded from signatures** â€” relay nodes can't trace origin

## ðŸ“¡ Wire Protocol

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚ Ver(1) â”‚ Type(1) â”‚ Priority(1) â”‚ TTL(1)              â”‚
â”‚ Flags(1) â”‚ FragIdx(1) â”‚ TotalFrag(1) â”‚ Pad(1)        â”‚
â”‚ PacketID (8)                                          â”‚
â”‚ SenderID (8)                                          â”‚
â”‚ RecipientID (8) â€” 0x00 for broadcast                  â”‚
â”‚ Timestamp (8)                                         â”‚
â”‚ PayloadLen (2) â”‚ Payload (variable)                   â”‚
â”‚ Ed25519 Signature (64)                                â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

**Priority Classes:**
| Priority | Class | Behavior |
|----------|-------|----------|
| 0 | Emergency (SOS) | Bypasses all queues and jitter |
| 1 | Coordinator | Reduced jitter |
| 2 | Messages | Normal routing |
| 3 | Media | Lowest priority, highest jitter |

## ðŸš€ Getting Started

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Android device with BLE (API 29+)

### Build
```bash
git clone https://github.com/your-org/meshlink.git
cd meshlink
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

## ðŸ—ºï¸ Roadmap

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

## ðŸ¤ Contributing

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

Please follow the existing code style and add tests for new features.

## ðŸ“„ License

This project is licensed under the MIT License â€” see [LICENSE](LICENSE) for details.

## ðŸ™ Acknowledgments

- [Noise Protocol Framework](https://noiseprotocol.org/) â€” handshake design
- [RFC 6479](https://tools.ietf.org/html/rfc6479) â€” replay protection
- The mesh networking research community
