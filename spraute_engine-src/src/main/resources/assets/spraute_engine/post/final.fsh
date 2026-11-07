#version 150
// The last pass: everything comes together here. The "uniform float u_..." of every setting are added by PostEngine,
// from the table of settings, in front of this text.

uniform sampler2D sceneTex;
uniform sampler2D depthTex;
uniform sampler2D bloomTex;
uniform sampler2D blurTex;
uniform sampler2D aoTex;
uniform sampler2D raysTex;
uniform sampler2D historyTex;
uniform vec2 screenSize;
uniform float time;
uniform float nearPlane;
uniform float farPlane;
uniform float focusAuto;
uniform float useDepth;
uniform float hasBloom;
uniform float hasBlur;
uniform float hasAo;
uniform float hasRays;
uniform float hasHistory;
uniform vec2 sunPos;
uniform float sunVisible;

in vec2 uv;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

float vnoise(vec2 p) {
    vec2 i = floor(p), f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1, 0)), f.x), mix(hash(i + vec2(0, 1)), hash(i + vec2(1, 1)), f.x), f.y);
}

float linearDepth(vec2 p) {
    float z = texture(depthTex, p).r * 2.0 - 1.0;
    return 2.0 * nearPlane * farPlane / (farPlane + nearPlane - z * (farPlane - nearPlane));
}

vec3 hueShift(vec3 c, float deg) {
    float a = radians(deg), co = cos(a), si = sin(a);
    vec3 k = vec3(0.57735);
    return c * co + cross(k, c) * si + k * dot(k, c) * (1.0 - co);
}

vec3 aces(vec3 x) {
    return clamp((x * (2.51 * x + 0.03)) / (x * (2.43 * x + 0.59) + 0.14), 0.0, 1.0);
}

vec3 heat(float t) {
    vec3 a = mix(vec3(0.02, 0.0, 0.25), vec3(0.6, 0.0, 0.7), smoothstep(0.0, 0.35, t));
    a = mix(a, vec3(1.0, 0.35, 0.0), smoothstep(0.3, 0.65, t));
    return mix(a, vec3(1.0, 1.0, 0.75), smoothstep(0.65, 1.0, t));
}

/** Where on the picture this pixel takes its colour from: all that bends the picture. */
vec2 warp(vec2 p) {
    vec2 c = p - 0.5;
    if (u_crt > 0.0) {
        float k = 0.22 * u_crt;
        c *= 1.0 + k * dot(c, c) * 2.0;
    }
    if (u_lens_distort != 0.0) c *= 1.0 + u_lens_distort * dot(c, c) * 1.4;
    if (u_swirl != 0.0) {
        float d = length(c * vec2(screenSize.x / screenSize.y, 1.0));
        float a = u_swirl * smoothstep(0.7, 0.0, d);
        float co = cos(a), si = sin(a);
        c = mat2(co, -si, si, co) * c;
    }
    p = c + 0.5;
    if (u_wave > 0.0) {
        float t = time * u_wave_speed;
        p += vec2(sin(p.y * 9.0 + t * 1.3), cos(p.x * 7.0 + t * 1.7)) * 0.018 * u_wave;
    }
    if (u_underwater > 0.0) {
        float t = time * 1.4;
        p += vec2(sin(p.y * 42.0 + t) + sin(p.y * 17.0 - t * 0.7), cos(p.x * 36.0 + t * 1.2)) * 0.0032 * u_underwater;
    }
    if (u_heat_haze > 0.0) {
        float k = u_heat_haze * smoothstep(0.1, 1.0, 1.0 - p.y + 0.35);
        p.x += sin(p.y * 120.0 + time * 7.0) * 0.0016 * k + (vnoise(vec2(p.x * 60.0, p.y * 80.0 - time * 3.5)) - 0.5) * 0.007 * k;
    }
    if (u_drunk > 0.0) {
        float t = time * 0.9;
        vec2 sway = vec2(sin(t) + 0.5 * sin(t * 2.3), cos(t * 0.7)) * 0.022 * u_drunk;
        float a = sin(t * 0.6) * 0.035 * u_drunk;
        vec2 q = p - 0.5;
        p = mat2(cos(a), -sin(a), sin(a), cos(a)) * q + 0.5 + sway;
    }
    if (u_vhs > 0.0) {
        float line = sin(p.y * 240.0 + time * 13.0) * 0.0007;
        float tear = smoothstep(0.985, 1.0, sin(p.y * 3.0 - time * 1.3)) * 0.012;
        p.x += (line + tear * vnoise(vec2(p.y * 200.0, time * 9.0))) * u_vhs;
    }
    if (u_glitch > 0.0) {
        float slot = floor(time * 11.0);
        float band = floor(p.y * 14.0 + hash(vec2(slot, 3.0)) * 14.0);
        if (hash(vec2(band, slot)) > 1.0 - u_glitch * 0.45) p.x += (hash(vec2(band * 3.1, slot)) - 0.5) * 0.14 * u_glitch;
    }
    if (u_rain_lens > 0.0) {
        vec2 g = vec2(p.x * 26.0, p.y * 11.0 + time * 0.55);
        vec2 id = floor(g), f = fract(g) - 0.5;
        float r = hash(id);
        if (r < 0.32 * u_rain_lens + 0.03) {
            vec2 o = f - vec2(hash(id + 3.0) - 0.5, hash(id + 7.0) - 0.5) * 0.5;
            float drop = smoothstep(0.22, 0.04, length(o * vec2(1.0, 2.0)));
            p -= o * drop * 0.06;
        }
    }
    if (u_pixelate >= 1.0) {
        vec2 cells = screenSize / u_pixelate;
        p = (floor(p * cells) + 0.5) / cells;
    }
    return p;
}

vec3 readScene(vec2 p) {
    return texture(sceneTex, clamp(p, 0.0, 1.0)).rgb;
}

/** The picture with its channels moved apart. */
vec3 split(vec2 p) {
    float amount = u_chromatic * 0.006 + u_vhs * 0.003 + u_glitch * 0.003;
    if (amount <= 0.0) return readScene(p);
    vec2 d = (p - 0.5) * (1.0 + dot(p - 0.5, p - 0.5) * 3.0) * amount * 2.0;
    return vec3(readScene(p + d).r, readScene(p).g, readScene(p - d).b);
}

void main() {
    vec2 p = warp(uv);
    vec3 c = split(p);

    // the second, moved image of drunkenness
    if (u_drunk > 0.0) {
        vec2 off = vec2(sin(time * 0.8), cos(time * 0.6)) * 0.014 * u_drunk;
        c = mix(c, readScene(p + off), 0.4 * u_drunk);
    }
    if (u_radial_blur > 0.0) {
        vec3 sum = c;
        vec2 dir = (p - 0.5) * u_radial_blur * 0.16;
        for (int i = 1; i < 12; i++) sum += readScene(p - dir * (float(i) / 12.0));
        c = sum / 12.0;
    }
    if (u_sharpen > 0.0) {
        vec2 t = 1.0 / screenSize;
        vec3 around = readScene(p + vec2(t.x, 0.0)) + readScene(p - vec2(t.x, 0.0)) + readScene(p + vec2(0.0, t.y)) + readScene(p - vec2(0.0, t.y));
        c = max(c + (c - around * 0.25) * u_sharpen, 0.0);
    }

    float d = useDepth > 0.5 ? linearDepth(p) : 0.0;
    bool sky = useDepth > 0.5 && texture(depthTex, p).r >= 0.99999;

    if (hasBlur > 0.5) {
        vec3 b = texture(blurTex, p).rgb;
        float amount = clamp(u_blur * 1.6, 0.0, 1.0);
        if (u_dof > 0.0 && useDepth > 0.5) {
            float focus = u_dof_focus > 0.0 ? u_dof_focus : focusAuto;
            amount = max(amount, smoothstep(u_dof_range * 0.5, u_dof_range * 2.0, abs(d - focus)) * u_dof);
        }
        if (u_tilt_shift > 0.0) amount = max(amount, smoothstep(0.12, 0.42, abs(p.y - 0.5)) * u_tilt_shift);
        c = mix(c, b, amount);
    }

    if (hasAo > 0.5 && !sky) c *= mix(1.0, texture(aoTex, p).r, clamp(u_ao, 0.0, 1.0));
    if (hasAo > 0.5 && !sky && u_ao > 1.0) c *= mix(1.0, texture(aoTex, p).r, clamp(u_ao - 1.0, 0.0, 2.0));

    if (u_fog > 0.0 && useDepth > 0.5) {
        float f = smoothstep(u_fog_start, max(u_fog_end, u_fog_start + 0.1), d);
        c = mix(c, u_fog_color, f * u_fog * (sky ? 0.92 : 1.0));
    }

    if (hasBloom > 0.5) c += texture(bloomTex, p).rgb * u_bloom * u_bloom_color;
    if (hasRays > 0.5) c += texture(raysTex, p).rgb * u_god_rays * u_god_rays_color;
    if (u_lens_flare > 0.0 && sunVisible > 0.0) {
        vec2 aspect = vec2(screenSize.x / screenSize.y, 1.0);
        vec2 s = sunPos, q = (uv - 0.5) * aspect, sq = (s - 0.5) * aspect;
        float glow = pow(max(0.0, 1.0 - length(q - sq) * 1.3), 4.0);
        float ghost = smoothstep(0.1, 0.0, length(q + sq * 0.6)) * 0.25 + smoothstep(0.05, 0.0, length(q + sq * 1.2)) * 0.3 + smoothstep(0.15, 0.0, length(q - sq * 0.4)) * 0.12;
        c += (vec3(1.0, 0.85, 0.65) * glow + vec3(0.6, 0.75, 1.0) * ghost) * u_lens_flare * sunVisible;
    }
    if (u_underwater > 0.0) {
        vec2 g = p * 24.0;
        float caustic = pow(abs(sin(g.x + sin(g.y + time) + time * 0.6) * sin(g.y + sin(g.x * 1.3 - time))), 6.0);
        c += vec3(0.25, 0.5, 0.65) * caustic * 0.45 * u_underwater;
    }

    // colour
    if (u_exposure != 0.0) c *= pow(2.0, u_exposure);
    if (u_tonemap > 0.0) c = mix(c, aces(c * 0.85), u_tonemap);
    c = mix(c, c * vec3(1.0 + u_temperature * 0.3, 1.0 - u_tint * 0.22, 1.0 - u_temperature * 0.3 + u_tint * 0.05), 1.0);
    if (u_hue != 0.0) c = hueShift(c, u_hue);
    float l = dot(c, LUMA);
    if (u_vibrance != 0.0) {
        float sat = max(c.r, max(c.g, c.b)) - min(c.r, min(c.g, c.b));
        c = mix(vec3(l), c, 1.0 + u_vibrance * (1.0 - sat));
    }
    c = mix(vec3(l), c, u_saturation);
    c = (c - 0.5) * u_contrast + 0.5;
    c = max(c, 0.0);
    if (u_gamma != 1.0) c = pow(c, vec3(1.0 / u_gamma));
    if (u_shadow_amount > 0.0) c = mix(c, c + u_shadow_color * 0.55, u_shadow_amount * (1.0 - smoothstep(0.0, 0.55, l)));
    if (u_highlight_amount > 0.0) c = mix(c, c * mix(vec3(1.0), u_highlight_color * 1.5, smoothstep(0.45, 1.0, l)), u_highlight_amount);
    if (u_color_amount > 0.0) c = mix(c, c * u_color * 1.2, u_color_amount);
    l = dot(c, LUMA);
    if (u_grayscale > 0.0) c = mix(c, vec3(l), u_grayscale);
    if (u_sepia > 0.0) c = mix(c, vec3(dot(c, vec3(0.393, 0.769, 0.189)), dot(c, vec3(0.349, 0.686, 0.168)), dot(c, vec3(0.272, 0.534, 0.131))), u_sepia);
    if (u_invert > 0.0) c = mix(c, 1.0 - c, u_invert);
    if (u_night_vision > 0.0) c = mix(c, vec3(0.1, 1.0, 0.25) * pow(l * 2.6, 0.75), u_night_vision);
    if (u_thermal > 0.0) c = mix(c, heat(clamp(l * 1.15, 0.0, 1.0)), u_thermal);

    // style
    if (u_outline > 0.0 || u_sketch > 0.0) {
        vec2 t = 1.0 / screenSize;
        float edge;
        if (useDepth > 0.5) {
            float dd = abs(linearDepth(p + vec2(t.x, 0.0)) - d) + abs(linearDepth(p - vec2(t.x, 0.0)) - d) + abs(linearDepth(p + vec2(0.0, t.y)) - d) + abs(linearDepth(p - vec2(0.0, t.y)) - d);
            edge = smoothstep(0.06, 0.4, dd / max(d, 1.0) * 4.0);
        } else edge = 0.0;
        float lx = dot(readScene(p + vec2(t.x, 0.0)) - readScene(p - vec2(t.x, 0.0)), LUMA);
        float ly = dot(readScene(p + vec2(0.0, t.y)) - readScene(p - vec2(0.0, t.y)), LUMA);
        float colourEdge = smoothstep(0.08, 0.3, length(vec2(lx, ly)));
        if (u_outline > 0.0) c = mix(c, u_outline_color, max(edge, colourEdge * 0.7) * u_outline);
        if (u_sketch > 0.0) {
            float paper = 0.93 + vnoise(uv * screenSize * 0.5) * 0.06;
            float hatch = step(fract((uv.x + uv.y) * screenSize.y * 0.18), 0.5) * (1.0 - smoothstep(0.1, 0.55, l));
            vec3 drawn = vec3(paper) * (1.0 - max(edge, colourEdge) * 0.85) * (1.0 - hatch * 0.35);
            c = mix(c, drawn, u_sketch);
        }
    }
    if (u_halftone > 0.0) {
        vec2 cell = fract(uv * screenSize / 5.0) - 0.5;
        float dot_ = smoothstep(0.02, -0.02, length(cell) - sqrt(1.0 - clamp(l, 0.0, 1.0)) * 0.62);
        c = mix(c, c * (1.0 - dot_ * 0.75), u_halftone);
    }
    if (u_posterize >= 2.0) c = floor(c * u_posterize + 0.5) / u_posterize;
    if (u_dither > 0.0) {
        vec2 f = mod(floor(gl_FragCoord.xy), 4.0);
        int x = int(f.x), y = int(f.y);
        float bayer = (float((x ^ y) & 1) * 8.0 + float(y & 1) * 4.0 + float(((x ^ y) >> 1) & 1) * 2.0 + float((y >> 1) & 1)) / 16.0 - 0.47;
        c += bayer * 0.22 * u_dither;
    }
    if (u_vhs > 0.0) {
        c = mix(c, vec3(dot(c, LUMA)) + (c - vec3(dot(c, LUMA))) * 0.8, u_vhs);
        c += (hash(vec2(floor(uv.y * screenSize.y * 0.5), time)) - 0.5) * 0.05 * u_vhs;
    }
    if (u_scanlines > 0.0) c *= 1.0 - u_scanlines * 0.45 * (0.5 + 0.5 * sin(uv.y * screenSize.y * 1.57));
    if (u_crt > 0.0) {
        float mask = mod(gl_FragCoord.x, 3.0);
        vec3 tint = vec3(mask < 1.0 ? 1.1 : 0.9, mask >= 1.0 && mask < 2.0 ? 1.1 : 0.9, mask >= 2.0 ? 1.1 : 0.9);
        c = mix(c, c * tint, u_crt * 0.6);
        vec2 edge = abs(p - 0.5) * 2.0;
        c *= 1.0 - smoothstep(0.96, 1.02, max(edge.x, edge.y)) * u_crt;
    }
    if (u_flicker > 0.0) c *= 1.0 + (vnoise(vec2(time * 17.0, 0.0)) - 0.5) * 0.35 * u_flicker;
    if (u_grain > 0.0) c += (hash(uv * screenSize + fract(time) * 97.0) - 0.5) * 0.28 * u_grain;

    // on the lens
    if (u_frost > 0.0) {
        float e = length((uv - 0.5) * vec2(1.25, 1.1));
        float n = vnoise(uv * 14.0) * 0.6 + vnoise(uv * 42.0) * 0.4;
        float f = smoothstep(0.75 - u_frost * 0.38, 0.8, e + n * 0.28);
        c = mix(c, vec3(0.86, 0.94, 1.0), f * 0.85);
    }
    if (u_vignette > 0.0 || u_pulse > 0.0) {
        float e = length((uv - 0.5) * vec2(1.0, 1.0)) * 1.42;
        float v = smoothstep(1.0 - u_vignette_softness, 1.0 + u_vignette_softness * 0.2, e);
        float amount = v * u_vignette;
        if (u_pulse > 0.0) {
            float beat = pow(0.5 + 0.5 * sin(time * u_pulse_speed * 6.2831853), 3.0);
            amount += smoothstep(0.35, 1.1, e) * beat * u_pulse;
        }
        c = mix(c, u_vignette_color, clamp(amount, 0.0, 1.0));
    }
    if (hasHistory > 0.5 && u_motion_blur > 0.0) c = mix(c, texture(historyTex, uv).rgb, u_motion_blur);
    if (u_fade > 0.0) c = mix(c, u_fade_color, u_fade);
    if (u_letterbox > 0.0 && (uv.y < u_letterbox || uv.y > 1.0 - u_letterbox)) c = vec3(0.0);

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
