plugins {
    id("com.vanniktech.maven.publish")
}

group = providers.gradleProperty("conventions.group").get()

val repository = providers.gradleProperty("conventions.repository").get()
val displayName = providers.gradleProperty("conventions.displayName").get()
val githubUrl = "https://github.com/buijs-dev/$repository"

// Signed only with a signing key (signingInMemoryKey or signing.keyId), so local and snapshot
// builds need none.
val signed =
    providers.gradleProperty("signingInMemoryKey").isPresent ||
        providers.gradleProperty("signing.keyId").isPresent

// Releases and snapshots go to Repsy (publishAllPublicationsToRepsyRepository), as the
// buijs-dev/ci-templates publish workflows run it; the style is build tooling and never goes to
// Maven Central. The credentials come from the environment those workflows set.
mavenPublishing {
    if (signed) signAllPublications()

    pom {
        val nameConvention = if (project == rootProject) {
            displayName
        } else {
            "$displayName - ${project.name}"
        }

        name.convention(nameConvention)
        description.convention(provider { project.description })
        url.convention(githubUrl)

        licenses {
            license {
                name = "MIT License"
                url = "$githubUrl/blob/main/LICENSE"
            }
        }

        developers {
            developer {
                id = "buijs-dev"
                name = "Gillian Buijs"
                email = "info@buijs.dev"
            }
        }

        scm {
            connection = "scm:git:$githubUrl.git"
            developerConnection = "scm:git:ssh://git@github.com/buijs-dev/$repository.git"
            url = githubUrl
        }
    }
}

publishing {
    repositories {
        maven {
            name = "Repsy"
            url = uri("https://repo.repsy.io/mvn/buijs-dev/maven")
            credentials {
                username = providers.environmentVariable("REPSY_USERNAME").orNull
                password = providers.environmentVariable("REPSY_PASSWORD").orNull
            }
        }
    }
}
