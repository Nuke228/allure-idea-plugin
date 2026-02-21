# Allure TestOps Integration — IntelliJ IDEA Plugin

An IntelliJ IDEA plugin that creates test cases in [Allure TestOps](https://qameta.io/) directly from your `@Test` methods and adds the `@AllureId` annotation back into the source code.

## Features

- Right-click on any `@Test` method and create a test case in Allure TestOps in one click
- Automatically adds `@AllureId("N")` annotation to the method after creation
- Uses `@DisplayName` value as the test case name when present, falls back to the method name
- Supports JUnit 4, JUnit 5, and TestNG test annotations
- Settings UI under **Settings > Tools > Allure TestOps** with connection test and project selection
- Secure token storage via IntelliJ's built-in credential manager

## Installation

1. Download the latest `allure-idea-plugin-x.x.x.zip` from [Releases](https://github.com/Nuke228/allure-idea-plugin/releases)
2. In IntelliJ IDEA: **Settings > Plugins > Gear icon > Install Plugin from Disk...**
3. Select the zip file and restart the IDE

## Configuration

1. Go to **Settings > Tools > Allure TestOps**
2. Enter your Allure TestOps URL (e.g. `https://allure.example.com`)
3. Enter your API token
4. Click **Test Connection & Load Projects** to verify
5. Select the target project from the dropdown
6. Click **Apply**

### Obtaining an API Token

1. Log in to your Allure TestOps instance
2. Go to your profile settings
3. Generate an API token

## Usage

1. Open a test class in the editor
2. Place your cursor on a `@Test` method that doesn't have `@AllureId`
3. Right-click and select **Create Allure Test Case**
4. The plugin will:
   - Create a test case in Allure TestOps
   - Add `@AllureId("N")` annotation to the method
   - Show a notification with the created test case ID

### Example

Before:
```kotlin
@Test
@DisplayName("User can log in with valid credentials")
fun `user can log in`() { ... }
```

After clicking **Create Allure Test Case**:
```kotlin
@AllureId("12345")
@Test
@DisplayName("User can log in with valid credentials")
fun `user can log in`() { ... }
```

The test case name in Allure TestOps will be `"User can log in with valid credentials"` (from `@DisplayName`).

## Building from Source

```bash
./gradlew buildPlugin
```

The plugin archive will be at `build/distributions/allure-idea-plugin-x.x.x.zip`.

## Requirements

- IntelliJ IDEA 2024.1+
- Java 17+
- Allure TestOps instance with API access

## License

This project is licensed under the Apache License 2.0 — see the [LICENSE](LICENSE) file for details.
