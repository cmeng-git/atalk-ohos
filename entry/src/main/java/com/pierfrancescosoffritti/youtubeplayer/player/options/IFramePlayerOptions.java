package com.pierfrancescosoffritti.youtubeplayer.player.options;

import ohos.utils.zson.ZSONObject;

import androidx.annotation.NonNull;

/**
 * Options used to configure the IFrame Player. All the options are listed here:
 * [IFrame player parameters](https://developers.google.com/youtube/player_parameters#Parameters)
 */
public class IFramePlayerOptions {
    private final ZSONObject playerOptions;

    private IFramePlayerOptions(ZSONObject options) {
        playerOptions = options;
    }

    public static IFramePlayerOptions getDefault() {
        return new Builder().build();
    }

    @Override
    public String toString() {
        return playerOptions.toString();
    }

    public String getOrigin() {
        return playerOptions.getString(Builder.ORIGIN);
    }

    public static class Builder {
        private static final String AUTOPLAY = "autoplay";
        private static final String CONTROLS = "controls";
        private static final String ENABLE_JS_API = "enablejsapi";
        private static final String FS = "fs";
        private static final String ORIGIN = "origin";
        private static final String REL = "rel";
        private static final String SHOW_INFO = "showinfo";
        private static final String IV_LOAD_POLICY = "iv_load_policy";
        private static final String MODEST_BRANDING = "modestbranding";
        private static final String CC_LOAD_POLICY = "cc_load_policy";
        private static final String CC_LANG_PREF = "cc_lang_pref";

        private final ZSONObject builderOptions = new ZSONObject();

        public Builder() {
            addInt(AUTOPLAY, 0);
            addInt(CONTROLS, 0);
            addInt(ENABLE_JS_API, 1);
            addInt(FS, 0);
            addString(ORIGIN, "https://www.youtube.com");
            addInt(REL, 0);
            addInt(SHOW_INFO, 0);
            addInt(IV_LOAD_POLICY, 3);
            addInt(MODEST_BRANDING, 1);
            addInt(CC_LOAD_POLICY, 0);
        }

        public IFramePlayerOptions build() {
            return new IFramePlayerOptions(builderOptions);
        }

        /**
         * Controls whether the web-based UI of the IFrame player is used or not.
         *
         * @param controls If set to 0: web UI is not used. If set to 1: web UI is used.
         */
        public Builder controls(int controls) {
            addInt(CONTROLS, controls);
            return this;
        }

        /**
         * Controls the related videos shown at the end of a video.
         *
         * @param rel If set to 0: related videos will come from the same channel as the video that was just played. If set to 1: related videos will come from multiple channels.
         */
        public Builder rel(int rel) {
            addInt(REL, rel);
            return this;
        }

        /**
         * Controls video annotations.
         *
         * @param ivLoadPolicy if set to 1: the player will show video annotations. If set to 3: they player won't show video annotations.
         */
        public Builder ivLoadPolicy(int ivLoadPolicy) {
            addInt(IV_LOAD_POLICY, ivLoadPolicy);
            return this;
        }

        /**
         * This parameter specifies the default language that the player will use to display captions.
         * If you use this parameter and also set the cc_load_policy parameter to 1, then the player
         * will show captions in the specified language when the player loads.
         * If you do not also set the cc_load_policy parameter, then captions will not display by default,
         * but will display in the specified language if the user opts to turn captions on.
         *
         * @param languageCode ISO 639-1 two-letter language code
         */
        public Builder langPref(String languageCode) {
            addString(CC_LANG_PREF, languageCode);
            return this;
        }


        /**
         * Controls video captions. It doesn't work with automatically generated captions.
         *
         * @param ccLoadPolicy if set to 1: the player will show captions. If set to 0: the player won't show captions.
         */
        public Builder ccLoadPolicy(int ccLoadPolicy) {
            addInt(CC_LOAD_POLICY, ccLoadPolicy);
            return this;
        }

        /**
         * Controls domain as the origin parameter value.
         *
         * @param origin your domain
         */
        public Builder origin(@NonNull String origin) {
            addString(ORIGIN, origin);
            return this;
        }

        private void addString(@NonNull String key, @NonNull String value) {
            builderOptions.put(key, value);
        }

        private void addInt(@NonNull String key, int value) {
            builderOptions.put(key, value);
        }
    }
}
