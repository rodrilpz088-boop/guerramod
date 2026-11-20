// Rayos de luz (crepusculares) que atraviesan nubes, árboles y montañas cuando miras hacia el sol.
#include "/lib/ajustes.glsl"
#include "/lib/comun.glsl"
#ifdef VSH
out vec2 uv;
void main() {
    gl_Position = ftransform();
    uv = gl_MultiTexCoord0.xy;
}
#endif
#ifdef FSH
#include "/lib/lut.glsl"
uniform sampler2D colortex0;
uniform sampler2D depthtex0;
in vec2 uv;
void main() {
    vec3 col = texture(colortex0, uv).rgb;
#if defined RAYOS_DE_LUZ && !defined DIM_NETHER && !defined DIM_END
    vec3 sp = sunPosition;
    if (sp.z < 0.0 && isEyeInWater == 0) {
        vec4 cl = gbufferProjection * vec4(sp, 1.0);
        vec2 sol = cl.xy / cl.w * 0.5 + 0.5;
        vec3 dirV = normalize(vistaDesde(uv, 1.0));
        float cerca = pow(max(dot(dirV, normalize(sp)), 0.0), 6.0);
        if (cerca > 0.001) {
            vec2 delta = (sol - uv) / 32.0;
            vec2 p = uv + delta * ruidoPantalla(gl_FragCoord.xy);
            float acc = 0.0;
            for (int i = 0; i < 32; i++) {
                if (p.x < 0.0 || p.x > 1.0 || p.y < 0.0 || p.y > 1.0) break;
                acc += texture(depthtex0, p).r >= 1.0 ? 1.0 : 0.0;
                p += delta;
            }
            acc /= 32.0;
            float altura = smoothstep(-0.05, 0.1, dirSolMundo().y);
            col += especial(4) * 0.012 * acc * cerca * altura * (1.0 - rainStrength) * exterior();
        }
    }
#endif
    /* DRAWBUFFERS:0 */
    gl_FragData[0] = vec4(col, 1.0);
}
#endif
