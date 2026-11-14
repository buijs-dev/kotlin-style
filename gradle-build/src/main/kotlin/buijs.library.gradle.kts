plugins {
    id("buijs.kotlin")
    id("buijs.repositories")
    id("buijs.publishing")
    id("buijs.coverage")
    id("buijs.dokka")
    `java-library`
}

publishing {
    publications.create<MavenPublication>("mavenJava") { from(components["java"]) }
}
