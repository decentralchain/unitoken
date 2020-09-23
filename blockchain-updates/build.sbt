import unitokenDockerKeys._

name := "blockchain-updates"

libraryDependencies ++= Dependencies.kafka +: Dependencies.protobuf.value

extensionClasses += "com.decentralchain.events.BlockchainUpdates"

inConfig(Compile)(
  Seq(
    PB.protoSources in Compile := Seq(PB.externalIncludePath.value),
    includeFilter in PB.generate := new SimpleFileFilter((f: File) => f.getName.endsWith(".proto") && f.getParent.replace('\\', '/').endsWith("unitoken/events")),
    PB.targets += scalapb.gen(flatPackage = true) -> sourceManaged.value
  ))

enablePlugins(RunApplicationSettings, unitokenExtensionDockerPlugin, ExtensionPackaging)

docker := docker.dependsOn(LocalProject("node-it") / docker).value
inTask(docker)(
  Seq(
    imageNames := Seq(ImageName("com.decentralchain/blockchain-updates")),
    exposedPorts := Set(6886),
    additionalFiles ++= Seq(
      (LocalProject("blockchain-updates") / Universal / stage).value
    )
  ))
