plugins {
    id("net.neoforged.moddev") version "2.0.78"
}

version = "1.22"
group = "ink.astrius"
base { archivesName = "driftward-fixes" }

java.toolchain.languageVersion = JavaLanguageVersion.of(21)

fun RepositoryHandler.repository(url: String, vararg groups: String) {
    exclusiveContent {
        forRepository {
            maven { this.url = uri(url) }
        }
        filter {
            groups.forEach { includeGroup(it) }
        }
    }
}

repositories {
    mavenCentral()
    repository(
        "https://maven.createmod.net",
        "com.simibubi.create", "net.createmod.ponder", "dev.engine-room.flywheel"
    )
    repository("https://maven.ithundxr.dev/snapshots", "com.tterrag.registrate")
    repository(
        "https://raw.githubusercontent.com/Fuzss/modresources/main/maven/",
        "fuzs.forgeconfigapiport"
    )
    repository(
        "https://maven.ryanhcode.dev/releases",
        "dev.ryanhcode.sable", "dev.ryanhcode.sable-companion",
        "dev.eriksonn.aeronautics", "dev.simulated_team.simulated"
    )
    repository(
        "https://maven.blamejared.com/",
        "foundry.veil", "gg.moonflower", "io.github.ocelot", "mezz.jei"
    )
    repository("https://api.modrinth.com/maven", "maven.modrinth")
    repository(
        "https://jitpack.io",
        "com.github.TheDeathlyCow", "com.github.Fallen-Breath.conditional-mixin",
    )
    repository(
        "https://maven.sinytra.org",
        "org.sinytra", "org.sinytra.forgified-fabric-api"
    )
    repository("https://maven.theillusivec4.top", "top.theillusivec4.curios")
    repository("https://maven.squiddev.cc", "cc.tweaked")
    repository("https://repo.sleeping.town/", "dev.emi")
}

neoForge {
    version = "21.1.247"
    mods {
        create("driftwardfixes") { sourceSet(sourceSets.main.get()) }
    }
    parchment {
        minecraftVersion = "1.21.1"
        mappingsVersion = "2024.11.17"
    }

    runs {
        create("client") {
            client()
            gameDirectory = project.file("run/client")
        }

        create("server") {
            server()
            gameDirectory = project.file("run/server")
        }
    }
}

dependencies {
    implementation("maven.modrinth:critters-and-companions:kGomvo87")  // 2.6.2
    implementation("maven.modrinth:supplementaries:1.21.1-3.9.3")
    implementation("maven.modrinth:farmers-delight:1.21.1-1.3.2")
    implementation("com.simibubi.create:create-1.21.1:6.0.10-280:slim") { isTransitive = false }
    implementation("com.tterrag.registrate:Registrate:MC1.21-1.3.0+67")
    implementation("dev.eriksonn.aeronautics:aeronautics-neoforge-1.21.1:1.3.0")
    implementation("dev.ryanhcode.sable:sable-neoforge-1.21.1:2.0.3")
    implementation("dev.simulated_team.simulated:simulated-neoforge-1.21.1:1.3.0")
    runtimeOnly("mezz.jei:jei-1.21.1-neoforge:19.43.0.392")
    implementation("maven.modrinth:thirst-was-reclaimed:1.21.1-3.0.4")
    implementation("com.github.TheDeathlyCow:thermoo:v4.8.1-neoforge")
    implementation("maven.modrinth:power-grid:8EtGIOFr")  // 0.5.5.1
    implementation("maven.modrinth:yungs-better-end-island:1.21.1-NeoForge-3.1.2")
    implementation("maven.modrinth:betterend-neoforge:21.0.33")
    jarJar(implementation("com.github.Fallen-Breath.conditional-mixin:conditional-mixin-neoforge:0.6.4")!!)
    // a stub file with most of the code stripped. needed to compile LavenderStructureOverlayRendererMixin
    // as it uses a private class from lavender and moddevgradle only access transforms minecraft itself
    // at compile time
    // https://github.com/vanutp-forks/lavender/tree/1.21-stripped
    implementation(files("libs/lavender-0.1.15-stub+1.21.jar"))
    compileOnly("dev.emi:emi-neoforge:1.1.24+1.21.1:api")
    runtimeOnly("dev.emi:emi-neoforge:1.1.24+1.21.1")
    runtimeOnly("maven.modrinth:moonlight:CitoQHqE")
    runtimeOnly("maven.modrinth:yacl:3.8.2+1.21.1-neoforge")
    runtimeOnly("maven.modrinth:geckolib:tPkJmim6")  // 4.9.2
    runtimeOnly("maven.modrinth:architectury-api:13.0.11+neoforge")
}

tasks.processResources {
    val expandProps = mapOf("version" to version)
    inputs.properties(expandProps)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(expandProps)
    }
}
