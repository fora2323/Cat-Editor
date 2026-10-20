plugins {
    kotlin("jvm") version "2.0.0"
    application
}

group = "__PACKAGE_NAME__"
version = "1.0.0"

repositories {
    mavenCentral()
}

application {
    mainClass.set("__PACKAGE_NAME__.MainKt")
}
