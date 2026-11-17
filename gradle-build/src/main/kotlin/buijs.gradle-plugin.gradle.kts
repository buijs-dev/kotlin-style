import com.vanniktech.maven.publish.GradlePlugin
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("buijs.kotlin")
    id("buijs.repositories")
    id("buijs.publishing")
    id("buijs.coverage")
    id("buijs.dokka")
    `java-gradle-plugin`
}

// The sources and javadoc jars Maven Central requires come from buijs.kotlin (withSourcesJar,
// withJavadocJar), so the plugin adds none of its own.
mavenPublishing {
    configure(GradlePlugin(JavadocJar.None(), SourcesJar.None()))
}
