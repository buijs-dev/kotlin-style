import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("dev.buijs.conventions.kotlin")
    id("dev.buijs.conventions.repositories")
    id("dev.buijs.conventions.publishing")
    id("dev.buijs.conventions.coverage")
    id("dev.buijs.conventions.dokka")
    `java-gradle-plugin`
}

// The sources and javadoc jars Maven Central requires come from dev.buijs.conventions.kotlin (withSourcesJar,
// withJavadocJar), so the plugin adds none of its own.
mavenPublishing {
    configure(GradlePlugin(JavadocJar.None(), SourcesJar.None()))
}
