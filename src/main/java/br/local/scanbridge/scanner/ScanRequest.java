package br.local.scanbridge.scanner;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class ScanRequest {

    @Min(75)
    @Max(600)
    private int dpi = 300;

    @Pattern(regexp = "color|grayscale|blackwhite")
    private String colorMode = "color";

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
}
