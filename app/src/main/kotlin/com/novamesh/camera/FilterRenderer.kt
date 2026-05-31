/**
 * FilterRenderer — OpenGL ES 2.0 real-time camera filter renderer.
 *
 * Provides GPU shader-based colour and effect filters for the camera preview:
 * - Color transforms (sepia, noir, vivid, warm, cool)
 * - Distortion effects (vignette, blur, pixelate)
 * - Artistic filters (neon, comic, oil paint)
 *
 * Usage:
 * ```kotlin
 * val renderer = FilterRenderer()
 * renderer.init()
 * // Each frame:
 * renderer.drawFrame(textureId, FilterType.SEPIA, transformMatrix)
 * // Cleanup:
 * renderer.release()
 * ```
 */

package com.novamesh.camera

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 2.0 renderer for applying real-time shader filters to camera frames.
 *
 * The renderer compiles vertex and fragment shaders at initialisation and
 * renders a full-screen quad textured with the camera frame, applying the
 * selected [FilterType] via the fragment shader.
 */
class FilterRenderer {

    // ──────────────────────────────────────────────────────────────────────────
    // Constants — shader source code
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Simple pass-through vertex shader.
     *
     * Accepts vertex positions and texture coordinates, applies an
     * optional transformation matrix, and passes texture coordinates
     * to the fragment shader.
     */
    companion object {
        private const val VERTEX_SHADER = """
            uniform mat4 uTransformMatrix;
            attribute vec4 aPosition;
            attribute vec4 aTextureCoord;
            varying vec2 vTextureCoord;
            void main() {
                gl_Position = uTransformMatrix * aPosition;
                vTextureCoord = aTextureCoord.xy;
            }
        """

        /**
         * Base fragment shader — pass-through (no filter).
         * Samples the texture directly.
         */
        private const val FRAGMENT_SHADER_NONE = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            void main() {
                gl_FragColor = texture2D(sTexture, vTextureCoord);
            }
        """

        /**
         * Sepia tone fragment shader.
         * Applies a 3×3 colour matrix that gives a warm brownish tone.
         */
        private const val FRAGMENT_SHADER_SEPIA = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                float r = dot(color.rgb, vec3(0.393, 0.769, 0.189));
                float g = dot(color.rgb, vec3(0.349, 0.686, 0.168));
                float b = dot(color.rgb, vec3(0.272, 0.534, 0.131));
                gl_FragColor = vec4(r, g, b, color.a);
            }
        """

        /**
         * Noir (black & white) fragment shader.
         * Desaturates the image using luminance weights.
         */
        private const val FRAGMENT_SHADER_NOIR = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
                gl_FragColor = vec4(vec3(gray), color.a);
            }
        """

        /**
         * Vivid fragment shader.
         * Increases contrast and saturation for a punchy look.
         */
        private const val FRAGMENT_SHADER_VIVID = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                // Increase contrast
                vec3 contrast = (color.rgb - 0.5) * 1.4 + 0.5;
                // Increase saturation
                float gray = dot(contrast, vec3(0.299, 0.587, 0.114));
                vec3 saturated = mix(vec3(gray), contrast, 1.5);
                gl_FragColor = vec4(saturated, color.a);
            }
        """

        /**
         * Warm fragment shader.
         * Adds an orange/yellow tint to simulate warm lighting.
         */
        private const val FRAGMENT_SHADER_WARM = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                color.r = min(1.0, color.r * 1.1);
                color.g = min(1.0, color.g * 0.95);
                color.b = max(0.0, color.b * 0.85);
                gl_FragColor = color;
            }
        """

        /**
         * Cool fragment shader.
         * Adds a blue tint to simulate cool / shadow lighting.
         */
        private const val FRAGMENT_SHADER_COOL = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                color.r = max(0.0, color.r * 0.9);
                color.g = min(1.0, color.g * 0.95);
                color.b = min(1.0, color.b * 1.15);
                gl_FragColor = color;
            }
        """

        /**
         * Vignette fragment shader.
         * Darkens the edges of the frame based on distance from centre.
         */
        private const val FRAGMENT_SHADER_VIGNETTE = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                vec2 center = vec2(0.5, 0.5);
                float dist = distance(vTextureCoord, center);
                float vignette = smoothstep(0.8, 0.2, dist);
                gl_FragColor = vec4(color.rgb * vignette, color.a);
            }
        """

        /**
         * Simple box blur fragment shader (9-tap).
         * Samples neighbouring pixels and averages them.
         */
        private const val FRAGMENT_SHADER_BLUR = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            uniform vec2 uTexOffset;
            void main() {
                vec4 color = vec4(0.0);
                float stepX = uTexOffset.x * 4.0;
                float stepY = uTexOffset.y * 4.0;
                // 3x3 blur kernel
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        vec2 offset = vec2(float(x) * stepX, float(y) * stepY);
                        color += texture2D(sTexture, vTextureCoord + offset);
                    }
                }
                gl_FragColor = color / 9.0;
            }
        """

        /**
         * Pixelate (mosaic) fragment shader.
         * Divides the image into blocks and samples the centre of each.
         */
        private const val FRAGMENT_SHADER_PIXELATE = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            uniform vec2 uTexOffset;
            void main() {
                float pixelSize = 0.04;
                vec2 uv = floor(vTextureCoord / pixelSize) * pixelSize + pixelSize * 0.5;
                gl_FragColor = texture2D(sTexture, uv);
            }
        """

        /**
         * Neon fragment shader.
         * Detects edges via a simple Laplacian and amplifies them with
         * bright saturated colours.
         */
        private const val FRAGMENT_SHADER_NEON = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            uniform vec2 uTexOffset;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                // Simple edge detection via offset sampling
                vec2 off = uTexOffset * 2.0;
                vec4 edge = texture2D(sTexture, vTextureCoord + vec2(off.x, 0.0))
                          + texture2D(sTexture, vTextureCoord - vec2(off.x, 0.0))
                          + texture2D(sTexture, vTextureCoord + vec2(0.0, off.y))
                          + texture2D(sTexture, vTextureCoord - vec2(0.0, off.y))
                          - 4.0 * color;
                float intensity = length(edge.rgb);
                vec3 neon = vec3(1.0) - vec3(intensity);
                gl_FragColor = vec4(mix(color.rgb, neon, 0.7), color.a);
            }
        """

        /**
         * Comic / posterize fragment shader.
         * Reduces the number of colour levels for a cel-shaded look.
         */
        private const val FRAGMENT_SHADER_COMIC = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            void main() {
                vec4 color = texture2D(sTexture, vTextureCoord);
                float levels = 4.0;
                vec3 quantized = floor(color.rgb * levels) / levels;
                // Add subtle edge line
                gl_FragColor = vec4(quantized, color.a);
            }
        """

        /**
         * Oil paint / kuwahara-style filter (simplified).
         * Blurs while preserving edges by averaging colours in blocks.
         */
        private const val FRAGMENT_SHADER_OIL_PAINT = """
            precision mediump float;
            varying vec2 vTextureCoord;
            uniform sampler2D sTexture;
            uniform vec2 uTexOffset;
            void main() {
                vec4 color = vec4(0.0);
                float stepX = uTexOffset.x * 3.0;
                float stepY = uTexOffset.y * 3.0;
                float weights[9];
                float total = 0.0;
                // Simple weighted average for "smoothed" look
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        vec2 offset = vec2(float(x) * stepX, float(y) * stepY);
                        vec4 sample = texture2D(sTexture, vTextureCoord + offset);
                        float w = 1.0 - length(sample.rgb - texture2D(sTexture, vTextureCoord).rgb) * 2.0;
                        w = clamp(w, 0.0, 1.0);
                        color += sample * w;
                        total += w;
                    }
                }
                gl_FragColor = color / max(total, 0.001);
            }
        """

        /** Mapping from [FilterType] to the corresponding fragment shader. */
        private val FILTER_SHADER_MAP = mapOf(
            FilterType.NONE to FRAGMENT_SHADER_NONE,
            FilterType.SEPIA to FRAGMENT_SHADER_SEPIA,
            FilterType.NOIR to FRAGMENT_SHADER_NOIR,
            FilterType.VIVID to FRAGMENT_SHADER_VIVID,
            FilterType.WARM to FRAGMENT_SHADER_WARM,
            FilterType.COOL to FRAGMENT_SHADER_COOL,
            FilterType.VIGNETTE to FRAGMENT_SHADER_VIGNETTE,
            FilterType.BLUR to FRAGMENT_SHADER_BLUR,
            FilterType.PIXELATE to FRAGMENT_SHADER_PIXELATE,
            FilterType.NEON to FRAGMENT_SHADER_NEON,
            FilterType.COMIC to FRAGMENT_SHADER_COMIC,
            FilterType.OIL_PAINT to FRAGMENT_SHADER_OIL_PAINT,
        )

        // ── Geometry ─────────────────────────────────────────────────────────

        /** Full-screen quad vertices (clip space: [-1, 1]). */
        private val QUAD_VERTICES = floatArrayOf(
            -1f, -1f,  // bottom left
             1f, -1f,  // bottom right
            -1f,  1f,  // top left
             1f,  1f,  // top right
        )

        /** Corresponding texture coordinates (flipped for camera). */
        private val TEX_COORDS = floatArrayOf(
            0f, 1f,  // bottom left
            1f, 1f,  // bottom right
            0f, 0f,  // top left
            1f, 0f,  // top right
        )

        /** Index buffer for triangle strip. */
        private val DRAW_ORDER = shortArrayOf(0, 1, 2, 1, 3, 2)

        /** Byte size of a float (4 bytes). */
        private const val FLOAT_SIZE = 4
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Filter types
    // ──────────────────────────────────────────────────────────────────────────

    /** Available camera filter effects. */
    enum class FilterType {
        /** No filter — pass-through. */
        NONE,
        /** Warm sepia tone. */
        SEPIA,
        /** Neon edge glow. */
        NEON,
        /** Black & white desaturated. */
        NOIR,
        /** Increased contrast and saturation. */
        VIVID,
        /** Orange/yellow warm tint. */
        WARM,
        /** Blue cool tint. */
        COOL,
        /** Darkened edges. */
        VIGNETTE,
        /** Simple box blur. */
        BLUR,
        /** Pixelation / mosaic. */
        PIXELATE,
        /** Comic book / posterize. */
        COMIC,
        /** Oil paint smoothing. */
        OIL_PAINT,
    }

    // ──────────────────────────────────────────────────────────────────────────
    // OpenGL resources
    // ──────────────────────────────────────────────────────────────────────────

    private var program: Int = 0
    private var currentFilterProgram: Int = 0

    // Attribute / uniform handles for the base program
    private var aPositionHandle: Int = 0
    private var aTextureCoordHandle: Int = 0
    private var uTransformMatrixHandle: Int = 0
    private var uTextureHandle: Int = 0
    private var uTexOffsetHandle: Int = 0

    private var vertexBuffer: FloatBuffer? = null
    private var texCoordBuffer: FloatBuffer? = null
    private var drawListBuffer: ShortBuffer? = null

    /** Identity matrix used as default transform. */
    private val identityMatrix = FloatArray(16).also { Matrix.setIdentityM(it, 0) }

    /** Whether the renderer has been initialised. */
    private var isInitialised = false

    // ──────────────────────────────────────────────────────────────────────────
    // Initialisation
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Initialises the OpenGL renderer.
     *
     * Compiles shaders, creates the GL program, and sets up vertex buffer
     * objects. Must be called on a thread with a valid OpenGL context.
     *
     * @return `true` if initialisation succeeded, `false` otherwise.
     */
    fun init(): Boolean {
        if (isInitialised) return true

        // ── Compile shaders ──────────────────────────────────────────────
        // We compile ALL shaders up front so that switching filters is fast.
        // For simplicity we store the "none" program as the base.

        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        if (vertexShader == 0) return false

        val fragmentShader = compileShader(
            GLES20.GL_FRAGMENT_SHADER,
            FRAGMENT_SHADER_NONE,
        )
        if (fragmentShader == 0) return false

        program = createProgram(vertexShader, fragmentShader)
        if (program == 0) return false

        // ── Get attribute/uniform locations ──────────────────────────────
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aTextureCoordHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        uTransformMatrixHandle = GLES20.glGetUniformLocation(program, "uTransformMatrix")
        uTextureHandle = GLES20.glGetUniformLocation(program, "sTexture")
        uTexOffsetHandle = GLES20.glGetUniformLocation(program, "uTexOffset")

        currentFilterProgram = program

        // ── Set up buffers ───────────────────────────────────────────────
        vertexBuffer = ByteBuffer
            .allocateDirect(QUAD_VERTICES.size * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(QUAD_VERTICES)
            .apply { position(0) }

        texCoordBuffer = ByteBuffer
            .allocateDirect(TEX_COORDS.size * FLOAT_SIZE)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(TEX_COORDS)
            .apply { position(0) }

        drawListBuffer = ByteBuffer
            .allocateDirect(DRAW_ORDER.size * 2) // short = 2 bytes
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
            .put(DRAW_ORDER)
            .apply { position(0) }

        isInitialised = true
        return true
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Rendering
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Renders a single frame with the selected [filter] applied.
     *
     * @param textureId       OpenGL texture ID of the camera frame.
     * @param filter          The [FilterType] to apply (default NONE).
     * @param transformMatrix 4×4 transformation matrix (pass identity if
     *                        none needed).
     */
    fun drawFrame(
        textureId: Int,
        filter: FilterType = FilterType.NONE,
        transformMatrix: FloatArray = identityMatrix,
    ) {
        if (!isInitialised) return

        // ── Select the appropriate shader program for this filter ────────
        val filterShader = FILTER_SHADER_MAP[filter] ?: FRAGMENT_SHADER_NONE
        ensureFilterProgram(filterShader)

        // ── Use the program ──────────────────────────────────────────────
        GLES20.glUseProgram(currentFilterProgram)

        // ── Set up vertex attributes ─────────────────────────────────────
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        GLES20.glVertexAttribPointer(
            aPositionHandle,
            2,               // 2 components per vertex (x, y)
            GLES20.GL_FLOAT,
            false,
            0,
            vertexBuffer,
        )

        GLES20.glEnableVertexAttribArray(aTextureCoordHandle)
        GLES20.glVertexAttribPointer(
            aTextureCoordHandle,
            2,               // 2 components per tex coord (u, v)
            GLES20.GL_FLOAT,
            false,
            0,
            texCoordBuffer,
        )

        // ── Set uniforms ─────────────────────────────────────────────────
        // Transform matrix
        GLES20.glUniformMatrix4fv(uTransformMatrixHandle, 1, false, transformMatrix, 0)

        // Texture unit 0
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTextureHandle, 0)

        // Texture offset (used by blur, pixelate, neon, oil paint)
        // Approximate pixel size in normalised coordinates
        if (uTexOffsetHandle != -1) {
            GLES20.glUniform2f(uTexOffsetHandle, 0.002f, 0.002f)
        }

        // ── Draw the quad ────────────────────────────────────────────────
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            DRAW_ORDER.size,
            GLES20.GL_UNSIGNED_SHORT,
            drawListBuffer,
        )

        // ── Cleanup ──────────────────────────────────────────────────────
        GLES20.glDisableVertexAttribArray(aPositionHandle)
        GLES20.glDisableVertexAttribArray(aTextureCoordHandle)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        GLES20.glUseProgram(0)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Texture management
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Creates a new OpenGL 2D texture suitable for uploading camera frames.
     *
     * The texture is configured with [GLES20.GL_NEAREST] filtering and
     * [GLES20.GL_CLAMP_TO_EDGE] wrapping.
     *
     * @return The OpenGL texture ID.
     */
    fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val textureId = textures[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        // Set filtering parameters
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )

        // Set wrapping parameters
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        return textureId
    }

    /**
     * Uploads a camera frame (as a [Bitmap]) to the GPU texture.
     *
     * @param textureId The OpenGL texture ID to upload to.
     * @param bitmap    The Android [Bitmap] containing the camera frame.
     */
    fun uploadTexture(textureId: Int, bitmap: android.graphics.Bitmap) {
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Cleanup
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Releases all OpenGL resources (program, shaders, buffers).
     *
     * Must be called on the GL thread when the renderer is no longer needed.
     */
    fun release() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }

        vertexBuffer = null
        texCoordBuffer = null
        drawListBuffer = null
        isInitialised = false
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Compiles a single GL shader of the given [type].
     *
     * @param type       [GLES20.GL_VERTEX_SHADER] or [GLES20.GL_FRAGMENT_SHADER].
     * @param source     The GLSL shader source string.
     * @return The shader ID, or 0 on failure.
     */
    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        if (shader == 0) return 0

        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        // Check compile status
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
        if (compiled[0] == 0) {
            GLES20.glDeleteShader(shader)
            return 0
        }

        return shader
    }

    /**
     * Links a vertex and fragment shader into a GL program.
     *
     * @return The program ID, or 0 on failure.
     */
    private fun createProgram(vertexShader: Int, fragmentShader: Int): Int {
        val programId = GLES20.glCreateProgram()
        if (programId == 0) return 0

        GLES20.glAttachShader(programId, vertexShader)
        GLES20.glAttachShader(programId, fragmentShader)
        GLES20.glLinkProgram(programId)

        // Check link status
        val linked = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linked, 0)
        if (linked[0] == 0) {
            GLES20.glDeleteProgram(programId)
            return 0
        }

        return programId
    }

    /**
     * Ensures the current filter program matches the requested [fragmentSource].
     *
     * Each filter has its own fragment shader. We lazily compile and cache
     * programs for each filter type.
     */
    private val filterProgramCache = mutableMapOf<String, Int>()

    private fun ensureFilterProgram(fragmentSource: String) {
        val cached = filterProgramCache[fragmentSource]
        if (cached != null) {
            if (currentFilterProgram != cached) {
                currentFilterProgram = cached
                updateUniformLocations(currentFilterProgram)
            }
            return
        }

        // Compile and link a new program for this filter
        val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER)
        if (vertexShader == 0) return

        val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        if (fragmentShader == 0) {
            GLES20.glDeleteShader(vertexShader)
            return
        }

        val newProgram = createProgram(vertexShader, fragmentShader)
        if (newProgram == 0) {
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            return
        }

        filterProgramCache[fragmentSource] = newProgram
        currentFilterProgram = newProgram
        updateUniformLocations(newProgram)

        // We can delete the shaders now since the program is linked
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)
    }

    /**
     * Updates attribute/uniform handles for the given [programId].
     */
    private fun updateUniformLocations(programId: Int) {
        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        aTextureCoordHandle = GLES20.glGetAttribLocation(programId, "aTextureCoord")
        uTransformMatrixHandle = GLES20.glGetUniformLocation(programId, "uTransformMatrix")
        uTextureHandle = GLES20.glGetUniformLocation(programId, "sTexture")
        uTexOffsetHandle = GLES20.glGetUniformLocation(programId, "uTexOffset")
    }
}
