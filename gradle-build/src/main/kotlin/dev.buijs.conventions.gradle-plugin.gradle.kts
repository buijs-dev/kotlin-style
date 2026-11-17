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

// The jars Maven Central requires: the sources from dev.buijs.conventions.kotlin
// (withSourcesJar), and as javadoc this module's Dokka HTML, which javadoc.io shows.
mavenPublishing {
    configure(GradlePlugin(JavadocJar.Dokka("dokkaGeneratePublicationHtml"), SourcesJar.None()))
}
