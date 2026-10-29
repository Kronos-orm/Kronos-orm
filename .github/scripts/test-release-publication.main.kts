@file:Import("../../scripts/common.main.kts", "../../scripts/release.main.kts")

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger

val workflowRevision = "1".repeat(40)
val checkoutRevision = "2".repeat(40)
check(sourceRevision(mapOf("GITHUB_SHA" to workflowRevision, "KRONOS_SOURCE_SHA" to checkoutRevision)) == checkoutRevision)
check(sourceRevision(mapOf("GITHUB_SHA" to workflowRevision)) == workflowRevision)
check(sourceRevision(mapOf("GITHUB_SHA" to workflowRevision, "KRONOS_SOURCE_SHA" to "")) == workflowRevision)

val directory = Files.createTempDirectory("kronos-central-contract-")
val libraryVersion = versions().getValue("version")
val version = libraryVersion.removeSuffix("-SNAPSHOT")
val payloadPath = "com/kotlinorm/kronos-core/$version/kronos-core-$version.pom"
val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
val uploads = AtomicInteger()
val downloads = AtomicInteger()
val requests = AtomicInteger()
var online = true
var state = "PUBLISHED"
var uploadHttpStatus = 200
var purl = "pkg:maven/com.kotlinorm/kronos-core@$version"
var visible = "payload"
server.createContext("/") { exchange ->
    val path = exchange.requestURI.path
    requests.incrementAndGet()
    if (!online) {
        exchange.sendResponseHeaders(503, -1); exchange.close(); return@createContext
    }
    exchange.requestBody.use { it.readBytes() }
    val body = when {
        path.endsWith("/upload") -> { uploads.incrementAndGet(); "deployment-contract" }
        path.endsWith("/artifact.sha256") -> java.security.MessageDigest.getInstance("SHA-256").digest("download".toByteArray()).joinToString("") { "%02x".format(it) } + "  *artifact\n"
        path.endsWith("/other.sha256") -> java.security.MessageDigest.getInstance("SHA-256").digest("other".toByteArray()).joinToString("") { "%02x".format(it) } + "\tother\n"
        path.endsWith("/other") -> "other"
        path.endsWith("/artifact") -> { downloads.incrementAndGet(); "download" }
        path.endsWith("/status") -> jsonMapper.writeValueAsString(mapOf("deploymentState" to state, "purls" to listOf(purl)))
        path.endsWith(".pom") -> visible
        else -> error("Unexpected Central request: $path")
    }.toByteArray()
    exchange.sendResponseHeaders(if (path.endsWith("/upload")) uploadHttpStatus else 200, body.size.toLong())
    exchange.responseBody.use { it.write(body) }
}
server.start()
val endpoint = "http://127.0.0.1:${server.address.port}"
fun archive(path: Path, signature: String, payload: String = "payload") {
    ZipOutputStream(Files.newOutputStream(path)).use { zip ->
        for ((name, bytes) in mapOf(payloadPath to payload, "$payloadPath.asc" to signature)) {
            zip.putNextEntry(ZipEntry(name)); zip.write(bytes.toByteArray()); zip.closeEntry()
        }
    }
}
fun rejects(block: () -> Unit) { check(runCatching(block).isFailure) { "Unsafe publication unexpectedly succeeded" } }
try {
    val bundle = directory.resolve("bundle.zip")
    val checkpoint = directory.resolve("checkpoint.json")
    archive(bundle, "signature-1")
    publishRelease(bundle, checkpoint, "$endpoint/publisher", {}, endpoint, version = version)
    check(uploads.get() == 1 && jsonMapper.readTree(checkpoint.toFile())["state"].asText() == "PUBLISHED")
    archive(bundle, "signature-2")
    publishRelease(bundle, checkpoint, "$endpoint/publisher", {}, endpoint, version = version)
    check(uploads.get() == 1) { "Resume reuploaded a release after signatures changed" }
    archive(bundle, "signature-3", "different-payload")
    rejects { publishRelease(bundle, checkpoint, "$endpoint/publisher", {}, endpoint, version = version) }
    check(uploads.get() == 1)
    archive(bundle, "signature-4")
    state = "FAILED"
    rejects { publishRelease(bundle, checkpoint, "$endpoint/publisher", {}, endpoint, version = version) }
    check(jsonMapper.readTree(checkpoint.toFile())["state"].asText() == "FAILED" && uploads.get() == 1)
    state = "PUBLISHED"; purl = "pkg:maven/com.kotlinorm/kronos-core@wrong-version"
    rejects { publishRelease(bundle, checkpoint, "$endpoint/publisher", {}, endpoint, version = version) }
    purl = "pkg:maven/com.kotlinorm/kronos-core@$version"; visible = "wrong-remote-bytes"
    rejects { publishRelease(bundle, checkpoint, "$endpoint/publisher", {}, endpoint, 2, version) }
    check(uploads.get() == 1)
    val cached = directory.resolve("cached.jar")
    downloadVerified("$endpoint/artifact", cached)
    check(cached.text() == "download" && downloads.get() == 1)
    val beforeCacheHit = requests.get()
    online = false
    downloadVerified("$endpoint/artifact", cached)
    check(requests.get() == beforeCacheHit && downloads.get() == 1) { "Valid cache required a network request" }
    online = true
    cached.write("corrupted-cache")
    downloadVerified("$endpoint/artifact", cached)
    check(cached.text() == "download" && downloads.get() == 2) { "Corrupt cache was reused" }
    val direct = directory.resolve("without-checksum.jar")
    downloadVerified("$endpoint/artifact", direct, checksumUrl = null)
    val beforeOffline = requests.get(); online = false
    downloadVerified("$endpoint/artifact", direct, checksumUrl = null)
    check(requests.get() == beforeOffline) { "An HTTPS-only cached resource required network access" }
    online = true
    direct.write("corrupted-resource")
    downloadVerified("$endpoint/artifact", direct, checksumUrl = null)
    check(direct.text() == "download")
    val distribution = directory.resolve("distribution")
    var extractions = 0
    fun extract() { extractions++; distribution.resolve("sdk.jar").write("sdk-content") }
    extractVerifiedArchive(cached, distribution, ::extract)
    extractVerifiedArchive(cached, distribution, ::extract)
    check(extractions == 1)
    distribution.resolve("sdk.jar").write("corrupted-sdk")
    extractVerifiedArchive(cached, distribution, ::extract)
    check(extractions == 2 && distribution.resolve("sdk.jar").text() == "sdk-content")
    downloadVerified("$endpoint/other", cached)
    check(cached.text() == "other") { "Cache from a different URL was reused" }
    val concurrent = directory.resolve("concurrent.jar")
    val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
    try {
        val before = downloads.get()
        val futures = (1..2).map { executor.submit<Path> { downloadVerified("$endpoint/artifact", concurrent) } }
        futures.forEach { it.get() }
        check(downloads.get() == before + 1) { "Concurrent downloads raced instead of sharing one verified result" }
    } finally { executor.shutdownNow() }
    println("Verified download contracts passed: filename digests, offline hit, URL binding, concurrency, download and extracted SDK corruption repair")
    val candidate = directory.resolve("candidate")
    val evidence = directory.resolve("resolved"); Files.createDirectories(evidence)
    val payloads = listOf("kronos-core-jvm", "kronos-syntax-jvm").map { artifact ->
        candidate.resolve("com/kotlinorm/$artifact/$libraryVersion/$artifact-$libraryVersion.jar").also { it.write(artifact) }
    }
    val nativePayloads = (publicationGroups.values.map { it.first() } + "macosX64").distinct().flatMap { target -> listOf("kronos-core", "kronos-syntax").map { module ->
        val artifact = "$module-${target.lowercase()}"
        candidate.resolve("com/kotlinorm/$artifact/$libraryVersion/$artifact-$libraryVersion.klib").also { it.write(artifact) }
    } }
    val apkPayloads = listOf("app.apk", "test.apk").map { name -> candidate.resolve(name).also { it.write(name) } }
    candidateManifest(candidate, "jvm")
    val resolvedPayloads = payloads.map { path ->
        val cached = directory.resolve("consumer-cache/${path.parent.parent.fileName}/$libraryVersion/${path.fileName}")
        Files.createDirectories(cached.parent); Files.copy(path, cached); cached
    }
    val records = evidence.resolve("gradle.jsonl")
    fun writeRecords(files: List<Path>, selectedVersion: String = libraryVersion) = records.write(files.joinToString("\n") { path ->
        jsonMapper.writeValueAsString(mapOf("path" to path.toString(), "sha256" to digest(path), "group" to "com.kotlinorm",
            "artifact" to path.parent.parent.fileName.toString(), "version" to selectedVersion, "configuration" to "contract"))
    } + "\n")
    writeRecords(resolvedPayloads)
    consumerReceipt(candidate, directory.resolve("receipt.json"), "server", "jvm", evidence)
    writeRecords(resolvedPayloads, "wrong-version")
    rejects { consumerReceipt(candidate, directory.resolve("receipt.json"), "server", "jvm", evidence) }
    writeRecords(resolvedPayloads.take(1))
    rejects { consumerReceipt(candidate, directory.resolve("receipt.json"), "server", "jvm", evidence) }
    writeRecords(resolvedPayloads)
    rejects { validateConsumedPayloads(setOf("com/kotlinorm/kronos-core-jvm/1/core.jar", "com/kotlinorm/kronos-syntax-jvm/1/syntax.jar",
        "com/kotlinorm/kronos-logging-linuxx64/1/logging.klib"), "native", "sqlite-linux") }
    rejects { validateConsumedPayloads(setOf("com/kotlinorm/kronos-core-lookalike-jvm/1/core.jar", "com/kotlinorm/kronos-syntax-jvm/1/syntax.jar"), "jvm", "server") }
    rejects { validateConsumedPayloads(setOf("app.apk"), "apk", "android-api-26") }
    rejects { validateConsumedPayloads(setOf("com/kotlinorm/kronos-core-macosarm64/1/core.klib",
        "com/kotlinorm/kronos-syntax-macosarm64/1/syntax.klib"), "native", "sqlite-macos-intel") }
    validateConsumedPayloads(setOf("com/kotlinorm/kronos-core-macosx64/1/core.klib",
        "com/kotlinorm/kronos-syntax-macosx64/1/syntax.klib"), "native", "sqlite-macos-intel")
    writeRecords(nativePayloads)
    consumerReceipt(candidate, directory.resolve("native-receipt.json"), "sqlite-linux", "native", evidence)
    records.write(apkPayloads.joinToString("\n") { path -> jsonMapper.writeValueAsString(mapOf("path" to path.toString(), "sha256" to digest(path), "configuration" to "apk")) } + "\n")
    consumerReceipt(candidate, directory.resolve("apk-receipt.json"), "android-api-26", "apk", evidence)
    val receipts = directory.resolve("receipts"); Files.createDirectories(receipts)
    val expected = requiredConsumers
    for ((name, family) in expected) {
        val origin = directory.resolve(when(family) { "native" -> "native-receipt.json"; "apk" -> "apk-receipt.json"; else -> "receipt.json" })
        val receipt = jsonMapper.readTree(origin.toFile()) as com.fasterxml.jackson.databind.node.ObjectNode
        receipt.put("consumer", name); receipts.resolve("$name.json").json(receipt)
    }
    val manifest = candidate.resolve("ci-candidate.json")
    verifyConsumers(manifest, manifest, receipts)
    val incorrect = jsonMapper.readTree(receipts.resolve("server.json").toFile()) as com.fasterxml.jackson.databind.node.ObjectNode
    incorrect.put("family", "apk"); receipts.resolve("server.json").json(incorrect)
    rejects { verifyConsumers(manifest, manifest, receipts) }
    Files.delete(receipts.resolve("server.json"))
    rejects { verifyConsumers(manifest, manifest, receipts) }
    writeRecords(resolvedPayloads)
    resolvedPayloads.first().write("modified-cached-artifact")
    rejects { consumerReceipt(candidate, directory.resolve("receipt.json"), "server", "jvm", evidence) }
    println("Consumer resolution contracts passed: actual hashes, wrong version, missing runtime, changed cached artifact")
    uploadHttpStatus = 500
    val uncertain = directory.resolve("uncertain.json")
    rejects { publishRelease(bundle, uncertain, "$endpoint/publisher", {}, endpoint, version = version) }
    check(jsonMapper.readTree(uncertain.toFile())["state"].asText() == "UPLOAD_OUTCOME_UNKNOWN")
    val attempted = uploads.get()
    rejects { publishRelease(bundle, uncertain, "$endpoint/publisher", {}, endpoint, version = version) }
    check(uploads.get() == attempted) { "An ambiguous upload was retried without recovering its deployment ID" }
    println("Central publication contracts passed: upload, resume, re-sign, changed payload, failed deployment, wrong version, public bytes")
} finally { server.stop(0); deleteTree(directory) }
