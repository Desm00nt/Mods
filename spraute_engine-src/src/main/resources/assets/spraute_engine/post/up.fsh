#version 150
// One step up the pyramid (dual filter): the wider level laid over the sharper one.

uniform sampler2D tex;
uniform sampler2D over;
uniform vec2 texel;
uniform float radius;
uniform float additive;

in vec2 uv;
out vec4 fragColor;

void main() {
    vec2 t = texel * radius;
    vec3 c = texture(tex, uv + vec2(-t.x * 2.0, 0.0)).rgb;
    c += texture(tex, uv + vec2(-t.x, t.y)).rgb * 2.0;
    c += texture(tex, uv + vec2(0.0, t.y * 2.0)).rgb;
    c += texture(tex, uv + vec2(t.x, t.y)).rgb * 2.0;
    c += texture(tex, uv + vec2(t.x * 2.0, 0.0)).rgb;
    c += texture(tex, uv + vec2(t.x, -t.y)).rgb * 2.0;
    c += texture(tex, uv + vec2(0.0, -t.y * 2.0)).rgb;
    c += texture(tex, uv + vec2(-t.x, -t.y)).rgb * 2.0;
    c /= 12.0;
    fragColor = vec4(additive > 0.5 ? (c + texture(over, uv).rgb) * 0.5 : c, 1.0);
}
