#version 330

#moj_import <minecraft:globals.glsl>

// Visão Psicodélica (Chá de Cogumelo): tela ondulando, cores separando e um arco-íris girando em anéis.
uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

vec3 hueShift(vec3 color, float angle) {
    const vec3 k = vec3(0.57735);
    float c = cos(angle);
    return color * c + cross(k, color) * sin(angle) + k * dot(k, color) * (1.0 - c);
}

void main() {
    // GameTime vai de 0 a 1 ao longo de um dia (20 min): x1200 = segundos
    float t = GameTime * 1200.0;
    vec2 uv = texCoord;
    uv.x += sin(uv.y * 16.0 + t * 1.7) * 0.0045;
    uv.y += cos(uv.x * 12.0 + t * 1.3) * 0.0045;

    // cores se separando (aberração cromática) que vai e volta
    vec2 split = vec2(sin(t * 0.9), cos(t * 0.7)) * 0.0035;
    vec3 color = vec3(texture(InSampler, uv + split).r, texture(InSampler, uv).g, texture(InSampler, uv - split).b);

    // saturação alta
    float luma = dot(color, vec3(0.3, 0.59, 0.11));
    color = mix(vec3(luma), color, 1.8);

    // arco-íris em anéis saindo do centro
    float dist = length((texCoord - 0.5) * vec2(OutSize.x / OutSize.y, 1.0));
    color = hueShift(color, t * 0.5 + dist * 5.0);

    // bordas pulsando escuras
    color *= 1.0 - smoothstep(0.45, 0.95, dist) * (0.35 + 0.15 * sin(t * 1.8));

    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
