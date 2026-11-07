#version 150
// One step down the pyramid (dual filter). threshold > 0 keeps only what is bright.

uniform sampler2D tex;
uniform vec2 texel;
uniform float threshold;

in vec2 uv;
out vec4 fragColor;

void main() {
    vec3 c = texture(tex, uv).rgb * 4.0;
    c += texture(tex, uv + vec2(-texel.x, -texel.y)).rgb;
    c += texture(tex, uv + vec2(texel.x, texel.y)).rgb;
    c += texture(tex, uv + vec2(texel.x, -texel.y)).rgb;
    c += texture(tex, uv + vec2(-texel.x, texel.y)).rgb;
    c /= 8.0;
    if (threshold > 0.0) {
        float l = max(c.r, max(c.g, c.b));
        float knee = max(threshold * 0.5, 0.001);
        float soft = clamp(l - threshold + knee, 0.0, 2.0 * knee);
        soft = soft * soft / (4.0 * knee + 0.0001);
        c *= max(soft, l - threshold) / max(l, 0.0001);
    }
    fragColor = vec4(c, 1.0);
}
