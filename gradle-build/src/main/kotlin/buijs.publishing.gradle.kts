plugins {
    `maven-publish`
}

group = providers.gradleProperty("conventions.group").get()

val repository = providers.gradleProperty("conventions.repository").get()
val displayName = providers.gradleProperty("conventions.displayName").get()
val githubUrl = "https://github.com/buijs-dev/$repository"

publishing {
    publications.withType<MavenPublication>().configureEach {
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
}

// Release versions go to GitHub Packages, snapshots to Repsy. Credentials come from the
// environment set by the buijs-dev/ci-templates publish workflows.
publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/buijs-dev/$repository")
            credentials {
                username = providers.environmentVariable("GITHUB_ACTOR").orNull
                password = providers.environmentVariable("GITHUB_TOKEN").orNull
            }
        }

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
