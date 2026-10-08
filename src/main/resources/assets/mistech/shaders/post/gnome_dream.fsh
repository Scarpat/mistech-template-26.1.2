#version 330

#moj_import <minecraft:globals.glsl>

// Dimensão dos Gnomos: sonho leve - a tela ondula devagar, as cores ficam mais vivas e as bordas embaçam num tom lilás.
uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

out vec4 fragColor;

void main() {
    float t = GameTime * 1200.0;
    vec2 uv = texCoord;
    uv.x += sin(uv.y * 9.0 + t * 0.6) * 0.0018;
    uv.y += cos(uv.x * 7.0 + t * 0.5) * 0.0018;
    vec3 color = texture(InSampler, uv).rgb;

    // borda "sonhadora": mistura com uma versão borrada e puxada para lilás
    vec2 texel = 1.0 / InSize;
    vec3 blur = vec3(0.0);
    for (int i = -2; i <= 2; i++) {
        for (int j = -2; j <= 2; j++) {
            blur += texture(InSampler, uv + vec2(i, j) * texel * 2.5).rgb;
        }
    }
    blur /= 25.0;
    float dist = length((texCoord - 0.5) * vec2(OutSize.x / OutSize.y, 1.0));
    float edge = smoothstep(0.35, 0.85, dist);
    color = mix(color, blur * vec3(1.05, 0.95, 1.15), edge * 0.7);

    // cores um pouco mais vivas
    float luma = dot(color, vec3(0.3, 0.59, 0.11));
    color = mix(vec3(luma), color, 1.25);
    fragColor = vec4(clamp(color, 0.0, 1.0), 1.0);
}
