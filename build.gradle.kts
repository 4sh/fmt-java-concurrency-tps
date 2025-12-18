import org.gradle.internal.jvm.inspection.JvmVendor

// Configure the JVM version used by Gradle itself (NOT by the app we build)
// Run ./gradlew updateDaemonJvm to update
tasks.updateDaemonJvm {
    languageVersion = JavaLanguageVersion.of(21)
    vendor = JvmVendorSpec.ADOPTIUM
}
