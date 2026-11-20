#ifndef AJUSTES
#define AJUSTES
// ---------------------------------------------------------------- opciones (menú del shader)
#define SOMBRAS_SUAVES
#define NUBES_VOLUMETRICAS
#define CALIDAD_NUBES 24 // [12 16 24 32 48]
#define CANTIDAD_NUBES 1.0 // [0.5 0.75 1.0 1.25 1.5]
#define TIEMPO 0 // [0 1 2 3]
#define CIRROS
#define SOMBRAS_NUBES
#define RAYOS_DE_LUZ
#define REFLEJOS_SSR
#define CHARCOS
#define PLANTAS_ONDULANTES
#define BLOOM
#define INTENSIDAD_BRUMA 1.0 // [0.0 0.5 1.0 1.5 2.0 3.0]
#define BRILLO 1.0 // [0.6 0.8 1.0 1.2 1.4 1.7 2.0]
#define SATURACION 1.05 // [0.8 0.9 1.0 1.05 1.1 1.2 1.3]

// ---------------------------------------------------------------- constantes que lee Oculus/Iris
const int shadowMapResolution = 2048; // [1024 2048 3072 4096]
const float shadowDistance = 128.0; // [64.0 96.0 128.0 160.0 192.0 256.0]
const float sunPathRotation = -30.0; // [-45.0 -40.0 -30.0 -20.0 -10.0 0.0]
const bool shadowHardwareFiltering = true;
const float shadowDistanceRenderMul = 1.0;
const float wetnessHalflife = 300.0;
const float drynessHalflife = 1200.0;
const float eyeBrightnessHalflife = 8.0;
const float ambientOcclusionLevel = 1.0;
const int noiseTextureResolution = 256;
/*
const int colortex0Format = RGBA16F;
const int colortex1Format = RGBA16;
const int colortex2Format = RGBA8;
const int colortex4Format = RGBA16;
const int colortex5Format = RGBA8;
const int colortex6Format = RGBA8;
const int colortex7Format = RGBA16F;
*/
const vec4 colortex0ClearColor = vec4(0.0, 0.0, 0.0, 1.0);
const vec4 colortex4ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
const vec4 colortex5ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
const vec4 colortex6ClearColor = vec4(0.0, 0.0, 0.0, 0.0);
const bool colortex7Clear = false;

// escala de la luz directa sobre el terreno respecto al cielo (calibrado con el modelo atmosférico)
const float SOL_K = 0.25;
#endif
