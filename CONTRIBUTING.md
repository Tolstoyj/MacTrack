# Contributing to DroidPad

Thank you for your interest in contributing to DroidPad! This document provides guidelines and instructions for contributing.

## Code of Conduct

- Be respectful and inclusive
- Welcome newcomers and help them learn
- Focus on constructive feedback
- Respect different viewpoints and experiences

## How Can I Contribute?

### Reporting Bugs

Before creating a bug report:
1. Check if the issue already exists
2. Test with the latest version
3. Gather relevant information

**Bug Report Template:**
```markdown
**Describe the bug**
A clear description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Go to '...'
2. Click on '...'
3. See error

**Expected behavior**
What you expected to happen.

**Screenshots**
If applicable, add screenshots.

**Device Information:**
- Android Version: 
- Device Model: 
- macOS Version: 

**Additional context**
Add any other context about the problem.
```

### Suggesting Features

Feature suggestions are welcome! Please:
1. Check if the feature already exists or is planned
2. Provide a clear description
3. Explain the use case
4. Consider implementation complexity

### Pull Requests

1. **Fork the repository**
2. **Create a feature branch**
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. **Make your changes**
   - Follow code style guidelines
   - Add tests if applicable
   - Update documentation
4. **Commit your changes**
   ```bash
   git commit -m "Add: Description of your feature"
   ```
5. **Push to your fork**
   ```bash
   git push origin feature/your-feature-name
   ```
6. **Create a Pull Request**
   - Provide a clear description
   - Reference related issues
   - Add screenshots if UI changes

## Development Setup

1. Clone the repository
2. Open in Android Studio
3. Sync Gradle dependencies
4. Run on a device or emulator

## Code Style

### Kotlin Style Guide

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use meaningful names
- Keep functions focused (single responsibility)
- Add KDoc comments for public APIs

### Formatting

- Use 4 spaces for indentation
- Maximum line length: 120 characters
- Use trailing commas in multi-line lists
- Organize imports

### Example

```kotlin
/**
 * Sends mouse movement to the connected device.
 *
 * @param deltaX Horizontal movement delta
 * @param deltaY Vertical movement delta
 */
fun sendMouseMovement(deltaX: Int, deltaY: Int) {
    // Implementation
}
```

## Project Structure

- `app/src/main/java/com/dps/droidpadmacos/`
  - `MainActivity.kt` - Main entry point
  - `bluetooth/` - Bluetooth HID implementation
  - `touchpad/` - Gesture detection
  - `sensor/` - Sensor-based features
  - `viewmodel/` - Business logic
  - `ui/` - UI components
  - `data/` - Data management

## Testing

When adding features:
- Test on multiple Android versions if possible
- Test Bluetooth connectivity
- Verify gesture recognition
- Test edge cases

## Commit Messages

Use clear, descriptive commit messages:

```
Add: Air Mouse sensitivity adjustment
Fix: Connection timeout issue
Update: Improve gesture detection accuracy
Refactor: Simplify Bluetooth service initialization
```

## Questions?

Feel free to:
- Open an issue for questions
- Start a discussion
- Contact maintainers

Thank you for contributing! 🎉

