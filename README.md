# 🏭 Système FESTO MAS

**Système de contrôle industriel intelligent - Architecture Multi-Agents**

## 📋 Présentation

Système de simulation d'une ligne de production industrielle qui utilise une architecture multi-agents (JADE) pour gérer automatiquement les pannes et optimiser la production.

## 🎯 Caractéristiques

| Fonctionnalité | Description |
|----------------|-------------|
| **Deux architectures** | Centralisée vs Composite |
| **Gestion auto des pannes** | Détection et reconfiguration automatique |
| **Équilibrage de charge** | Redistribution intelligente du travail |
| **Monitoring web** | Interface JADE intégrée (port 1099) |
| **Communication temps réel** | Messages ACL entre agents |

## 🏗️ Architecture du Système

### Comparaison des Architectures

**Architecture Centralisée**
- Contrôle hiérarchique unique
- Décision centralisée
- Supervision globale

**Architecture Composite**
- Contrôle distribué
- Modules spécialisés
- Meilleure résilience

### Composants Principaux

**Agents de Contrôle**
- `RLRAControllerAgent` (Centralisé)
- `RLRACompositeAgent` (Composite)

**Agents de Surveillance**
- `SiteMonitorAgent` - Surveille un site de production
- `MachineAgent` - Simule une machine physique
- `TransportCoordinatorAgent` - Gère le flux de transport

**Modules Composite**
- `MonitorModule` - Reçoit les alertes
- `LearnerModule` - Crée les plans
- `ExecutorModule` - Exécute les actions
