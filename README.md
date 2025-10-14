# Chat Application - Backend

A RESTful API backend for a real-time chat application built with Spring Boot. Features JWT authentication, WebSocket messaging, MySQL database integration, and comprehensive security configurations.

## 🚀 Technologies Used

- **Spring Boot 3.3.1** - Application framework
- **Spring Security** - Authentication and authorization
- **Spring WebSocket** - Real-time messaging
- **Spring Data JPA** - Database abstraction
- **MySQL 8.0** - Primary database
- **JWT (JJWT)** - Token-based authentication
- **Flyway** - Database migrations
- **Maven** - Dependency management
- **H2** - In-memory testing database

## 📋 Features

- JWT-based authentication with refresh tokens
- Real-time messaging via WebSocket/STOMP
- User registration and login
- Secure password hashing with BCrypt
- Database migrations with Flyway
- Comprehensive request logging
- CORS configuration
- Unit and integration testing
- Docker containerization

## 🏗️ Project Structure

```
src/main/java/com/chatapp/chatapp/
├── auth/
│   ├── JwtAuthenticationFilter.java         # JWT filter for requests
│   ├── WebSocketAuthInterceptor.java        # Websocket authentication
│   └── WebSocketHandshakeInterceptor.java   # Websocket handshake
├── config/
│   ├── Config.java               # General configuration & beans
│   ├── CorsConfig.java           # CORS configuration
│   ├── SecurityConfig.java       # Security configuration
│   └── WebSocketConfig.java      # WebSocket configuration
├── controller/
│   ├── AuthController.java       # Authentication endpoints
│   ├── MessageController.java    # Message endpoints & WebSocket
│   └── UserController.java       # User management endpoints
├── DTO/
│   ├── AuthRequest.java          # Login request DTO
│   ├── AuthResponse.java         # Login response DTO
│   ├── RegisterRequest.java      # Registration request DTO
│   ├── MessageRequest.java       # Message request DTO
│   ├── TokenDTO.java            # Token transfer DTO
│   └── JwtValidationResult.java  # JWT validation result DTO
├── entity/
│   ├── User.java                # User entity
│   ├── Message.java             # Message entity
│   └── Token.java               # Token entity
├── repository/
│   ├── UserRepository.java     # User data access
│   ├── MessageRepository.java  # Message data access
│   └── TokenRepository.java     # Token data access
├── service/
│   └── LogoutService.java        # Logout handling
│   ├── AuthService.java         # Authentication services
│   ├── JwtService.java          # JWT token services
│   ├── MessageService.java      # Message services
│   └── UserService.java         # User services
├── util/
│   └── ApplicationLogger.java   # Centralized logging utility
└── ChatappApplication.java      # Main application class

src/main/resources/
├── application.properties        # Main configuration
├── application-docker.properties # Docker environment config
├── application-dev.properties    # Local  environment config
├── db/migration/                 # Flyway database migrations
│   ├── V1__Create_user_table.sql
│   ├── V2__Create_message_table.sql
│   └── V3__Create_token_table.sql

src/test/java/com/chatapp/chatapp/
├── AuthControllerTest.java       # Authentication endpoint tests
├── AuthServiceTest.java          # Authentication service tests
├── JwtServiceTest.java           # JWT service tests
└── test_util/
    └── MockJwtService.java       # JWT mocking utility

Configuration Files:
├── pom.xml                       # Maven dependencies & build
├── compose.yaml                  # Production Docker Compose
├── docker-compose.dev.yml        # Development Docker Compose
├── Dockerfile                    # Production Docker image
├── Dockerfile.dev                # Development Docker image
├── .env.example                  # Environment variables template
└── application-dev.properties.example # Dev config template
```

## 🛠️ Installation & Setup

### Prerequisites
- Java 22
- Maven 3.6+
- MySQL 8.0+
- Docker & Docker Compose (for containerized setup)

### Development Setup

1. **Clone the repository structure**
   ```bash
   # Create parent directory
   mkdir chat-application
   cd chat-application
   
   # Clone both repositories
   git clone <backend-repository-url> chatapp-spring-backend
   git clone <frontend-repository-url> chatapp-react-frontend
   ```

2. **Database Setup (Local Development)**
   ```sql
   CREATE DATABASE chatapp;
   CREATE USER 'chatapp_user'@'localhost' IDENTIFIED BY 'your_password';
   GRANT ALL PRIVILEGES ON chatapp.* TO 'chatapp_user'@'localhost';
   FLUSH PRIVILEGES;
   ```

3. **Environment Configuration**
   ```bash
   cd chatapp-spring-backend
   
   # Copy example files
   cp .env.example .env
   cp application-dev.properties.example src/main/resources/application-dev.properties
   ```
   
   Configure the following variables in `.env`:
   ```env
   # Database Configuration
   DB_URL=jdbc:mysql://localhost:3306/chatapp
   DB_USERNAME=chatapp_user
   DB_PASSWORD=your_password
   DB_PORT=3306
   
   # JWT Configuration
   JWT_SECRET_KEY=your-256-bit-secret-key
   JWT_EXPIRATION=3600000
   JWT_REFRESH_EXPIRATION=604800000
   
   # CORS
   CORS_ALLOWED_ORIGINS=

   # Application Ports
   BACKEND_PORT=8080
   FRONTEND_PORT=3000
   
   # API URLs
   VITE_API_BASE_URL=http://localhost:8080/api/v1
   VITE_WS_BASE_URL=http://localhost:8080/ws
   ```
   
   **Configure `application-dev.properties`**:
   
   Edit `src/main/resources/application-dev.properties` with your local settings:
   ```properties
   # Development profile
   spring.datasource.username=chatapp_user
   spring.datasource.password=your_password
   application.security.jwt.secret-key=your-256-bit-secret-key
   application.security.jwt.expiration=3600000
   application.security.jwt.refresh-token.expiration=604800000
   ```
   
   **Note**: Generate a secure JWT secret key using:
   ```bash
   openssl rand -base64 32
   ```

4. **Build the Application**
   ```bash
   # Clean and install dependencies
   ./mvnw clean install
   
   # Or using Maven
   mvn clean install
   ```

5. **Build and Run (Local)**
   ```bash
   # Using Maven wrapper
   ./mvnw spring-boot:run
   
   # Or using Maven
   mvn spring-boot:run
   ```

## 🐋 Docker Setup (Recommended)

### Development Environment
```bash
# From the backend repository directory
docker-compose -f docker-compose.dev.yml up --build
```

This will start:
- Backend application with hot reload
- Frontend application with hot reload  
- MySQL database
- All services networked together

### Production Environment
```bash
# From the backend repository directory
docker-compose up --build
```

This will start:
- Optimized backend application
- Optimized frontend application (built and served via Nginx)
- MySQL database

### Verify Installation
- Backend API: `http://localhost:8080`
- Frontend App: `http://localhost:5173` (dev) or `http://localhost:3000` (prod)
- Database: `localhost:3306`

## 📊 Database Schema

### Users Table
```sql
CREATE TABLE user (
    uid VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    email VARCHAR(255) UNIQUE
);
```

### Messages Table
```sql
CREATE TABLE message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    text TEXT,
    user_uid VARCHAR(36),
    FOREIGN KEY (user_uid) REFERENCES user(uid)
);
```

### Tokens Table
```sql
CREATE TABLE token (
    id INT AUTO_INCREMENT PRIMARY KEY,
    token VARCHAR(512),
    expired BOOLEAN,
    revoked BOOLEAN,
    user_uid VARCHAR(36),
    FOREIGN KEY (user_uid) REFERENCES user(uid)
);
```

## 🔐 Authentication & Security

### JWT Configuration
- **Access Token Expiration**: 1 hour (configurable)
- **Refresh Token Expiration**: 7 days (configurable)
- **Secret Key**: Configurable via environment variables

### Security Features
- Password hashing with BCrypt
- Token-based authentication
- Refresh token rotation
- CORS configuration for frontend integration
- Request/response logging via [`ApplicationLogger`](src/main/java/com/chatapp/chatapp/util/ApplicationLogger.java)

### Authentication Flow
1. User registers with [`POST /api/v1/auth/register`](src/main/java/com/chatapp/chatapp/auth/AuthController.java)
2. User logs in with [`POST /api/v1/auth/login`](src/main/java/com/chatapp/chatapp/auth/AuthController.java)
3. JWT access token and refresh token returned
4. Access token used for API authentication
5. Refresh token used to obtain new access tokens

## 📡 API Endpoints

### Authentication
- `POST /api/v1/auth/register` - User registration
- `POST /api/v1/auth/login` - User login
- `POST /api/v1/auth/refresh` - Token refresh
- `GET /api/v1/auth/validateToken` - Token validation
- `POST /api/v1/auth/logout` - User logout

### Messages
- `GET /api/v1/messages` - Get all messages
- WebSocket endpoint: `/app/chat` - Send message
- WebSocket topic: `/topic/messages` - Receive messages

### Users
- `GET /api/v1/users` - Get all users
- `POST /api/v1/users` - Create user
- `PUT /api/v1/users/{id}` - Update user
- `DELETE /api/v1/users/{id}` - Delete user

## 🔌 WebSocket Configuration

### Endpoints
- **Connect**: `/ws`
- **Send Messages**: `/app/chat`
- **Subscribe**: `/topic/messages`

### Message Flow
1. Client connects to WebSocket endpoint
2. Client sends message to `/app/chat`
3. Server processes via [`MessageController`](src/main/java/com/chatapp/chatapp/controller/MessageController.java)
4. Server broadcasts to `/topic/messages`
5. All connected clients receive the message

## ⚙️ Configuration

### Environment Variables
All configuration is managed through environment variables defined in `.env` and `application-dev.properties`

### CORS Configuration
Configured in [`CorsConfig.java`](src/main/java/com/chatapp/chatapp/config/CorsConfig.java) to allow frontend requests.

### Test Configuration
- **H2 in-memory database** for testing
- **Test profiles** with separate configuration
- **MockJwtService** in [`test_util/MockJwtService.java`](src/test/java/com/chatapp/chatapp/test_util/MockJwtService.java) for authentication testing
- **Integration tests** for controllers
- **Unit tests** for services

### Test Structure
```
src/test/java/com/chatapp/chatapp/
├── AuthControllerTest.java       # Authentication endpoint tests
├── AuthServiceTest.java          # Authentication service tests
├── JwtServiceTest.java           # JWT service tests
├── LogoutServiceTest.java        # Logout service tests
└── test_util/
    └── MockJwtService.java       # JWT mocking utility
```

## 📊 Logging

### ApplicationLogger Utility
Centralized logging utility in [`ApplicationLogger.java`](src/main/java/com/chatapp/chatapp/util/ApplicationLogger.java) provides:
- Request/response logging
- User action tracking
- Error logging with context
- Debug logging for development

## 🔧 Database Migrations

### Flyway Migrations
Located in [`src/main/resources/db/migration/`](src/main/resources/db/migration/):
- `V1__Create_user_table.sql` - Initial user table
- `V2__Create_message_table.sql` - Message table
- `V3__Create_token_table.sql` - Token table

## 🚀 Deployment

### Docker Deployment (Recommended)
The application is fully containerized with production-ready Docker configurations:

```bash
# Production deployment
docker-compose up --build

# Development with hot reload
docker-compose -f docker-compose.dev.yml up --build
```

### Manual Deployment
```bash
# Create JAR file
./mvnw clean package

# Run JAR
java -jar target/chatapp-0.0.1-SNAPSHOT.jar
```

## 🛡️ Security Considerations

### Implemented Security Features
- Password hashing with BCrypt
- JWT token expiration and refresh rotation
- Request/response logging

### Additional Security Notes
- All sensitive configuration is externalized to environment variables
- Database credentials are not hardcoded
- JWT secret keys should be properly generated and secured in production

## 🐛 Troubleshooting

### Common Issues

1. **Database Connection Errors**
   - Verify MySQL is running (check with `docker-compose ps`)
   - Check connection parameters in `.env`
   - Ensure database exists and user has permissions

2. **JWT Token Issues**
   - Verify JWT secret key configuration
   - Check token expiration settings
   - Clear browser cookies and try again

3. **WebSocket Connection Problems**
   - Check CORS configuration in [`CorsConfig.java`](src/main/java/com/chatapp/chatapp/config/CorsConfig.java)
   - Verify WebSocket endpoint mapping
   - Test with WebSocket client tools

4. **Docker Issues**
   - Ensure both repositories are in the same parent directory
   - Check Docker compose file paths
   - Verify environment variables are properly set

### Debug Mode
Enable debug logging in environment variables:
```env
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_WEB=DEBUG
LOGGING_LEVEL_ORG_SPRINGFRAMEWORK_SECURITY=DEBUG
LOGGING_LEVEL_COM_CHATAPP_CHATAPP=DEBUG
```

## 📚 Documentation

Additional documentation available in [`/documentation`](documentation/):
- Architecture diagrams
- Sequence diagrams  
- Database schema diagrams
- API flow diagrams

## 🏗️ Architecture Overview

![Application Architecture](documentation/diagrams/architecture.png)

## 🔐 Authentication Flow

![Authentication Sequence](documentation/diagrams/auth-sequence.png)

## 💬 Message Flow

![Message Flow Sequence](documentation/diagrams/message-flow-sequence.png)

## 📊 Database Schema

![Database Schema](documentation/diagrams/database-schema.png)

## 🔗 Related Projects

- [Frontend Repository](../chatapp-react-frontend) - React frontend application

## 📄 License

This project is licensed under the MIT License.

---

**Note**: This backend is designed to work with the corresponding React frontend. Both applications are orchestrated using Docker Compose for seamless development and deployment.