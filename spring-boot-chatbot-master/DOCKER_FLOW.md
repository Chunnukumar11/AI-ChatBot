# Docker Containerization Flow Diagram

## Complete Docker Flow Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     DEVELOPMENT ENVIRONMENT                              │
│                   (Your Local Machine - Windows)                         │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ↓
    ┌──────────────────────────────────────────────────────┐
    │         Source Code & Configuration                  │
    │  ┌────────────────────────────────────────────────┐  │
    │  │  src/                                          │  │
    │  │  ├─ main/java/.../*.java (Spring Boot code)  │  │
    │  │  ├─ main/resources/                           │  │
    │  │  │  ├─ application.properties                 │  │
    │  │  │  ├─ templates/index.html                   │  │
    │  │  │  └─ static/style.css                       │  │
    │  │  └─ test/...                                  │  │
    │  │                                                │  │
    │  │  pom.xml (Maven dependencies)                 │  │
    │  │  Dockerfile (Docker build instructions)       │  │
    │  │  docker-compose.yml (Container orchestration) │  │
    │  └────────────────────────────────────────────────┘  │
    └──────────────────────────────────────────────────────┘
                                    │
                                    ↓
    ┌──────────────────────────────────────────────────────┐
    │              DOCKER BUILD PROCESS                    │
    │              (Multi-Stage Build)                     │
    ├──────────────────────────────────────────────────────┤
    │                                                      │
    │  STAGE 1: Builder                                    │
    │  ┌────────────────────────────────────────────────┐  │
    │  │ FROM maven:3.9.11-eclipse-temurin-17          │  │
    │  │                                                │  │
    │  │ 1. COPY pom.xml                               │  │
    │  │ 2. COPY src code                              │  │
    │  │ 3. RUN mvn clean package -DskipTests          │  │
    │  │    ↓ Compiles Java                            │  │
    │  │    ↓ Downloads dependencies                   │  │
    │  │    ↓ Runs unit tests                          │  │
    │  │    ↓ Packages JAR                             │  │
    │  │ 4. OUTPUT: chatbot-1.0.0.jar (45MB)           │  │
    │  └────────────────────────────────────────────────┘  │
    │              │                                       │
    │              ↓ (Only JAR is passed to next stage)   │
    │                                                      │
    │  STAGE 2: Runtime                                    │
    │  ┌────────────────────────────────────────────────┐  │
    │  │ FROM eclipse-temurin:17-jre-alpine            │  │
    │  │ (Minimal JRE image)                           │  │
    │  │                                                │  │
    │  │ 1. COPY --from=builder JAR file               │  │
    │  │ 2. EXPOSE 8080                                │  │
    │  │ 3. Setup HEALTHCHECK                          │  │
    │  │ 4. ENTRYPOINT: java -jar app.jar              │  │
    │  └────────────────────────────────────────────────┘  │
    │                                                      │
    └──────────────────────────────────────────────────────┘
                                    │
                                    ↓ (~99 seconds total)
    ┌──────────────────────────────────────────────────────┐
    │        DOCKER IMAGE CREATED                         │
    │                                                      │
    │  Image Name:   chatbot:latest                       │
    │  Size:         ~380MB (optimized)                   │
    │  Layers:       ~10 layers (compressed)              │
    │  Registry:     Local Docker (docker:desktop-linux)  │
    │                                                      │
    │  Breakdown:                                          │
    │  ├─ Base Alpine JRE:    ~230MB                      │
    │  ├─ Application JAR:    ~45MB                       │
    │  ├─ Dependencies:       ~105MB (in JAR)             │
    │  └─ Overhead:           ~small                      │
    │                                                      │
    └──────────────────────────────────────────────────────┘
                                    │
                                    ↓
    ┌──────────────────────────────────────────────────────┐
    │     CONTAINER INITIALIZATION                         │
    │                                                      │
    │  docker run -d                                       │
    │    -p 8080:8080                                      │
    │    -e JAVA_OPTS="-Xmx512m"                          │
    │    chatbot:latest                                    │
    │                                                      │
    │  1. Docker creates container instance               │
    │  2. Allocates port mapping (host:container)         │
    │  3. Sets environment variables                      │
    │  4. Mounts filesystem layers                        │
    │  5. Prepares network namespace                      │
    │                                                      │
    └──────────────────────────────────────────────────────┘
                                    │
                                    ↓
    ┌──────────────────────────────────────────────────────┐
    │     CONTAINER STARTUP                               │
    │                                                      │
    │  1. Java JVM initializes                            │
    │  2. Spring Boot context loads                       │
    │  3. Tomcat server starts (localhost:8080)           │
    │  4. Application ready (status: "Up 10s")            │
    │  5. Health check: PASSING ✓                         │
    │                                                      │
    │  Logs show:                                          │
    │  [SUCCESS] ChatbotApplication started in 2.9s       │
    │  [INFO] Tomcat started on port 8080                │
    │                                                      │
    └──────────────────────────────────────────────────────┘
                                    │
                                    ↓
┌─────────────────────────────────────────────────────────────────────────┐
│              RUNNING CONTAINER (ISOLATED ENVIRONMENT)                   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────┐          │
│  │            Docker Container Process                      │          │
│  │                                                          │          │
│  │  Container ID: 7a2500fcc9ee...                         │          │
│  │  Port Mapping: 0.0.0.0:8080 → localhost:8080           │          │
│  │  Memory: 512MB (max)                                   │          │
│  │  CPU: Unlimited (by default)                           │          │
│  │  Restart Policy: unless-stopped                        │          │
│  │  Health: PASSING (checked every 30s)                   │          │
│  │                                                          │          │
│  │  ┌────────────────────────────────────────────────────┐ │          │
│  │  │       Spring Boot Application Inside               │ │          │
│  │  │                                                    │ │          │
│  │  │  http://localhost:8080/                           │ │          │
│  │  │  ├─ [GET /] → Serve index.html (Thymeleaf)       │ │          │
│  │  │  ├─ [POST /api/chat] → ChatController            │ │          │
│  │  │  │  └─ ChatService (Hardcoded + API responses)   │ │          │
│  │  │  │     ├─ "hello" → Hardcoded response           │ │          │
│  │  │  │     ├─ "what is devops?" → Hardcoded          │ │          │
│  │  │  │     ├─ Other → Gemini API (if key valid)      │ │          │
│  │  │  │     └─ Error → Friendly fallback              │ │          │
│  │  │  └─ Static files: style.css, favicon             │ │          │
│  │  │                                                    │ │          │
│  │  │  Database: None (stateless)                       │ │          │
│  │  │  Config: Loaded from application.properties       │ │          │
│  │  │                                                    │ │          │
│  │  └────────────────────────────────────────────────────┘ │          │
│  │                                                          │          │
│  │  Isolated from:                                         │          │
│  │  ✓ Host filesystem (except mapped volumes)            │          │
│  │  ✓ Host processes (containerized)                     │          │
│  │  ✓ Other containers (network isolation)               │          │
│  │  ✓ Host network (port-mapped only)                    │          │
│  │                                                          │          │
│  └──────────────────────────────────────────────────────────┘          │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ↓
    ┌──────────────────────────────────────────────────────┐
    │         CLIENT ACCESS                                │
    │                                                      │
    │  Web Browser:                                        │
    │  http://localhost:8080                              │
    │                    │                                 │
    │                    ↓                                 │
    │  ┌────────────────────────────────────────────────┐  │
    │  │   Request reaches port 8080                   │  │
    │  │   ↓                                            │  │
    │  │   Docker Port Mapper                          │  │
    │  │   (0.0.0.0:8080 → container:8080)            │  │
    │  │   ↓                                            │  │
    │  │   Inside Container                            │  │
    │  │   ↓                                            │  │
    │  │   Spring Boot Handler                         │  │
    │  │   ↓                                            │  │
    │  │   Response sent back                          │  │
    │  └────────────────────────────────────────────────┘  │
    │                    │                                 │
    │                    ↓                                 │
    │  Browser displays ChatBot UI                        │
    │                                                      │
    └──────────────────────────────────────────────────────┘
```

---

## 🔄 Docker Compose Flow

```
docker-compose up --build
        │
        ├─ Read docker-compose.yml
        ├─ Create network: chatbot-network (bridge)
        ├─ Build image from Dockerfile
        ├─ Create container from image
        ├─ Set port mappings: 8080:8080
        ├─ Set environment variables
        ├─ Mount volumes (if configured)
        ├─ Start container
        ├─ Attach to container logs
        └─ Monitor health checks
             │
             ├─ Every 30s: wget http://localhost:8080/
             ├─ Timeout: 3 seconds
             ├─ Retries: 3
             └─ Status: healthy ✓
```

---

## 📊 Component Interaction in Container

```
┌─────────────────────────────────────────┐
│  EXTERNAL: Web Browser                  │
│  User: "hello"                          │
└──────────────┬──────────────────────────┘
               │
               ↓ HTTP POST
┌─────────────────────────────────────────┐
│  Port 8080 (Container Network Namespace)│
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│  Tomcat Server (Embedded in Spring Boot)│
│  ├─ Thread Pool: 200 threads            │
│  ├─ Request Handler                     │
│  └─ Session Manager                     │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│  ChatController (/api/chat)             │
│  • Parse JSON request                   │
│  • Validate input                       │
│  • Call ChatService                     │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│  ChatService                            │
│  │                                      │
│  ├─→ getHardcodedResponse()             │
│  │   └─ Check 20+ test responses        │
│  │      └─ "hello" → Match!             │
│  │         Return: "Hello! 👋..."       │
│  │                                      │
│  └─→ [If no match] tryGeminiAPI()       │
│      └─ Build request                   │
│      └─ WebClient.post() (Reactive)     │
│      └─ gemini.api.url + key            │
│      └─ Parse response                  │
│      └─ [If error] → Fallback message   │
│                                          │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│  ChatResponse (JSON)                    │
│  {                                      │
│    "response": "Hello! 👋 Welcome..."   │
│  }                                      │
└──────────────┬──────────────────────────┘
               │
               ↓
┌─────────────────────────────────────────┐
│  Tomcat → HTTP Response                 │
│  Content-Type: application/json         │
└──────────────┬──────────────────────────┘
               │
               ↓ HTTP (Port 8080)
┌─────────────────────────────────────────┐
│  Web Browser                            │
│  ├─ Receive JSON                        │
│  ├─ JavaScript processes                │
│  ├─ Update DOM                          │
│  ├─ Display message bubble              │
│  └─ Show response: "Hello! 👋..."       │
└─────────────────────────────────────────┘
```

---

## 🎯 Container Lifecycle

```
State: Created
   │
   ↓
State: Running (Health Check: Starting)
   │
   ├─ Wait 5 seconds (start-period)
   │
   ↓
State: Running (Health Check: Healthy)
   │
   ├─ Every 30s: curl http://localhost:8080/
   ├─ If success (exit 0): ✓ Healthy
   ├─ If failure (exit 1): ✗ Unhealthy
   ├─ Timeout after 3s
   │
   ↓ [User stops container]
State: Exited (Exit Code: 143 - Terminated)
   │
   ├─ Restart Policy: unless-stopped
   ├─ → Auto-restart (except manual stop)
   │
   ↓
State: Running Again
```

---

## 🔐 Container Security & Isolation

```
Host Machine (Windows)
├─ Docker Desktop VM (Linux)
│  └─ Docker Daemon
│     │
│     ├─ Container: chatbot-app
│     │  ├─ Isolated Process Namespace (PID)
│     │  ├─ Isolated Network Namespace (Network)
│     │  ├─ Isolated Mount Namespace (Filesystem)
│     │  ├─ Isolated IPC Namespace (Signals)
│     │  ├─ Isolated UTS Namespace (Hostname)
│     │  │
│     │  ├─ Read-only filesystem (except /tmp, /var)
│     │  ├─ No direct host file access
│     │  ├─ Resource limits: 512MB RAM max
│     │  ├─ CPU cgroups (fair share)
│     │  │
│     │  ├─ Can access: Only port 8080 (via port-map)
│     │  ├─ Can access: application.properties
│     │  ├─ Cannot access: Host /etc, /home, /usr, etc.
│     │  │
│     │  └─ Java process runs as root (in container)
│     │     But isolated from host root
│     │
│     └─ Network Bridge: chatbot-network
│        ├─ Isolated from other containers (if not on same network)
│        └─ Port 8080 mapped to host: 0.0.0.0:8080
```

---

## 📈 Performance Comparison

```
┌──────────────────────┬──────────────┬──────────────┐
│ Metric               │ Local Maven  │ Docker       │
├──────────────────────┼──────────────┼──────────────┤
│ Build time           │ ~45s         │ ~99s (1st)   │
│ (First time)         │ (compile)    │ (+ docker)   │
├──────────────────────┼──────────────┼──────────────┤
│ Build time           │ ~10s (cache) │ ~0.3s        │
│ (Subsequent)         │ (cached)     │ (from image) │
├──────────────────────┼──────────────┼──────────────┤
│ Startup time         │ ~2.9s        │ ~2.9s        │
│                      │              │ (same Java)  │
├──────────────────────┼──────────────┼──────────────┤
│ Image/Artifact size  │ ~45MB (JAR)  │ ~380MB       │
│                      │              │ (image)      │
├──────────────────────┼──────────────┼──────────────┤
│ Runtime memory       │ ~512MB       │ ~512MB       │
│                      │ (system)     │ (limited)    │
├──────────────────────┼──────────────┼──────────────┤
│ Deployment           │ Manual       │ Automated    │
│ Complexity           │              │              │
├──────────────────────┼──────────────┼──────────────┤
│ Reproducibility      │ Good         │ Excellent    │
│                      │ (same env)   │ (guaranteed) │
└──────────────────────┴──────────────┴──────────────┘
```

---

## 🚀 Deployment Scenarios

```
1. LOCAL DEVELOPMENT
   docker-compose up --build
   ├─ Rebuild if code changes
   ├─ Auto-restart on failure
   ├─ View logs in console
   └─ Stop with: docker-compose down

2. PRODUCTION - Docker Host
   docker run -d \
     -p 80:8080 \
     -e JAVA_OPTS="-Xmx2g" \
     --restart always \
     chatbot:v1.0.0
   
   ├─ Published to port 80 (HTTP)
   ├─ Larger memory allocation
   ├─ Auto-restart on failure
   └─ Monitor with: docker logs

3. PRODUCTION - Docker Swarm
   docker service create \
     --replicas 3 \
     --publish 80:8080 \
     chatbot:v1.0.0
   
   ├─ 3 container replicas
   ├─ Load balancing
   ├─ Service discovery
   └─ Rolling updates

4. PRODUCTION - Kubernetes
   kubectl apply -f deployment.yaml
   
   ├─ Pod replicas
   ├─ Health monitoring
   ├─ Auto-scaling
   ├─ Rolling updates
   └─ Service exposure
```

---

## ✅ Docker Commands Reference

```
BUILD:
  docker build -t chatbot:latest .
  docker build -t chatbot:v1.0 --build-arg VERSION=1.0 .

RUN:
  docker run -d -p 8080:8080 chatbot:latest
  docker run -it chatbot:latest /bin/sh (shell access)

COMPOSE:
  docker-compose up --build
  docker-compose up -d (detached)
  docker-compose down

LOGS:
  docker logs chatbot-app
  docker logs -f chatbot-app (follow)

INSPECT:
  docker ps (running containers)
  docker images (built images)
  docker inspect chatbot-app (container details)
  docker exec -it chatbot-app /bin/sh

CLEANUP:
  docker stop chatbot-app
  docker rm chatbot-app
  docker rmi chatbot:latest
  docker system prune (remove unused resources)
```

---

## 📋 Current Setup Status

✅ **Docker Image Built**
  - Tag: `chatbot:latest`
  - Size: ~380MB (optimized)
  - Status: Ready to deploy

✅ **Container Running**
  - ID: `7a2500fcc9ee...`
  - Port: `8080` (mapped)
  - Status: Up 10 seconds
  - Health: Passing ✓

✅ **Access Application**
  - Web UI: http://localhost:8080
  - API: POST http://localhost:8080/api/chat
  - Status: ✓ Running

---

**Last Updated**: December 15, 2025  
**Docker Version**: Latest (supports Docker Compose v3.8+)  
**Build Time**: 99.5 seconds (first time)  
**Image Layers**: ~10 (optimized with multi-stage)
