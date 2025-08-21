# NATS JetStream Docker Setup

Hướng dẫn setup NATS JetStream sử dụng Docker cho ứng dụng crochet-webapp.

## 📋 Yêu cầu

- Docker Desktop
- Docker Compose

## 🚀 Cách sử dụng

### 1. Khởi động NATS Server

```bash
# Chạy script Windows
start-nats-docker.bat

# Hoặc sử dụng Docker Compose trực tiếp
docker-compose -f docker-compose.nats.yml up -d
```

### 2. Kiểm tra NATS Server

```bash
# Test connection
test-nats-docker.bat

# Hoặc kiểm tra thủ công
curl http://localhost:8222/healthz
curl http://localhost:8222/jsz
```

### 3. Chạy Spring Boot Application

```bash
./gradlew.bat bootRun
```

### 4. Dừng NATS Server

```bash
# Dừng script Windows
stop-nats-docker.bat

# Hoặc sử dụng Docker Compose trực tiếp
docker-compose -f docker-compose.nats.yml down
```

## 🔧 Cấu hình

### Docker Compose Services

- **nats**: NATS server với JetStream
  - Port 4222: Client connections
  - Port 8222: HTTP monitoring
  - Port 6222: Clustering

- **nats-cli**: NATS CLI tool (optional)

### Spring Boot Configuration

```yaml
nats:
  servers: nats://localhost:4222
  connection-name: crochet-webapp
  jetstream:
    enabled: true
    stream-name: notifications
    consumer-name: notification-processor
```

## 🧪 Test NATS Connection

### Từ Spring Boot Application

1. Tạo một comment trong ứng dụng
2. Kiểm tra logs để xem notification events được publish
3. Kiểm tra database để xem notifications được tạo

### Sử dụng NATS CLI

```bash
# Kết nối vào container CLI
docker exec -it crochet-nats-cli sh

# Subscribe to notifications
nats sub "notifications.>" -s nats://nats:4222

# Publish test message
nats pub "notifications.test" "Hello NATS!" -s nats://nats:4222
```

## 📊 Monitoring

### Health Check
```
http://localhost:8222/healthz
```

### JetStream Info
```
http://localhost:8222/jsz
```

### Connections Info
```
http://localhost:8222/connz
```

### View Logs
```bash
docker-compose -f docker-compose.nats.yml logs -f nats
```

## 🔍 Troubleshooting

### NATS không khởi động được

```bash
# Kiểm tra logs
docker-compose -f docker-compose.nats.yml logs nats

# Restart container
docker-compose -f docker-compose.nats.yml restart nats
```

### Connection timeout

```bash
# Kiểm tra port
netstat -an | findstr 4222

# Kiểm tra Docker container
docker-compose -f docker-compose.nats.yml ps
```

### JetStream không hoạt động

```bash
# Kiểm tra JetStream status
curl http://localhost:8222/jsz

# Reset JetStream (sẽ mất dữ liệu)
docker-compose -f docker-compose.nats.yml down -v
docker-compose -f docker-compose.nats.yml up -d
```

## 📁 File Structure

```
crochetwebapp/
├── Dockerfile.nats              # NATS Docker image
├── docker-compose.nats.yml      # Docker Compose configuration
├── start-nats-docker.bat        # Start script
├── stop-nats-docker.bat         # Stop script
├── test-nats-docker.bat         # Test script
└── README-NATS-Docker.md        # This file
```

## 🎯 Next Steps

1. **Deploy**: Deploy NATS Docker container lên server production
2. **Monitoring**: Setup monitoring cho NATS server
3. **Backup**: Cấu hình backup cho JetStream data
4. **Clustering**: Setup NATS clustering cho high availability

## 📞 Support

Nếu gặp vấn đề, kiểm tra:
1. Docker logs: `docker-compose -f docker-compose.nats.yml logs`
2. Application logs: Kiểm tra Spring Boot logs
3. NATS monitoring: `http://localhost:8222`
