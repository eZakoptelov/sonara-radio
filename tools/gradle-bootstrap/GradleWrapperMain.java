package org.gradle.wrapper;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Small bootstrap launcher used by this project to fetch the configured Gradle distribution. */
public final class GradleWrapperMain {
    private GradleWrapperMain() {}

    public static void main(String[] args) throws Exception {
        Path projectDir = Paths.get(System.getProperty("sonora.wrapper.home", System.getProperty("user.dir"))).toAbsolutePath().normalize();
        Path propertiesFile = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (!Files.isRegularFile(propertiesFile)) {
            Path wrapperJar = Paths.get(GradleWrapperMain.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path wrapperDirectory = wrapperJar.getParent();
            if (wrapperDirectory != null && wrapperDirectory.getParent() != null && wrapperDirectory.getParent().getParent() != null) {
                projectDir = wrapperDirectory.getParent().getParent();
                propertiesFile = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties");
            }
        }
        if (!Files.isRegularFile(propertiesFile)) {
            throw new IllegalStateException("Cannot find gradle/wrapper/gradle-wrapper.properties for this project");
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(propertiesFile)) {
            properties.load(input);
        }

        String distributionUrl = properties.getProperty("distributionUrl");
        if (distributionUrl == null || distributionUrl.isBlank()) {
            throw new IllegalStateException("distributionUrl is missing in gradle-wrapper.properties");
        }

        URI uri = URI.create(distributionUrl);
        String zipName = Paths.get(uri.getPath()).getFileName().toString();
        String distributionName = zipName.endsWith(".zip") ? zipName.substring(0, zipName.length() - 4) : zipName;
        String userHome = System.getenv("GRADLE_USER_HOME");
        Path gradleHome = userHome == null || userHome.isBlank()
                ? Paths.get(System.getProperty("user.home"), ".gradle")
                : Paths.get(userHome);
        Path distributionDir = gradleHome.resolve("wrapper/dists").resolve(distributionName).resolve("sonora-bootstrap");
        Path installDir = distributionDir.resolve(distributionName.replaceAll("-(bin|all)$", ""));
        Path gradleExecutable = executablePath(installDir);

        if (!Files.isRegularFile(gradleExecutable)) {
            Files.createDirectories(distributionDir);
            Path archive = distributionDir.resolve(zipName + ".part");
            System.out.println("Downloading Gradle distribution: " + distributionUrl);
            download(uri.toURL().toString(), archive);

            String expectedSha = properties.getProperty("distributionSha256Sum");
            if (expectedSha != null && !expectedSha.isBlank()) {
                String actualSha = sha256(archive);
                if (!actualSha.equalsIgnoreCase(expectedSha.trim())) {
                    Files.deleteIfExists(archive);
                    throw new IllegalStateException("Gradle distribution checksum mismatch. Expected " + expectedSha + " but received " + actualSha);
                }
            }

            unzipSafely(archive, distributionDir);
            Files.deleteIfExists(archive);
            gradleExecutable = executablePath(installDir);
            if (!Files.isRegularFile(gradleExecutable)) {
                throw new IllegalStateException("Gradle executable was not found after extracting " + zipName);
            }
            if (!isWindows()) {
                gradleExecutable.toFile().setExecutable(true, false);
            }
        }

        List<String> command = new ArrayList<>();
        if (isWindows()) {
            StringBuilder windowsCommand = new StringBuilder(quoteWindows(gradleExecutable.toString()));
            for (String arg : args) windowsCommand.append(' ').append(quoteWindows(arg));
            command.add("cmd.exe");
            command.add("/c");
            command.add(windowsCommand.toString());
        } else {
            command.add(gradleExecutable.toString());
            for (String arg : args) command.add(arg);
        }

        Process process = new ProcessBuilder(command)
                .directory(projectDir.toFile())
                .inheritIO()
                .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) System.exit(exitCode);
    }


    private static String quoteWindows(String value) {
        if (value.indexOf(' ') < 0 && value.indexOf('\t') < 0 && value.indexOf('"') < 0) return value;
        return "\"" + value.replace("\"", "\\\"") + "\"";
    }

    private static Path executablePath(Path installDir) {
        return installDir.resolve("bin").resolve(isWindows() ? "gradle.bat" : "gradle");
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void download(String url, Path destination) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(30_000);
        connection.setReadTimeout(180_000);
        connection.setRequestProperty("User-Agent", "SONORA-Gradle-Bootstrap/1.0");
        try {
            int status = connection.getResponseCode();
            if (status < 200 || status >= 300) throw new IllegalStateException("Download failed with HTTP " + status);
            try (InputStream input = connection.getInputStream(); OutputStream output = Files.newOutputStream(destination)) {
                byte[] buffer = new byte[64 * 1024];
                int read;
                while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream input = Files.newInputStream(file)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) digest.update(buffer, 0, read);
        }
        StringBuilder result = new StringBuilder();
        for (byte value : digest.digest()) result.append(String.format("%02x", value));
        return result.toString();
    }

    private static void unzipSafely(Path archive, Path destination) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(Files.newInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                Path target = destination.resolve(entry.getName()).normalize();
                if (!target.startsWith(destination.normalize())) throw new IllegalStateException("Unsafe entry in Gradle archive");
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) Files.createDirectories(parent);
                    try (OutputStream output = Files.newOutputStream(target)) {
                        byte[] buffer = new byte[64 * 1024];
                        int read;
                        while ((read = zip.read(buffer)) != -1) output.write(buffer, 0, read);
                    }
                }
                zip.closeEntry();
            }
        }
    }
}
