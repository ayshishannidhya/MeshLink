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
# MeshLink â€” Build Tasks

## Phase 1: Project Skeleton & Build System
- [/] Root build.gradle.kts, settings.gradle.kts, version catalog
- [ ] App module (MeshLinkApp, MainActivity, Navigation)
- [ ] All module build.gradle.kts files

## Phase 2: Core Common
- [ ] Extensions, constants, result types

## Phase 3: Core Crypto Engine
- [ ] X25519 key generation
- [ ] Ed25519 signing
- [ ] ChaCha20-Poly1305 AEAD
- [ ] HKDF key derivation
- [ ] Noise XX handshake
- [ ] Replay guard
- [ ] Identity manager

## Phase 4: Core Network Protocol
- [ ] Packet format with priority, fragments, route ID
- [ ] PacketCodec encode/decode
- [ ] Fragment splitter/assembler
- [ ] Transport interface
- [ ] BLE transport
- [ ] Wi-Fi Direct transport
- [ ] LAN transport
- [ ] Transport manager (no internet relay in V1)

## Phase 5: Core Mesh Engine
- [ ] Weighted router (8 metrics)
- [ ] Controlled flood with dedup
- [ ] Store-carry-forward (DTN)
- [ ] Neighbor table
- [ ] Leader election
- [ ] Reputation system
- [ ] Anti-spam / rate limiter
- [ ] Priority queue

## Phase 6: Core Database
- [ ] Room + SQLCipher setup
- [ ] Entities and DAOs
- [ ] Migrations

## Phase 7: Core Domain
- [ ] Use cases
- [ ] Domain models
- [ ] Repository interfaces

## Phase 8: Core UI Design System
- [ ] Theme, typography, colors
- [ ] Shared composables
- [ ] Dark/AMOLED modes

## Phase 9-13: Feature Modules
- [ ] Chat (1:1, groups, channels)
- [ ] Contacts (identity, QR, discovery)
- [ ] Settings (transport, battery, backup)
- [ ] Mesh Map (topology visualization)
- [ ] Dev Tools (packet log, routing table)

## Phase 14: Testing
- [ ] Crypto unit tests
- [ ] Packet codec tests
- [ ] Router tests

## Phase 15: Documentation
- [ ] README.md
- [ ] ARCHITECTURE.md
- [ ] PROTOCOL.md
- [ ] CI/CD workflow
