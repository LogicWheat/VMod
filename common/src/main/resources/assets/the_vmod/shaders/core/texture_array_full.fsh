#version 450 core

#moj_import <fog.glsl>

uniform sampler2DArray texture_array;
uniform int MaxLayers;
uniform int CurrentLayer;

uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexColor;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;

out vec4 fragColor;

float getCoord(int capacity, int layer)
{
    return max(0, min(float(capacity - 1), floor(float(layer) + 0.5)));
}

//TODO
void main() {
    vec4 color = texture(texture_array, vec3(texCoord0, getCoord(MaxLayers, CurrentLayer)));
    color *= vertexColor * ColorModulator;
//    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
//    color *= lightMapColor;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);

//    vec4 temp = vertexColor * ColorModulator * overlayColor * lightMapColor * vertexDistance * FogStart * FogEnd * FogColor;
//
//    vec4 color = texture(texture_array, vec3(texCoord0, getCoord(MaxLayers, CurrentLayer)));
//    color.a = 1.0;
//
//    fragColor = color;
}
