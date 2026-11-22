#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;

layout(location = 0) out vec4 fragColor;

void main() {
    vec2 shift = vec2(0.024, 0.007);
    vec4 firstView = texture(InSampler, clamp(texCoord - shift, vec2(0.001), vec2(0.999)));
    vec4 secondView = texture(InSampler, clamp(texCoord + shift, vec2(0.001), vec2(0.999)));
    fragColor = mix(firstView, secondView, 0.5);
}
