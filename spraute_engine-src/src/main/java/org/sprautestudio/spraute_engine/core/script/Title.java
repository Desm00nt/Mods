package org.sprautestudio.spraute_engine.core.script;

public record Title(String text, String subtitle, int fadeIn, int stay, int fadeOut) {
    public static Title.Builder builder(String text) {
        return new Title.Builder(text);
    }

    public static final class Builder {
        private final String text;
        private String subtitle = "";
        private int fadeIn = 10;
        private int stay = 70;
        private int fadeOut = 20;

        private Builder(String text) {
            this.text = text;
        }

        public Title.Builder subtitle(String s) {
            this.subtitle = s;
            return this;
        }

        public Title.Builder times(int in, int hold, int out) {
            this.fadeIn = in;
            this.stay = hold;
            this.fadeOut = out;
            return this;
        }

        public Title build() {
            return new Title(this.text, this.subtitle, this.fadeIn, this.stay, this.fadeOut);
        }
    }
}
