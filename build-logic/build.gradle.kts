plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

gradlePlugin {
    plugins {
        register("common") {
            id = "elytratrims-common"
            implementationClass = "CommonPlugin"
        }
    }
}
