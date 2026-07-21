#version 330
//? if >=26.3
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:fog.glsl>
#include <minecraft:projection.glsl>
#include <minecraft:dynamictransforms.glsl>
//? if <26.3
//#include <minecraft:projection.glsl>

//$ shader_in '0' >> vec
layout(location = 0) in vec3 Position;
//$ shader_in '1' >> vec
layout(location = 1) in vec2 UV0;

//$ shader_out '0' >> vec
layout(location = 0) out vec4 texProj0;
//$ shader_out '1' >> vec
layout(location = 1) out vec2 texCoord0;

//$ shader_out '2' >> float
layout(location = 2) out float sphericalVertexDistance;
//$ shader_out '3' >> float
layout(location = 3) out float cylindricalVertexDistance;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);

    texProj0 = projection_from_position(gl_Position);
    texCoord0 = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;

    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
}
