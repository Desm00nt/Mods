#version 150
// Shade in corners and joints, from depth alone. White — open, dark — hidden.

uniform sampler2D depthTex;
uniform float nearPlane;
uniform float farPlane;
uniform float radius;
uniform float aspect;
uniform float tanFov;

in vec2 uv;
out vec4 fragColor;

float linear(vec2 p) {
    float z = texture(depthTex, p).r * 2.0 - 1.0;
    return 2.0 * nearPlane * farPlane / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

float noise(vec2 p) {
    return fract(52.9829189 * fract(dot(p, vec2(0.06711056, 0.00583715))));
}

void main() {
    float d = linear(uv);
    if (d > farPlane * 0.9) {
        fragColor = vec4(1.0);
        return;
    }
    // a world radius seen at this distance, on the screen
    float screenR = radius / (d * tanFov * 2.0);
    screenR = clamp(screenR, 0.002, 0.12);
    float angle = noise(gl_FragCoord.xy) * 6.2831853;
    const int STEPS = 12;
    float hidden = 0.0;
    for (int i = 0; i < STEPS; i++) {
        float k = (float(i) + 0.5) / float(STEPS);
        float a = angle + float(i) * 2.399963;
        vec2 p = uv + vec2(cos(a) / aspect, sin(a)) * screenR * sqrt(k);
        float s = linear(p);
        float diff = d - s;
        // nearer than here, but not a whole object in front
        hidden += clamp(diff / radius, 0.0, 1.0) * (1.0 - smoothstep(radius, radius * 3.0, diff));
    }
    fragColor = vec4(vec3(1.0 - clamp(hidden / float(STEPS) * 2.2, 0.0, 1.0)), 1.0);
}
