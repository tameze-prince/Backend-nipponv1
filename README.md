# 🌸 NipponHub - Solution E-commerce Asiatique

> Plateforme complète de commerce électronique pour vendre et acheter les meilleurs produits asiatiques en ligne.

[![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=java)](https://www.java.com)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-7.0.5-6DB33F?logo=spring-boot)](https://spring.io/projects/spring-boot)
[![Next.js](https://img.shields.io/badge/Next.js-16.2.4-000000?logo=next.js)](https://nextjs.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-336791?logo=postgresql)](https://www.postgresql.org)
[![License](https://img.shields.io/badge/License-Proprietary-red)](#)

---

## 📋 Table des Matières

- [À Propos](#-à-propos)
- [Démarrage Rapide](#-démarrage-rapide)
- [Structure du Projet](#-structure-du-projet)
- [Documentation](#-documentation)
- [Déploiement](#-déploiement)
- [Support](#-support)

---

## 🎯 À Propos

**NipponHub** est une plateforme e-commerce professionnelle permettant:

### Pour les Vendeurs 🏪
- Gérer les produits avec variantes multiples
- Créer des ventes flash avec réductions
- Recevoir des notifications en temps réel
- Analyser les performances de vente
- Accéder à un tableau de bord personnalisé

### Pour l'Admin 👑
- Gérer tous les utilisateurs et produits
- Voir toutes les commandes en temps réel  
- Modérer les contenus
- Analyser les statistiques globales
- Recevoir les notifications système

### Pour les Clients 🛍️
- Parcourir une large sélection de produits
- Créer un panier avec variantes
- Passer commande facilement
- **3 options de paiement**: Mobile Money, Retrieve, COD

---

## 🚀 Démarrage Rapide

### Configuration Minimale

- **Java 21** (Backend)
- **Node.js 20+** (Frontend)
- **PostgreSQL 16** (Database)
- **Docker & Docker Compose** (Optionnel, Recommandé)

### Démarrer en 2 Minutes

```bash
# 1. Cloner le repository
git clone <your-repo-url>
cd nipponhub

# 2. Préparer environment
cp .env.example .env

# 3. Démarrer avec Docker Compose (Tout-en-un)
docker-compose up -d

# 4. Accéder à l'application
# Frontend:  http://localhost:3000
# Backend:   http://localhost:8080
# Database:  localhost:5432
```

### Ou: Démarrage Manuel

```bash
# Terminal 1: Backend
cd nipponhubv1
mvn spring-boot:run

# Terminal 2: Frontend
cd nipponhub-frontend
npm install
npm run dev

# Terminal 3: Database (si pas de Docker)
docker run -e POSTGRES_PASSWORD=password postgres:16
```

---

## 📁 Structure du Projet

```
nipponhub/
├── 📄 Documentation compl COMPLETE_DOCUMENTATION.md  ⭐ À LIRE EN PREMIER
├── 📄 DEPLOYMENT_GUIDE.md                           ⭐ Guide déploiement
├── 📄 ENVIRONMENT_SETUP_GUIDE.md                    Configuration environment
├── 📄 README_DELIVERABLES.md                        Index des livrables
├── 📄 .env.example                                  Template variables d'environnement
├── 📄 docker-compose.yml                            Orchestration conteneurs
│
├── 📁 nipponhubv1/                                  Backend (Spring Boot)
│   ├── Dockerfile                                   Build image Docker
│   ├── pom.xml                                      Dépendances Maven
│   ├── src/main/java/                              Code source
│   ├── src/main/resources/                         Configuration
│   └── mvnw                                         Maven wrapper
│
└── 📁 nipponhub-frontend/                           Frontend (Next.js)
    ├── Dockerfile                                   Build image Docker
    ├── package.json                                 Dépendances npm
    ├── app/                                        Pages Next.js
    ├── src/                                        Source code (components, services, stores)
    └── public/                                     Ressources statiques
```

---

## 📚 Documentation

### Pour les Propriétaires/Décideurs
👉 **Commencez par**: [COMPLETE_DOCUMENTATION.md](./COMPLETE_DOCUMENTATION.md)
- Vue d'ensemble complète
- Architecture du système
- Fonctionnalités principales
- Emplacements pour screenshots

### Pour les Développeurs/Tech Team
👉 **Commencez par**: [COMPLETE_DOCUMENTATION.md - Guide Développeur](./COMPLETE_DOCUMENTATION.md#-guide-développeur)
- Stack technologique
- Setup développement
- Structure répertoires
- API REST Endpoints

### Pour le Déploiement
👉 **Commencez par**: [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)
- Architecture recommandée (Render + Vercel)
- Setup étape-par-étape
- Plateformes alternatives (Railway, Fly.io)
- Troubleshooting
- Coûts estimés

### Pour la Configuration
👉 **Commencez par**: [ENVIRONMENT_SETUP_GUIDE.md](./ENVIRONMENT_SETUP_GUIDE.md)
- Variables d'environnement
- Setup Local
- Setup Production
- Secrets management

---

## 🌐 Déploiement

### Déploiement Gratuit (Recommandé pour Start)

**Architecture:**
```
Frontend (Next.js)  →  Vercel           (Gratuit + optimisé)
Backend (Spring)    →  Render.com       (Gratuit + PostgreSQL)
```

**Durée**: ~30 minutes
**Coût**: Complètement GRATUIT

👉 **Guide complet**: [DEPLOYMENT_GUIDE.md → Render.com](./DEPLOYMENT_GUIDE.md#1️⃣-option-a-rendercom--vercel-recommandée)

### Alternatives

| Platform | Frontend | Backend | Database | Cost | Setup |
|----------|----------|---------|----------|------|-------|
| **Render + Vercel** | Vercel | Render | Render | FREE | Easy |
| **Railway.app** | Railway | Railway | Railway | $5/mo | Easy |
| **Fly.io** | Fly | Fly | Fly | $0-30/mo | Medium |
| **AWS** | S3+Cloudfront | EC2 | RDS | $50-200/mo | Hard |

---

## 🔑 Fonctionnalités Clés

### Product Management
- ✅ Créer/Modifier/Supprimer produits
- ✅ **Produits avec variantes** (taille, couleur, etc.)
- ✅ Upload images via Cloudinary
- ✅ Stock management (futur)

### Sales & Promotions
- ✅ **Flash Sales** avec réduction temporaire
- ✅ Disable/Enable automatiquement
- ✅ Badge promotion visible aux clients

### Notifications
- ✅ **Real-time notifications** (cloche UI)
- ✅ Auto-refresh 30s (peut être WebSocket)
- ✅ Admin reçoit: Nouvelles commandes, nouveaux users
- ✅ Owners reçoivent: Quand leurs produits sont commandés
- ✅ Clients reçoivent: Statut commande

### Commandes & Paiement
- ✅ Panier complet
- ✅ **3 méthodes de paiement**:
  - Mobile Money (frais livraison: 1500 FCFA ou gratuit >50k)
  - Retrieve (Retrait client, GRATUIT)
  - COD (Paiement à la livraison, même frais que Mobile Money)
- ✅ Calcul frais livraison automatique

### Sécurité & Autorisations
- ✅ JWT Token Authentication
- ✅ Role-based access control (CUSTOMER, OWNER, ADMIN)
- ✅ Owners ne peuvent modifier que LEURS produits
- ✅ CORS configuration
- ✅ Spring Security framework

---

## 🛠️ Développement

### Commands Backend

```bash
cd nipponhubv1

# Build
mvn clean package

# Développement
mvn spring-boot:run

# Tests
mvn test

# API Documentation
curl http://localhost:8080/swagger-ui.html
```

### Commands Frontend

```bash
cd nipponhub-frontend

# Install dependencies
npm install

# Development server
npm run dev

# Build for production
npm run build

# Start production build
npm start

# Linting
npm run lint
```

### Docker Commands

```bash
# Démarrer tous les services
docker-compose up -d

# Afficher les services
docker-compose ps

# Voir les logs
docker-compose logs -f backend
docker-compose logs -f frontend

# Arrêter
docker-compose down

# Arrêter et supprimer volumes
docker-compose down -v
```

---

## 📊 Architecture

```
┌─────────────────────────────────────────┐
│     Frontend (Next.js + React)          │
│     http://localhost:3000               │
└────────────────────┬────────────────────┘
                     │ (REST API)
                     │
┌────────────────────▼────────────────────┐
│     Backend (Spring Boot 7.0.5)         │
│     http://localhost:8080               │
│     ├─ Controllers                      │
│     ├─ Services                         │
│     └─ Repositories                     │
└────────────────────┬────────────────────┘
                     │ (JDBC)
                     │
┌────────────────────▼────────────────────┐
│   Database (PostgreSQL 16)              │
│   localhost:5432/nipponhub              │
└─────────────────────────────────────────┘
```

---

## 🔐 Variables d'Environnement

### Essentielles

```bash
# Database
DB_URL=jdbc:postgresql://localhost:5432/nipponhub
DB_USERNAME=nipponhub
DB_PASSWORD=secure_password

# JWT
JWT_SECRET=your_secret_key
JWT_ACCESS_EXPIRATION=86400

# CORS
CORS_ORIGINS=http://localhost:3000
BASE_URL=http://localhost:8080

# Cloudinary (Image upload)
CLOUDINARY_CLOUD_NAME=your_cloud
CLOUDINARY_API_KEY=your_key
CLOUDINARY_API_SECRET=your_secret

# Frontend
NEXT_PUBLIC_API_URL=http://localhost:8080
```

👉 **Voir aussi**: [.env.example](./.env.example)

---

## 📞 Support

### Documentation
- 🔗 [COMPLETE_DOCUMENTATION.md](./COMPLETE_DOCUMENTATION.md)
- 🔗 [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)
- 🔗 [ENVIRONMENT_SETUP_GUIDE.md](./ENVIRONMENT_SETUP_GUIDE.md)
- 🔗 [README_DELIVERABLES.md](./README_DELIVERABLES.md)

### Ressources
- [Spring Boot Docs](https://docs.spring.io/spring-boot)
- [Next.js Docs](https://nextjs.org/docs)
- [PostgreSQL Docs](https://www.postgresql.org/docs)
- [Docker Docs](https://docs.docker.com)

### Troubleshooting

**Q: Mon Docker ne démarre pas?**
A: Voir [DEPLOYMENT_GUIDE.md → Troubleshooting](#)

**Q: Comment déployer en production?**
A: Voir [DEPLOYMENT_GUIDE.md → Render.com Setup](#)

**Q: Quelle est la configuration des variables d'environnement?**
A: Voir [ENVIRONMENT_SETUP_GUIDE.md](#)

---

## 🎯 Prochaines Étapes

### 1. Setup Local ✅
```bash
docker-compose up -d
# Visiter http://localhost:3000
```

### 2. Lire la Documentation
- [ ] [COMPLETE_DOCUMENTATION.md](./COMPLETE_DOCUMENTATION.md)
- [ ] [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)

### 3. Préparer Production (Optionnel)
- [ ] Créer accounts: Render, Vercel, Cloudinary
- [ ] Générer secrets: JWT, DB Password
- [ ] Configurer domaines
- [ ] Suivre [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md)

---

## 📈 Statistiques du Projet

| Aspect | Détail |
|--------|--------|
| **Backend** | Spring Boot 7.0.5 + Java 21 |
| **Frontend** | Next.js 16.2.4 + React 19 |
| **Database** | PostgreSQL 16 |
| **Lignes de Code** | ~17,000+ |
| **Fonctionnalités** | 15+ principales |
| **API Endpoints** | 30+ |
| **Components Frontend** | 20+ réutilisables |
| **Coverage Sécurité** | JWT + Spring Security + CORS |

---

## 📝 License

Proprietary - Tous droits réservés © 2026 NipponHub

---

## 👥 Équipe

Développé avec ❤️ pour les amateurs de produits asiatiques

---

## 🎉 Prêt à Démarrer?

1. **Setup Local**: 
   ```bash
   docker-compose up -d && open http://localhost:3000
   ```

2. **Lire la Docs**:
   - [COMPLETE_DOCUMENTATION.md](./COMPLETE_DOCUMENTATION.md) (10 min)
   - [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) (5 min)

3. **Déployer**:
   - Suivre [DEPLOYMENT_GUIDE.md](./DEPLOYMENT_GUIDE.md) (30 min)

---

**Questions?** Consultez la documentation complète! 📚

**Version**: 1.0  
**Last Updated**: 2026-04  
**Status**: ✅ Production Ready
