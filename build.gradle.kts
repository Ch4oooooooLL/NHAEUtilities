
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

tasks.withType<Jar>().configureEach {
    manifest {
        attributes(
            "MixinConfigs" to "mixins.nhaeutilities.json,mixins.nhaeutilities.superwirelesskit.json"
        )
    }
}
