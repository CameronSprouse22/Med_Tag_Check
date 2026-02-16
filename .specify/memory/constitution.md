<!--
Sync Impact Report - Constitution v1.0.0
========================================
Version Change: Initial creation → 1.0.0
Rationale: First constitution for Med Check Tag Android application

Added Sections:
- Core Principles (5 principles defined)
  1. User Safety First - Medical app safety standards
  2. Privacy by Design - Health data protection
  3. Test-Driven Development - Reliability requirements
  4. NFC Reliability - Core NFC functionality standards
  5. Simplicity & Android Standards - Platform best practices
- Technology Stack - Java, Android, NFC specifications
- Development Workflow - Testing, review, deployment processes
- Governance - Amendment and compliance procedures

Templates Status:
- plan-template.md: ✅ Compatible (Constitution Check section will apply new principles)
- spec-template.md: ✅ Compatible (User scenarios align with safety/privacy principles)
- tasks-template.md: ✅ Compatible (Task organization supports TDD and testing gates)

Follow-up TODOs: None - all placeholders filled

Date: 2026-02-14
-->

# Med Check Tag Constitution

## Core Principles

### I. User Safety First

**NON-NEGOTIABLE**: This application handles medication information, which directly impacts user health and safety.

- All medication data MUST be validated for completeness and accuracy before storage
- NFC read/write operations MUST include verification steps to prevent data corruption
- Error messages MUST be clear, actionable, and never misleading to users
- User confirmations MUST be required for all medication-related actions (add, modify, delete)
- The application MUST gracefully handle all failure scenarios without data loss
- UI MUST prevent accidental actions through appropriate confirmation dialogs

**Rationale**: Medication errors can have serious health consequences. Safety gates and validations are mandatory to protect users.

### II. Privacy by Design

**MANDATORY**: Health information is sensitive personal data that MUST be protected.

- All medication data MUST be stored locally on the device only (no cloud sync by default)
- No medication information shall be transmitted to external services without explicit user consent
- NFC tags MUST be encrypted if they contain identifiable medical information
- Application MUST not require unnecessary permissions beyond NFC and local storage
- Logs and crash reports MUST NOT contain medication names, dosages, or schedules
- User data export features MUST include privacy warnings

**Rationale**: Health data privacy is both an ethical obligation and a legal requirement (HIPAA considerations for US users). Local-first design protects user privacy.

### III. Test-Driven Development

**NON-NEGOTIABLE**: Reliability is critical for medical applications.

- Tests MUST be written before implementation (Red-Green-Refactor cycle)
- All NFC operations MUST have unit tests with mock NFC tags
- All database operations MUST have instrumented tests on actual Android devices
- Every user story MUST have corresponding instrumented UI tests
- Code coverage MUST be maintained at minimum 80% for core medication logic
- Tests MUST pass before any code can be merged

**Rationale**: Healthcare applications demand the highest reliability standards. TDD ensures code correctness and prevents regression.

### IV. NFC Reliability

**MANDATORY**: NFC tag reading/writing is the core functionality and MUST work consistently.

- NFC operations MUST include retry logic with exponential backoff (max 3 attempts)
- Tag format MUST be standardized (NDEF format) with version metadata
- Application MUST validate tag data integrity using checksums
- Users MUST receive clear feedback during NFC operations (scanning, writing, success, failure)
- Application MUST handle partial reads/writes gracefully without corrupting data
- NFC functionality MUST degrade gracefully on devices without NFC hardware (read-only mode)

**Rationale**: NFC operations can be unreliable due to physical positioning, interference, and tag quality. Robust error handling is essential for user trust.

### V. Simplicity & Android Standards

**MANDATORY**: Follow Android platform conventions and keep the codebase maintainable.

- Use Android Jetpack libraries (Room for database, ViewModel for UI state, WorkManager for background tasks)
- Follow Material Design guidelines for UI/UX consistency
- Implement proper Activity/Fragment lifecycle management
- Use Android's built-in NFC APIs (android.nfc package)
- Code MUST follow Java naming conventions and Android code style
- Keep architecture simple: MVVM pattern with clear separation of concerns
- YAGNI principle: Implement only what's specified, no speculative features

**Rationale**: Android provides robust platform libraries and design patterns. Using them ensures compatibility, maintainability, and familiar UX for users.

## Technology Stack

**Language**: Java (Android SDK minimum API Level 21 / Android 5.0+)

**Required Dependencies**:
- AndroidX Core libraries (AppCompat, ConstraintLayout, Material Components)
- AndroidX Room (local database persistence)
- AndroidX Lifecycle (ViewModel, LiveData)
- Android NFC API (android.nfc, android.nfc.tech)

**Testing Framework**:
- JUnit 4 for unit tests
- AndroidX Test (Espresso for UI tests, JUnit rules for instrumented tests)
- Mockito for mocking NFC and database components

**Build System**: Gradle with Android Gradle Plugin

**Minimum Requirements**:
- Android 5.0 (API 21) or higher
- NFC hardware capability (android.hardware.nfc feature)

**Constraints**:
- App size target: < 10MB (minimal dependencies)
- Startup time: < 2 seconds on mid-range devices
- NFC read/write operations: < 3 seconds for typical medication data

## Development Workflow

**Constitution Compliance Gates**:
1. **Pre-Implementation**: Verify spec includes safety validations and privacy considerations
2. **Pre-Commit**: All tests pass, code coverage meets 80% threshold
3. **Pre-Merge**: Peer review confirms adherence to all five core principles

**Testing Requirements**:
- Unit tests for all business logic (medication validation, NFC data formatting)
- Instrumented tests for database operations (Room DAO tests)
- UI tests for critical user journeys (add medication via NFC, view medication list)
- Manual testing on real NFC tags before each release

**Code Review Standards**:
- All code MUST be reviewed by at least one other developer
- Reviews MUST verify safety validations are present and correct
- Reviews MUST check for privacy leaks (logging sensitive data)
- Reviews MUST confirm test coverage for new code

**Version Control**:
- Feature branches follow pattern: `###-feature-name`
- Commit messages MUST reference user story or task ID
- No direct commits to main branch

**Release Process**:
- All tests MUST pass in CI/CD pipeline
- Manual testing on minimum 2 physical devices with different Android versions
- Privacy audit checklist MUST be completed
- Release notes MUST document any changes to NFC tag format or database schema

## Governance

**Amendment Process**:
1. Proposed changes MUST be documented with rationale
2. Team review and consensus required for amendments
3. Version bump according to semantic versioning:
   - **MAJOR**: Removal or redefinition of core principles (requires full project audit)
   - **MINOR**: New principle added or substantial guidance expansion
   - **PATCH**: Clarifications, wording improvements, non-semantic updates
4. Migration plan MUST accompany any change affecting existing code
5. All amendments MUST update sync impact report and propagate to templates

**Compliance Oversight**:
- This constitution supersedes all other development practices
- All specification documents MUST reference relevant principles
- All task breakdowns MUST include constitution compliance verification
- All pull requests MUST include a constitution compliance statement
- Quarterly constitution review to ensure relevance and effectiveness

**Complexity Justification**:
- Any violation of core principles MUST be documented with justification
- Temporary exceptions require explicit approval and remediation timeline
- Complexity must be justified against user safety and privacy requirements

**Version**: 1.0.0 | **Ratified**: 2026-02-14 | **Last Amended**: 2026-02-14
