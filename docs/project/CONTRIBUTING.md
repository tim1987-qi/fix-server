# Contributing to FIX Server

## 🤝 Welcome Contributors!

Thank you for your interest in contributing to the FIX Server project! This guide will help you understand how to contribute effectively to this enterprise-grade financial trading infrastructure project.

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Contribution Types](#contribution-types)
- [Development Workflow](#development-workflow)
- [Code Standards](#code-standards)
- [Testing Requirements](#testing-requirements)
- [Documentation Guidelines](#documentation-guidelines)
- [Review Process](#review-process)
- [Performance Considerations](#performance-considerations)
- [Security Guidelines](#security-guidelines)

## 📜 Code of Conduct

### Our Commitment

We are committed to providing a welcoming and inclusive environment for all contributors, regardless of background, experience level, or identity. We expect all participants to adhere to our code of conduct.

### Expected Behavior

- **Be Respectful**: Treat all community members with respect and courtesy
- **Be Collaborative**: Work together constructively and share knowledge
- **Be Professional**: Maintain professional standards in all interactions
- **Be Inclusive**: Welcome newcomers and help them get started
- **Be Constructive**: Provide helpful feedback and suggestions

### Unacceptable Behavior

- Harassment, discrimination, or offensive language
- Personal attacks or inflammatory comments
- Sharing of sensitive financial or trading information
- Violation of intellectual property rights
- Disruptive or unprofessional behavior

### Reporting Issues

If you experience or witness unacceptable behavior, please report it to the project maintainers at [security@fixserver.com](mailto:security@fixserver.com).

## 🚀 Getting Started

### Prerequisites

Before contributing, ensure you have:

- **Java 8+** (Java 11+ recommended)
- **Maven 3.6+**
- **Git** for version control
- **IDE** (IntelliJ IDEA, Eclipse, or VS Code)
- **Docker** (optional, for containerized testing)

### Initial Setup

1. **Fork the Repository**
   ```bash
   # Fork the repository on GitHub
   # Clone your fork
   git clone https://github.com/YOUR_USERNAME/fix-server.git
   cd fix-server
   ```

2. **Set Up Upstream Remote**
   ```bash
   git remote add upstream https://github.com/fixserver/fix-server.git
   git fetch upstream
   ```

3. **Verify Setup**
   ```bash
   # Build the project
   ./mvnw clean compile
   
   # Run tests
   ./mvnw test
   
   # Start the server
   ./scripts/run.sh
   ```

## 🔧 Development Setup

### IDE Configuration

#### IntelliJ IDEA
1. Import as Maven project
2. Install Lombok plugin
3. Enable annotation processing
4. Configure code style (4 spaces, no tabs)
5. Set up run configurations

#### Eclipse
1. Import as existing Maven project
2. Install Lombok (download lombok.jar and run)
3. Enable annotation processing
4. Configure formatter settings

#### VS Code
1. Install Java Extension Pack
2. Install Lombok Annotations Support
3. Configure Maven integration
4. Set up debugging configuration

### Environment Setup

```bash
# Set up development environment
export JAVA_HOME="/path/to/java11"
export MAVEN_HOME="/path/to/maven"
export PATH="$JAVA_HOME/bin:$MAVEN_HOME/bin:$PATH"

# Configure Git
git config user.name "Your Name"
git config user.email "your.email@example.com"

# Set up pre-commit hooks (optional)
./scripts/setup-hooks.sh
```

## 🎯 Contribution Types

### 1. Bug Fixes
- Fix defects in existing functionality
- Improve error handling and edge cases
- Resolve performance issues
- Address security vulnerabilities

### 2. Feature Development
- Implement new FIX protocol features
- Add performance optimizations
- Enhance monitoring and observability
- Develop client libraries and tools

### 3. Documentation
- Improve user guides and tutorials
- Update API documentation
- Create examples and samples
- Translate documentation

### 4. Testing
- Add unit and integration tests
- Develop performance benchmarks
- Create load testing scenarios
- Improve test coverage

### 5. Infrastructure
- Enhance build and deployment scripts
- Improve Docker and Kubernetes configurations
- Add monitoring and alerting
- Optimize CI/CD pipelines

## 🔄 Development Workflow

### 1. Issue Creation

Before starting work, create or find an existing issue:

```markdown
**Issue Template:**

**Type**: Bug Fix / Feature / Documentation / Performance

**Description**: 
Clear description of the issue or feature request

**Expected Behavior**: 
What should happen

**Current Behavior**: 
What actually happens (for bugs)

**Steps to Reproduce**: 
1. Step one
2. Step two
3. Step three

**Environment**: 
- Java version:
- OS:
- FIX Server version:

**Additional Context**: 
Any additional information, logs, or screenshots
```

### 2. Branch Creation

Create a feature branch for your work:

```bash
# Update your fork
git checkout main
git pull upstream main
git push origin main

# Create feature branch
git checkout -b feature/your-feature-name
# or
git checkout -b bugfix/issue-number-description
```

### 3. Development Process

```bash
# Make your changes
# ... edit files ...

# Test your changes
./mvnw test

# Run performance tests (if applicable)
./mvnw test -Dtest=*PerformanceTest

# Check code style
./mvnw checkstyle:check

# Commit your changes
git add .
git commit -m "feat: add new FIX message validation

- Implement comprehensive validation for FIX 4.4 messages
- Add unit tests for validation logic
- Update documentation with validation rules

Fixes #123"
```

### 4. Commit Message Format

Use conventional commit format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes
- `refactor`: Code refactoring
- `perf`: Performance improvements
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

**Examples:**
```
feat(parser): add FIX 5.0 message parsing support

Implement parsing for FIX 5.0 protocol messages including
new field types and validation rules.

Closes #456

fix(session): resolve memory leak in session cleanup

Fix memory leak caused by unclosed resources in session
termination process.

Fixes #789

docs(api): update client integration examples

Add comprehensive examples for different client scenarios
including error handling and reconnection logic.
```

### 5. Pull Request Process

```bash
# Push your branch
git push origin feature/your-feature-name

# Create pull request on GitHub
# Fill out the PR template completely
```

**Pull Request Template:**
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Performance improvement
- [ ] Documentation update
- [ ] Breaking change

## Testing
- [ ] Unit tests added/updated
- [ ] Integration tests added/updated
- [ ] Performance tests added/updated
- [ ] Manual testing completed

## Performance Impact
- [ ] No performance impact
- [ ] Performance improvement
- [ ] Potential performance regression (explain)

## Documentation
- [ ] Documentation updated
- [ ] API documentation updated
- [ ] Examples updated

## Checklist
- [ ] Code follows style guidelines
- [ ] Self-review completed
- [ ] Tests pass locally
- [ ] Performance benchmarks pass
- [ ] Security considerations addressed
```

## 📏 Code Standards

### 1. Java Code Style

#### Naming Conventions
```java
// Classes: PascalCase
public class FIXProtocolHandler {
    
    // Constants: UPPER_SNAKE_CASE
    private static final int MAX_MESSAGE_SIZE = 8192;
    
    // Variables and methods: camelCase
    private MessageStore messageStore;
    
    public void processMessage(FIXMessage message) {
        // Implementation
    }
}
```

#### Code Organization
```java
/**
 * Class-level documentation with purpose and usage.
 * 
 * @author Your Name
 * @since 1.0.0
 */
@Component
public class ExampleClass {
    
    // 1. Static fields
    private static final Logger logger = LoggerFactory.getLogger(ExampleClass.class);
    
    // 2. Instance fields
    private final MessageStore messageStore;
    
    // 3. Constructor
    public ExampleClass(MessageStore messageStore) {
        this.messageStore = messageStore;
    }
    
    // 4. Public methods
    public void publicMethod() {
        // Implementation
    }
    
    // 5. Private methods
    private void privateMethod() {
        // Implementation
    }
}
```

#### Error Handling
```java
public class ExampleService {
    
    public void processMessage(FIXMessage message) throws FIXProtocolException {
        try {
            validateMessage(message);
            storeMessage(message);
        } catch (ValidationException e) {
            logger.warn("Message validation failed: {}", e.getMessage());
            throw new FIXProtocolException("Invalid message format", e);
        } catch (StorageException e) {
            logger.error("Failed to store message", e);
            throw new FIXProtocolException("Storage failure", e);
        }
    }
}
```

### 2. Performance Guidelines

#### Memory Efficiency
```java
// Good: Reuse objects
private final StringBuilder stringBuilder = new StringBuilder(1024);

public String formatMessage(FIXMessage message) {
    stringBuilder.setLength(0); // Reset
    // Build message
    return stringBuilder.toString();
}

// Good: Use object pooling
@Autowired
private ObjectPool<FIXMessage> messagePool;

public FIXMessage createMessage() {
    return messagePool.borrowObject();
}

public void releaseMessage(FIXMessage message) {
    message.reset();
    messagePool.returnObject(message);
}
```

#### Avoid Common Pitfalls
```java
// Bad: String concatenation in loops
String result = "";
for (Field field : fields) {
    result += field.toString(); // Creates new String objects
}

// Good: Use StringBuilder
StringBuilder result = new StringBuilder();
for (Field field : fields) {
    result.append(field.toString());
}

// Bad: Unnecessary object creation
public String getFieldValue(int tag) {
    return fields.get(String.valueOf(tag)); // Creates String object
}

// Good: Use primitive collections
private final TIntObjectHashMap<String> fields = new TIntObjectHashMap<>();

public String getFieldValue(int tag) {
    return fields.get(tag);
}
```

### 3. Security Guidelines

#### Input Validation
```java
public void setField(int tag, String value) {
    // Validate input parameters
    if (tag <= 0) {
        throw new IllegalArgumentException("Tag must be positive: " + tag);
    }
    if (value == null) {
        throw new IllegalArgumentException("Value cannot be null");
    }
    if (value.length() > MAX_FIELD_LENGTH) {
        throw new IllegalArgumentException("Value too long: " + value.length());
    }
    
    // Sanitize input
    String sanitizedValue = sanitizeFieldValue(value);
    fields.put(tag, sanitizedValue);
}
```

#### Sensitive Data Handling
```java
public class AuditLogger {
    
    public void logMessage(FIXMessage message) {
        // Remove sensitive fields before logging
        FIXMessage sanitized = message.copy();
        sanitized.removeField(FIXTags.PASSWORD);
        sanitized.removeField(FIXTags.NEW_PASSWORD);
        
        logger.info("Message processed: {}", sanitized.toFixString());
    }
}
```

## 🧪 Testing Requirements

### 1. Unit Testing

#### Test Structure
```java
@ExtendWith(MockitoExtension.class)
class FIXProtocolHandlerTest {
    
    @Mock
    private MessageStore messageStore;
    
    @Mock
    private SessionManager sessionManager;
    
    @InjectMocks
    private FIXProtocolHandler handler;
    
    @Test
    @DisplayName("Should process valid logon message successfully")
    void shouldProcessValidLogonMessage() {
        // Given
        FIXMessage logonMessage = createValidLogonMessage();
        FIXSession session = mock(FIXSession.class);
        
        // When
        FIXMessage response = handler.processMessage(logonMessage, session);
        
        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMessageType()).isEqualTo("A");
        verify(messageStore).storeMessage(any(), any(), any());
    }
    
    @Test
    @DisplayName("Should reject invalid message format")
    void shouldRejectInvalidMessageFormat() {
        // Given
        FIXMessage invalidMessage = createInvalidMessage();
        FIXSession session = mock(FIXSession.class);
        
        // When & Then
        assertThatThrownBy(() -> handler.processMessage(invalidMessage, session))
            .isInstanceOf(FIXProtocolException.class)
            .hasMessageContaining("Invalid message format");
    }
}
```

#### Test Coverage Requirements
- **Minimum Coverage**: 80% overall
- **Critical Paths**: 95% coverage for performance-critical code
- **Edge Cases**: Test all error conditions and edge cases
- **Integration Points**: Test all external integrations

### 2. Integration Testing

```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class FIXServerIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:13");
    
    @Autowired
    private FIXProtocolServer server;
    
    @Test
    void shouldHandleCompleteMessageFlow() throws Exception {
        // Test complete message processing flow
        // from client connection to database storage
    }
}
```

### 3. Performance Testing

```java
@Test
@DisplayName("Message parsing should meet performance targets")
void messageParsingPerformanceTest() {
    // Given
    String testMessage = createLargeTestMessage();
    int iterations = 10000;
    
    // When
    long startTime = System.nanoTime();
    for (int i = 0; i < iterations; i++) {
        parser.parseMessage(testMessage);
    }
    long endTime = System.nanoTime();
    
    // Then
    double avgTimeNanos = (endTime - startTime) / (double) iterations;
    double avgTimeMicros = avgTimeNanos / 1000.0;
    
    assertThat(avgTimeMicros).isLessThan(100.0); // Target: <100μs
}
```

## 📚 Documentation Guidelines

### 1. Code Documentation

#### Javadoc Standards
```java
/**
 * Processes incoming FIX messages and manages session state.
 * 
 * This class handles the core FIX protocol logic including message
 * validation, session management, and response generation. It supports
 * both FIX 4.4 and FIX 5.0 protocols.
 * 
 * <p>Example usage:
 * <pre>{@code
 * FIXProtocolHandler handler = new FIXProtocolHandler(messageStore, sessionManager);
 * FIXMessage response = handler.processMessage(incomingMessage, session);
 * }</pre>
 * 
 * @author FIX Server Team
 * @version 1.0.0
 * @since 1.0.0
 * @see FIXMessage
 * @see FIXSession
 */
public class FIXProtocolHandler {
    
    /**
     * Processes a FIX message from a client session.
     * 
     * @param message the FIX message to process, must not be null
     * @param session the client session that sent the message, must not be null
     * @return the response message, or null if no response is needed
     * @throws FIXProtocolException if the message is invalid or processing fails
     * @throws IllegalArgumentException if message or session is null
     */
    public FIXMessage processMessage(FIXMessage message, FIXSession session) 
            throws FIXProtocolException {
        // Implementation
    }
}
```

### 2. User Documentation

#### README Updates
When adding new features, update relevant README sections:

```markdown
## New Feature: Advanced Message Validation

### Overview
The advanced message validation feature provides comprehensive
validation of FIX messages according to protocol specifications.

### Usage
```java
FIXValidator validator = new FIXValidator();
ValidationResult result = validator.validate(message);
if (!result.isValid()) {
    // Handle validation errors
}
```

### Configuration
```yaml
fix:
  validation:
    enabled: true
    strict-mode: true
    custom-rules: classpath:validation-rules.xml
```
```

### 3. API Documentation

Keep API documentation current with code changes:

```java
/**
 * REST API for FIX server management.
 * 
 * @RestController
 * @RequestMapping("/api/v1/fix")
 */
public class FIXManagementController {
    
    /**
     * Get active FIX sessions.
     * 
     * @return list of active sessions
     * @response 200 Success
     * @response 500 Internal server error
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionInfo>> getActiveSessions() {
        // Implementation
    }
}
```

## 🔍 Review Process

### 1. Self-Review Checklist

Before submitting a pull request:

- [ ] **Code Quality**
  - [ ] Code follows style guidelines
  - [ ] No code smells or anti-patterns
  - [ ] Proper error handling
  - [ ] No hardcoded values

- [ ] **Testing**
  - [ ] Unit tests added/updated
  - [ ] Integration tests added/updated
  - [ ] All tests pass locally
  - [ ] Test coverage meets requirements

- [ ] **Performance**
  - [ ] No performance regressions
  - [ ] Memory usage optimized
  - [ ] Benchmarks pass

- [ ] **Security**
  - [ ] Input validation implemented
  - [ ] No security vulnerabilities
  - [ ] Sensitive data handled properly

- [ ] **Documentation**
  - [ ] Code documented
  - [ ] API documentation updated
  - [ ] User guides updated

### 2. Peer Review Process

#### Review Guidelines for Reviewers

1. **Functionality Review**
   - Does the code solve the intended problem?
   - Are edge cases handled properly?
   - Is error handling comprehensive?

2. **Code Quality Review**
   - Is the code readable and maintainable?
   - Does it follow established patterns?
   - Are there any code smells?

3. **Performance Review**
   - Are there any performance implications?
   - Is memory usage optimized?
   - Are there any blocking operations?

4. **Security Review**
   - Is input properly validated?
   - Are there any security vulnerabilities?
   - Is sensitive data handled correctly?

#### Review Comments

Provide constructive feedback:

```markdown
**Good Example:**
"Consider using a StringBuilder here for better performance when 
concatenating multiple strings in the loop. This will reduce 
memory allocations."

**Bad Example:**
"This is wrong."
```

### 3. Approval Process

- **Required Approvals**: 2 approvals from maintainers
- **Automated Checks**: All CI checks must pass
- **Performance Tests**: Performance benchmarks must pass
- **Security Scan**: Security vulnerability scan must pass

## ⚡ Performance Considerations

### 1. Performance Impact Assessment

For all changes, consider:

- **Latency Impact**: Will this change affect message processing latency?
- **Throughput Impact**: Will this change affect message throughput?
- **Memory Impact**: Will this change affect memory usage?
- **CPU Impact**: Will this change affect CPU utilization?

### 2. Performance Testing

Run performance tests for changes that might impact performance:

```bash
# Run performance benchmarks
./mvnw test -Dtest=*PerformanceTest

# Run load tests
./scripts/load-test.sh

# Profile with JFR
./scripts/profile.sh
```

### 3. Performance Regression Prevention

- **Baseline Measurements**: Establish performance baselines
- **Automated Testing**: Include performance tests in CI
- **Monitoring**: Monitor performance metrics in production
- **Alerting**: Set up alerts for performance degradation

## 🔒 Security Guidelines

### 1. Security Review Requirements

All contributions must consider security implications:

- **Input Validation**: Validate all external inputs
- **Authentication**: Verify user authentication
- **Authorization**: Check user permissions
- **Data Protection**: Protect sensitive data
- **Audit Logging**: Log security-relevant events

### 2. Security Testing

```java
@Test
void shouldRejectMaliciousInput() {
    // Test with various malicious inputs
    String[] maliciousInputs = {
        "<script>alert('xss')</script>",
        "'; DROP TABLE messages; --",
        "../../../etc/passwd",
        "\\x00\\x01\\x02"
    };
    
    for (String input : maliciousInputs) {
        assertThatThrownBy(() -> service.processInput(input))
            .isInstanceOf(ValidationException.class);
    }
}
```

### 3. Vulnerability Reporting

If you discover a security vulnerability:

1. **Do NOT** create a public issue
2. **Email** security@fixserver.com with details
3. **Include** steps to reproduce
4. **Wait** for acknowledgment before disclosure

## 🎯 Getting Help

### 1. Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: General questions and discussions
- **Email**: maintainers@fixserver.com for private matters
- **Documentation**: Comprehensive guides and API reference

### 2. Mentorship Program

New contributors can request mentorship:

- **Pairing Sessions**: Code review and guidance
- **Architecture Discussions**: Understanding system design
- **Best Practices**: Learning development practices
- **Career Development**: Growth opportunities

### 3. Office Hours

Regular office hours for contributor support:

- **When**: Every Friday 2-4 PM UTC
- **Where**: GitHub Discussions
- **What**: Q&A, code review, architecture discussions

## 🏆 Recognition

### 1. Contributor Recognition

We recognize contributors through:

- **Contributors File**: Listed in CONTRIBUTORS.md
- **Release Notes**: Mentioned in release announcements
- **Blog Posts**: Featured in technical blog posts
- **Conference Talks**: Speaking opportunities at events

### 2. Contribution Levels

- **First-time Contributor**: Welcome package and guidance
- **Regular Contributor**: Recognition in community
- **Core Contributor**: Commit access and review privileges
- **Maintainer**: Project leadership and decision-making

## 📝 License

By contributing to this project, you agree that your contributions will be licensed under the same license as the project (Apache License 2.0).

## 🙏 Thank You

Thank you for contributing to the FIX Server project! Your contributions help make financial trading infrastructure more reliable, performant, and secure for the entire community.

For questions about contributing, please reach out to the maintainers or create a discussion on GitHub. We're here to help and excited to work with you!