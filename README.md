# Zylo
Enterprise-grade e-commerce platform API with dual authentication systems,  real-time order streaming, and distributed caching. Built with Spring Boot,  Kafka, Redis, MongoDB, and Protobuf. Implements session-based auth (Shiro),  cloud auth (AWS Cognito), MQTT device integration, and production-ready  monitoring and testing.

## Project Structure
Modular Structure (Easier to break down into Microservices later)
```bash
Zylo/
├── src/
│   ├── main/
│   │   ├── java/com/example/zylo/
│   │   │   ├── user/          # User package
│   │   │   ├── product/       # Product package
│   │   │   ├── order/         # Order package
│   │   │   ├── device-simulator/          # Device package
│   │   │   ├── shared/common/      # Shared functionality package (if any)
│   │   │   └── ZyloApplication.java
│   │   └── resources/
│   │       ├── static/          # CSS, JS, Images
│   │       ├── templates/       # HTML
│   │       ├── db/init.sql      # SQL Scripts to initialize the DB locally
│   │       └── application.properties
│   └── test/                    # Unit and Integration Tests
├── .env                         # Configuration data
├── pom.xml (or build.gradle)    # Build configuration
└── README.md
```


## Git
```
git branch feat/name
git checkout feat/name
git add .
git commit -m "message"
git push origin feat/name
```

## Git Commits Naming Convention
<pre>
<b>build</b>: Changes that affect the build system or external dependencies
<b>ci</b>: Changes to our CI configuration files and scripts
<b>docs</b>: Documentation only changes
<b>feat</b>: A new feature
<b>fix</b>: A bug fix
<b>perf</b>: A code change that improves performance
<b>refactor</b>: A code change that neither fixes a bug nor adds a feature
<b>style</b>: Changes that do not affect the meaning of the code (white-space, formatting, missing semi-colons, etc)
<b>test</b>: Adding missing tests or correcting existing tests
</pre>
