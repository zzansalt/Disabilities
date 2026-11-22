#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;
uniform sampler2D DepthSampler;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec4 sharp = texture(InSampler, texCoord);
    float rawDepth = texture(DepthSampler, texCoord).r;

    // Minecraft 26.3 uses Reverse-Z where rawDepth ~= zNear (0.05) / distance
    float dist = rawDepth > 0.00001 ? (0.05 / rawDepth) : 1000.0;

    // Nearby objects turn pitch black so only their silhouettes remain visible
    float silhouetteMask = 1.0 - smoothstep(4.5, 6.2, dist);
    vec3 result = mix(sharp.rgb, vec3(0.0, 0.0, 0.0), silhouetteMask);
    fragColor = vec4(result, sharp.a);
}
