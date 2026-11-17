import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinJvm
import com.vanniktech.maven.publish.SourcesJar

plugins {
    id("dev.buijs.conventions.kotlin")
    id("dev.buijs.conventions.repositories")
    id("dev.buijs.conventions.publishing")
    id("dev.buijs.conventions.coverage")
    id("dev.buijs.conventions.dokka")
    `java-library`
}

// The jars every publication carries: the sources from dev.buijs.conventions.kotlin
// (withSourcesJar), and as javadoc this module's Dokka HTML.
mavenPublishing {
    configure(KotlinJvm(JavadocJar.Dokka("dokkaGeneratePublicationHtml"), SourcesJar.None()))
}
