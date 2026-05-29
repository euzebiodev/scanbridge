package br.local.scanbridge.scanner;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "scanbridge.scanner")
public class ScanProperties {

    private Path outputDirectory = Path.of("data", "scans");
    private Path scriptPath = Path.of("scripts", "scan-wia.ps1");
    private Path saneScriptPath = Path.of("scripts", "scan-sane.sh");
    private String deviceName = "";
    private int dpi = 300;
    private String colorMode = "color";
    private int timeoutSeconds = 120;

    public Path getOutputDirectory() {
        return outputDirectory;
    }

    public void setOutputDirectory(Path outputDirectory) {
        this.outputDirectory = outputDirectory;
    }

    public Path getScriptPath() {
        return scriptPath;
    }

    public void setScriptPath(Path scriptPath) {
        this.scriptPath = scriptPath;
    }

    public Path getSaneScriptPath() {
        return saneScriptPath;
    }

    public void setSaneScriptPath(Path saneScriptPath) {
        this.saneScriptPath = saneScriptPath;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public int getDpi() {
        return dpi;
    }

    public void setDpi(int dpi) {
        this.dpi = dpi;
    }

    public String getColorMode() {
        return colorMode;
    }

    public void setColorMode(String colorMode) {
        this.colorMode = colorMode;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds;
    }
}
