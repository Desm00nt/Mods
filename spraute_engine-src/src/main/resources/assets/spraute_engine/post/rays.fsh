#version 150
// Light going out from the sun over what stands in its way: only the open sky counts as light.

uniform sampler2D tex;
uniform sampler2D depthTex;
uniform vec2 sunPos;
uniform float useDepth;

in vec2 uv;
out vec4 fragColor;

void main() {
    const int STEPS = 40;
    vec2 dir = (sunPos - uv) / float(STEPS) * 0.85;
    vec2 p = uv + dir * fract(52.9829189 * fract(dot(gl_FragCoord.xy, vec2(0.06711056, 0.00583715))));
    float weight = 1.0;
    vec3 sum = vec3(0.0);
    for (int i = 0; i < STEPS; i++) {
        if (p.x > 0.0 && p.x < 1.0 && p.y > 0.0 && p.y < 1.0) {
            float open = useDepth > 0.5 ? step(0.9999, texture(depthTex, p).r) : 1.0;
            vec3 c = texture(tex, p).rgb;
            float bright = smoothstep(0.55, 0.95, max(c.r, max(c.g, c.b)));
            sum += c * bright * open * weight;
        }
        weight *= 0.965;
        p += dir;
    }
    fragColor = vec4(sum / float(STEPS) * 2.4, 1.0);
}
