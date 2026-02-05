# Contributing to NerveMind

Thank you for your interest in contributing to NerveMind! We welcome contributions from everyone.

## Getting Started

1.  **Fork the repository** on GitHub.
2.  **Clone your fork** locally:
    ```bash
    git clone https://github.com/YOUR_USERNAME/NerveMind.git
    cd NerveMind
    ```
3.  **Create a branch** for your feature or fix:
    ```bash
    git checkout -b feature/amazing-feature
    ```

## Development Environment

-   **Java 25** is required.
-   We use **Gradle** for build automation.
-   The project uses **Checkstyle** and **PMD** for code quality.

To run the application locally:
```bash
./gradlew :app:bootRun
```

To run tests:
```bash
./gradlew test
```

## Submitting a Pull Request

1.  Ensure your code builds and tests pass: `./gradlew check`.
2.  Commit your changes with clear messages.
3.  Push to your fork: `git push origin feature/amazing-feature`.
4.  Open a **Pull Request** to the `main` branch of `tolgayilmaz86/NerveMind`.

## Resources

-   [Architecture Guide](docs/ARCHITECTURE.md)