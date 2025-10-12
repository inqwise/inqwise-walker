---
name: Bug report
about: Create a report to help us improve
title: '[BUG] '
labels: ['bug']
assignees: ['alex-inqwise']

---

**Describe the bug**
A clear and concise description of what the bug is.

**To Reproduce**
Steps to reproduce the behavior:
1. Create walker with '...'
2. Configure event handler '....'
3. Call handle method with '....'
4. See error

**Expected behavior**
A clear and concise description of what you expected to happen.

**Code Sample**
```java
// Minimal code sample that reproduces the issue
JsonObjectWalker walker = JsonObjectWalker.instance();
walker.handler(event -> {
    // Your handler code
});
ObjectWalkingContext context = walker.handle(yourObject);
```

**Environment:**
 - inqwise-walker version: [e.g. 1.1.0]
 - Java version: [e.g. 21]
 - Maven version: [e.g. 3.9.11]
 - OS: [e.g. Ubuntu 22.04, Windows 11, macOS 14]

**Additional context**
Add any other context about the problem here, such as stack traces, logs, or related issues.

**Stack trace**
```
Paste any relevant stack trace here
```