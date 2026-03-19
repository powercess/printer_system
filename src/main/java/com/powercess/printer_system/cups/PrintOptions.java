package com.powercess.printer_system.cups;

import java.util.Map;

/**
 * 打印选项配置
 */
public record PrintOptions(
    Integer copies,
    String duplex,
    String media,
    String colorModel,
    String resolution,
    Map<String, String> additionalOptions
) {
    public static final String DUPLEX_ONE_SIDED = "one-sided";
    public static final String DUPLEX_TWO_SIDED_LONG_EDGE = "two-sided-long-edge";
    public static final String DUPLEX_TWO_SIDED_SHORT_EDGE = "two-sided-short-edge";

    public static final String MEDIA_A4 = "A4";
    public static final String MEDIA_A3 = "A3";
    public static final String MEDIA_LETTER = "Letter";

    public static final String COLOR_GRAY = "Gray";
    public static final String COLOR_RGB = "RGB";

    public PrintOptions {
        copies = copies != null ? copies : 1;
        duplex = duplex != null ? duplex : DUPLEX_ONE_SIDED;
        media = media != null ? media : MEDIA_A4;
        colorModel = colorModel != null ? colorModel : COLOR_GRAY;
        additionalOptions = additionalOptions != null ? additionalOptions : Map.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isDuplex() {
        return DUPLEX_TWO_SIDED_LONG_EDGE.equals(duplex) || DUPLEX_TWO_SIDED_SHORT_EDGE.equals(duplex);
    }

    public boolean isColor() {
        return COLOR_RGB.equals(colorModel);
    }

    public static class Builder {
        private Integer copies;
        private String duplex;
        private String media;
        private String colorModel;
        private String resolution;
        private Map<String, String> additionalOptions;

        public Builder copies(Integer copies) {
            this.copies = copies;
            return this;
        }

        public Builder duplex(String duplex) {
            this.duplex = duplex;
            return this;
        }

        public Builder media(String media) {
            this.media = media;
            return this;
        }

        public Builder colorModel(String colorModel) {
            this.colorModel = colorModel;
            return this;
        }

        public Builder resolution(String resolution) {
            this.resolution = resolution;
            return this;
        }

        public Builder additionalOptions(Map<String, String> additionalOptions) {
            this.additionalOptions = additionalOptions;
            return this;
        }

        public PrintOptions build() {
            return new PrintOptions(copies, duplex, media, colorModel, resolution, additionalOptions);
        }
    }
}