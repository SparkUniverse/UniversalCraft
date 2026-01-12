package gg.essential.universal;

import gg.essential.universal.render.ScissorState;
import gg.essential.universal.shader.BlendState;
import gg.essential.universal.utils.ReleasedDynamicTexture;
import gg.essential.universal.vertex.UVertexConsumer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

//#if STANDALONE
//$$ import gg.essential.universal.standalone.nanovg.NvgContext;
//$$ import gg.essential.universal.standalone.nanovg.NvgFont;
//$$ import gg.essential.universal.standalone.nanovg.NvgFontFace;
//$$ import gg.essential.universal.standalone.render.BufferBuilder;
//$$ import gg.essential.universal.standalone.render.DefaultShader;
//$$ import gg.essential.universal.standalone.render.DefaultVertexFormats;
//$$ import gg.essential.universal.standalone.render.Gl2Renderer;
//$$ import gg.essential.universal.standalone.render.VertexFormat;
//$$ import org.jetbrains.annotations.ApiStatus;
//$$ import org.lwjgl.opengl.GL11;
//$$ import org.lwjgl.opengl.GL20;
//$$ import java.net.URL;
//$$ import static kotlin.io.TextStreamsKt.readBytes;
//$$ import static org.lwjgl.opengl.GL14.*;
//#else
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

import static org.lwjgl.opengl.GL11.GL_TEXTURE_BINDING_2D;
import static org.lwjgl.opengl.GL13.GL_ACTIVE_TEXTURE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;

//#if MC>=12111
//$$ import com.mojang.blaze3d.textures.FilterMode;
//#endif

//#if MC>=12109
//$$ import net.minecraft.client.font.TextDrawable;
//$$ import net.minecraft.text.StyleSpriteSource;
//#endif

//#if MC>=12106
//$$ import com.mojang.blaze3d.systems.RenderPass;
//$$ import com.mojang.blaze3d.textures.GpuTextureView;
//$$ import gg.essential.universal.render.URenderPass;
//$$ import gg.essential.universal.render.URenderPipeline;
//$$ import gg.essential.universal.vertex.UBuiltBuffer;
//$$ import net.minecraft.client.font.BakedGlyph;
//#endif

//#if MC>=12105
//$$ import com.mojang.blaze3d.pipeline.RenderPipeline;
//$$ import com.mojang.blaze3d.textures.TextureFormat;
//$$ import net.minecraft.client.texture.GlTexture;
//#endif

//#if MC>=12102
//#if MC<12105
//$$ import net.minecraft.client.gl.ShaderProgramKey;
//$$ import net.minecraft.client.gl.ShaderProgramKeys;
//#endif
//$$ import java.util.HashMap;
//#endif

//#if MC>=12100
//#if MC<12105
//$$ import net.minecraft.client.render.BufferRenderer;
//#endif
//$$ import net.minecraft.client.render.BuiltBuffer;
//$$ import net.minecraft.client.util.BufferAllocator;
//#endif

//#if MC>=12005
//$$ import org.joml.Vector3f;
//#endif

//#if MC>=11904
//$$ import net.minecraft.client.font.TextRenderer;
//#endif

//#if MC>=11900
//$$ import net.minecraft.text.Text;
//#endif

//#if MC>=11700
//$$ import net.minecraft.client.render.GameRenderer;
//$$ import net.minecraft.client.render.Shader;
//$$ import org.jetbrains.annotations.ApiStatus;
//$$ import java.util.IdentityHashMap;
//$$ import java.util.Map;
//$$ import java.util.function.Supplier;
//#else
import net.minecraft.client.renderer.vertex.VertexFormatElement;
//#endif

//#if MC>=11602
//$$ import net.minecraft.util.ResourceLocation;
//$$ import net.minecraft.util.text.Style;
//$$ import net.minecraft.util.text.CharacterManager;
//$$ import net.minecraft.util.text.StringTextComponent;
//$$ import net.minecraft.util.text.ITextProperties;
//$$ import net.minecraft.client.renderer.RenderType;
//$$ import net.minecraft.client.renderer.IRenderTypeBuffer;
//$$ import java.util.ArrayList;
//$$ import java.util.Optional;
//#else
import org.lwjgl.util.vector.Matrix3f;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;
//#endif

//#if MC>=11502
//$$ import com.mojang.blaze3d.platform.GlStateManager;
//$$ import net.minecraft.client.renderer.texture.NativeImage;
//$$ import java.io.ByteArrayInputStream;
//$$ import java.io.ByteArrayOutputStream;
//#endif

//#if MC>=11400
//$$ import net.minecraft.client.renderer.texture.Texture;
//#else
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.ITextureObject;
//#endif

//#endif

@SuppressWarnings("deprecation") // lots of MC methods are deprecated on some versions but only replaced on the next one
public class UGraphics {
    private static final Pattern formattingCodePattern = Pattern.compile("(?i)\u00a7[0-9A-FK-OR]");

    private static UMatrixStack UNIT_STACK = UMatrixStack.UNIT;

    //#if STANDALONE
    //$$ public static void init() { /* triggers static initializer */ }
    //$$ @ApiStatus.Internal
    //$$ public static final Gl2Renderer RENDERER = new Gl2Renderer();
    //$$ private static final NvgFont MC_FONT;
    //$$ static {
    //$$     URL fontUrl = UGraphics.class.getResource("/fonts/Minecraft-Regular.otf");
    //$$     assert fontUrl != null;
    //$$     MC_FONT = new NvgFont(new NvgFontFace(new NvgContext(), readBytes(fontUrl)), 10, 7f, 1f);
    //$$
    //$$     // TODO should probably put this in a unit test, but lwjgl makes those a bit tricky to set up
    //$$     assert UGraphics.getStringWidth("a") == 6;
    //$$     assert UGraphics.getStringWidth("aa") == 12;
    //$$     assert UGraphics.getStringWidth(" ") == 4;
    //$$     assert UGraphics.getStringWidth("  ") == 8;
    //$$     assert UGraphics.getStringWidth("a ") == 10;
    //$$     assert UGraphics.getStringWidth("a  ") == 14;
    //$$ }
    //$$
    //$$ private BufferBuilder instance;
    //$$ private DrawMode drawMode;
    //$$ private CommonVertexFormats vertexFormat;
    //$$ private DefaultShader shader;
    //#else

    //#if MC>=12109
    //$$ public static Style EMPTY_WITH_FONT_ID = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("minecraft", "alt")));
    //#elseif MC>=12100
    //$$ public static Style EMPTY_WITH_FONT_ID = Style.EMPTY.withFont(Identifier.of("minecraft", "alt"));
    //#elseif MC>=11602
    //$$ public static Style EMPTY_WITH_FONT_ID = Style.EMPTY.setFontId(new ResourceLocation("minecraft", "alt"));
    //#endif
    public static int ZERO_TEXT_ALPHA = 10;
    private WorldRenderer instance;
    private VertexFormat vertexFormat;

    //#if MC>=11904
    //$$ private static final TextRenderer.TextLayerType TEXT_LAYER_TYPE = TextRenderer.TextLayerType.NORMAL;
    //#elseif MC>=11602
    //$$ private static final boolean TEXT_LAYER_TYPE = false;
    //#endif

    //#if MC>=12100
    //$$ /**
    //$$  * A buffer to use for storing sorted quad vertex indices. The buffer will automatically grow as needed,
    //$$  * the specified size is not a hard cap.
    //$$  */
    //$$ private static final BufferAllocator SORTED_QUADS_ALLOCATOR = new BufferAllocator(65536);
    //#endif

    public UGraphics(WorldRenderer instance) {
        this.instance = instance;
    }
    //#endif

    public UVertexConsumer asUVertexConsumer() {
        //#if STANDALONE
        //$$ return instance;
        //#else
        return UVertexConsumer.of(instance);
        //#endif
    }

    public static UGraphics getFromTessellator() {
        //#if STANDALONE
        //$$ return new UGraphics();
        //#elseif MC>=12100
        //$$ return new UGraphics(null);
        //#else
        return new UGraphics(getTessellator().getWorldRenderer());
        //#endif
    }

    //#if MC<11700
    @Deprecated // See UGraphics.GL
    public static void pushMatrix() {
        GL.pushMatrix();
    }

    @Deprecated // See UGraphics.GL
    public static void popMatrix() {
        GL.popMatrix();
    }

    @Deprecated // See UGraphics.GL
    public static void translate(float x, float y, float z) {
        GL.translate(x, y, z);
    }

    @Deprecated // See UGraphics.GL
    public static void translate(double x, double y, double z) {
        GL.translate(x, y, z);
    }

    @Deprecated // See UGraphics.GL
    public static void rotate(float angle, float x, float y, float z) {
        GL.rotate(angle, x, y, z);
    }

    @Deprecated // See UGraphics.GL
    public static void scale(float x, float y, float z) {
        GL.scale(x, y, z);
    }

    @Deprecated // See UGraphics.GL
    public static void scale(double x, double y, double z) {
        GL.scale(x, y, z);
    }
    //#endif

    //#if !STANDALONE
    public static Tessellator getTessellator() {
        return Tessellator.getInstance();
    }
    //#endif

    //#if MC>=12100
    //$$ // No possible alternative on 1.21. A compile time error here is better than a run time one.
    //#else
    @Deprecated // Use the non-static methods for 1.17+ compatibility or call UGraphics.getTessellator().draw() directly
    public static void draw() {
        getTessellator().draw();
    }
    //#endif

    public static boolean isCoreProfile() {
        //#if MC>=11700
        //$$ return true;
        //#else
        return false;
        //#endif
    }

    @Deprecated // only works on Forge 1.12.2 and below (relies on a Forge patch)
    public static void enableStencil() {
        //#if MC<11400
        Framebuffer framebuffer = Minecraft.getMinecraft().getFramebuffer();
        if (!framebuffer.isStencilEnabled()) {
            framebuffer.enableStencil();
        }
        //#endif
    }

    @Deprecated // see UGraphics.Globals
    public static void cullFace(int mode) {
        //#if MC>=12105 && !STANDALONE
        //#elseif MC>=11502
        //$$ GL11.glCullFace(mode);
        //#elseif MC>10809
        //$$ GlStateManager.CullFace[] values = GlStateManager.CullFace.values();
        //$$ for (GlStateManager.CullFace value : values) {
        //$$     if (value.mode == mode) {
        //$$         GlStateManager.cullFace(value);
        //$$         return;
        //$$     }
        //$$ }
        //$$ throw new IllegalArgumentException(String.format("Mode %d is not valid!", mode));
        //#else
        GlStateManager.cullFace(mode);
        //#endif

    }

    public static void enableLighting() {
        //#if MC<11700
        GlStateManager.enableLighting();
        //#endif
    }

    public static void disableLighting() {
        //#if MC<11700
        GlStateManager.disableLighting();
        //#endif
    }

    public static void disableLight(int mode) {
        //#if MC<=11202
        GlStateManager.disableLight(mode);
        //#endif
    }

    public static void enableLight(int mode) {
        //#if MC<=11202
        GlStateManager.enableLight(mode);
        //#endif
    }

    @Deprecated // see UGraphics.Globals
    public static void enableBlend() {
        //#if STANDALONE
        //$$ glEnable(GL_BLEND);
        //#elseif MC<12105
        GlStateManager.enableBlend();
        //#endif
    }

    /**
     * @deprecated see {@link #enableTexture2D()}
     */
    @Deprecated
    public static void disableTexture2D() {
        //#if MC>=11904
        //$$ // no-op
        //#else
        GlStateManager.disableTexture2D();
        //#endif

    }

    public static void disableAlpha() {
        //#if MC<11700
        GlStateManager.disableAlpha();
        //#endif
    }

    public static void alphaFunc(int func, float ref) {
        //#if MC<11700
        GlStateManager.alphaFunc(func, ref);
        //#endif
    }

    public static void shadeModel(int mode) {
        //#if MC<11700
        GlStateManager.shadeModel(mode);
        //#endif
    }

    @Deprecated // see UGraphics.Globals
    public static void blendEquation(int equation) {
        //#if STANDALONE
        //$$ org.lwjgl.opengl.GL14.glBlendEquation(equation);
        //#elseif MC>=12105
        //#elseif MC>=10900
        //$$ GlStateManager.glBlendEquation(equation);
        //#else
        org.lwjgl.opengl.GL14.glBlendEquation(equation);
        //#endif
    }

    @Deprecated // see UGraphics.Globals
    public static void tryBlendFuncSeparate(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {
        //#if STANDALONE
        //$$ glBlendFuncSeparate(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
        //#elseif MC<12105
        GlStateManager.tryBlendFuncSeparate(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
        //#endif
    }

    /**
     * @deprecated Relies on the global {@link #setActiveTexture(int) activeTexture} state.<br>
     *     Instead of manually managing TEXTURE_2D state, prefer using
     *     {@link #beginWithDefaultShader(DrawMode, CommonVertexFormats)} or any of the other non-deprecated begin methods as
     *     these will set (and restore) the appropriate state for the given {@link CommonVertexFormats} right before/after
     *     rendering.<br>
     *     Also incompatible with OpenGL Core / MC 1.17.
     */
    @Deprecated
    public static void enableTexture2D() {
        //#if MC>=11904
        //$$ // no-op
        //#else
        GlStateManager.enableTexture2D();
        //#endif
    }

    @Deprecated // see UGraphics.Globals
    public static void disableBlend() {
        //#if STANDALONE
        //$$ glDisable(GL_BLEND);
        //#elseif MC<12105
        GlStateManager.disableBlend();
        //#endif
    }

    public static void deleteTexture(int glTextureId) {
        //#if STANDALONE || MC>=12105
        //$$ GL11.glDeleteTextures(glTextureId);
        //#else
        GlStateManager.deleteTexture(glTextureId);
        //#endif
    }

    public static void enableAlpha() {
        //#if MC<11700
        GlStateManager.enableAlpha();
        //#endif
    }

    public static void configureTexture(int glTextureId, Runnable block) {
        int prevTextureBinding = GL11.glGetInteger(GL_TEXTURE_BINDING_2D);
        //#if STANDALONE
        //$$ glBindTexture(GL_TEXTURE_2D, glTextureId);
        //#elseif MC>=12105
        //$$ GlStateManager._bindTexture(glTextureId);
        //#else
        GlStateManager.bindTexture(glTextureId);
        //#endif

        block.run();

        //#if STANDALONE
        //$$ glBindTexture(GL_TEXTURE_2D, prevTextureBinding);
        //#elseif MC>=12105
        //$$ GlStateManager._bindTexture(prevTextureBinding);
        //#else
        GlStateManager.bindTexture(prevTextureBinding);
        //#endif
    }

    public static void configureTextureUnit(int index, Runnable block) {
        int prevActiveTexture = getActiveTexture();
        setActiveTexture(GL_TEXTURE0 + index);

        block.run();

        setActiveTexture(prevActiveTexture);
    }

    /**
     * @deprecated Changes global state and may as such easily lead to bug if the caller forgets to store the previous
     * state and restore it afterwards.<br>
     * Prefer {@link #configureTextureUnit(int, Runnable)} instead (which only changes the global state for the duration
     * of the given block).<br>
     * If you must change it for longer, then use {@link #getActiveTexture()} before {@link #setActiveTexture(int)} and
     * make sure to restore it afterwards.
     */
    @Deprecated
    public static void activeTexture(int glId) {
        setActiveTexture(glId);
    }

    public static int getActiveTexture() {
        return GL11.glGetInteger(GL_ACTIVE_TEXTURE);
    }

    public static void setActiveTexture(int glId) {
        //#if STANDALONE
        //$$ glActiveTexture(glId);
        //#elseif MC>=11700
        //$$ GlStateManager._activeTexture(glId);
        //#elseif MC>=11400
        //$$ GlStateManager.activeTexture(glId);
        //#else
        GlStateManager.setActiveTexture(glId);
        //#endif
    }

    /**
     * @deprecated Relies on the global {@link #setActiveTexture(int) activeTexture} state.<br>
     * Prefer {@link #bindTexture(int, int)} instead.
     */
    @Deprecated
    public static void bindTexture(int glTextureId) {
        //#if STANDALONE
        //$$ glBindTexture(GL_TEXTURE_2D, glTextureId);
        //#elseif MC>=12109
        //$$ bindTexture(getActiveTexture() - GL_TEXTURE0, glTextureId);
        //#elseif MC>=11700
        //$$ bindTexture(GlStateManager._getActiveTexture() - GL_TEXTURE0, glTextureId);
        //#else
        GlStateManager.bindTexture(glTextureId);
        //#endif
    }

    //#if !STANDALONE
    /**
     * @deprecated Relies on the global {@link #setActiveTexture(int) activeTexture} state.<br>
     * Prefer {@link #bindTexture(int, ResourceLocation)} instead.
     */
    @Deprecated
    public static void bindTexture(ResourceLocation resourceLocation) {
        bindTexture(getOrLoadTextureId(resourceLocation));
    }
    //#endif

    @Deprecated // see UGraphics.Globals
    public static void bindTexture(int index, int glTextureId) {
        //#if STANDALONE
        //$$ configureTextureUnit(index, () -> glBindTexture(GL_TEXTURE_2D, glTextureId));
        //#elseif MC>=12111
        //$$ throw new UnsupportedOperationException("No longer supported on 1.21.11+, use `UBufferBuilder`/`URenderPipeline` instead.");
        //#elseif MC>=12106
        //$$ RenderSystem.setShaderTexture(index, RenderSystem.getDevice().createTextureView(new UnownedGlTexture(glTextureId)));
        //#elseif MC>=12105
        //$$ RenderSystem.setShaderTexture(index, new UnownedGlTexture(glTextureId));
        //#elseif MC>=11700
        //$$ RenderSystem.setShaderTexture(index, glTextureId);
        //#else
        configureTextureUnit(index, () -> GlStateManager.bindTexture(glTextureId));
        //#endif
    }

    //#if !STANDALONE
    @Deprecated // see UGraphics.Globals
    public static void bindTexture(int index, ResourceLocation resourceLocation) {
        bindTexture(index, getOrLoadTextureId(resourceLocation));
    }

    private static int getOrLoadTextureId(ResourceLocation resourceLocation) {
        TextureManager textureManager = UMinecraft.getMinecraft().getTextureManager();
        //#if MC>=11400
        //$$ Texture texture = textureManager.getTexture(resourceLocation);
        //#else
        ITextureObject texture = textureManager.getTexture(resourceLocation);
        //#endif
        if (texture == null) {
            texture = new SimpleTexture(resourceLocation);
            textureManager.loadTexture(resourceLocation, texture);
        }
        //#if MC>=12105 && !STANDALONE
        //$$ return ((GlTexture) texture.getGlTexture()).getGlId();
        //#else
        return texture.getGlTextureId();
        //#endif
    }
    //#endif

    public static int getStringWidth(String in) {
        //#if STANDALONE
        //$$ return (int) MC_FONT.getStringWidth(in, 10f);
        //#else
        return UMinecraft.getFontRenderer().getStringWidth(in);
        //#endif
    }

    public static int getFontHeight() {
        //#if STANDALONE
        //$$ return 9;
        //#else
        return UMinecraft.getFontRenderer().FONT_HEIGHT;
        //#endif
    }

    @Deprecated // Pass UMatrixStack as first arg, required for 1.17+
    public static void drawString(String text, float x, float y, int color, boolean shadow) {
        drawString(UNIT_STACK, text, x, y, color, shadow);
    }

    public static void drawString(UMatrixStack stack, String text, float x, float y, int color, boolean shadow) {
        if ((color >> 24 & 255) <= 10) return;
        //#if STANDALONE
        //$$ MC_FONT.drawString(stack, text, new Color(color), x, y, 10f, 1f, shadow, null);
        //#else
        //#if MC>=12106
        //$$ GlyphDrawerImpl drawerImpl = new GlyphDrawerImpl(stack.peek().getModel());
        //$$ UMinecraft.getFontRenderer().prepare(text, x, y, color, shadow, 0).draw(drawerImpl);
        //$$ drawerImpl.flush();
        //#else
        //#if MC>=11602
        //#if MC>=12100
        //$$ VertexConsumerProvider.Immediate irendertypebuffer$impl = UMinecraft.getMinecraft().getBufferBuilders().getEntityVertexConsumers();
        //#else
        //$$ IRenderTypeBuffer.Impl irendertypebuffer$impl = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
        //#endif
        //$$ UMinecraft.getFontRenderer().renderString(text, x, y, color, shadow, stack.peek().getModel(), irendertypebuffer$impl, TEXT_LAYER_TYPE, 0, 15728880);
        //$$ irendertypebuffer$impl.finish();
        //#else
        if (stack != UNIT_STACK) GL.pushMatrix();
        if (stack != UNIT_STACK) stack.applyToGlobalState();
        //#if MC>=11502
        //$$ if (shadow) {
        //$$     UMinecraft.getFontRenderer().drawStringWithShadow(text, x, y, color);
        //$$ } else {
        //$$     UMinecraft.getFontRenderer().drawString(text, x, y, color);
        //$$ }
        //#else
        UMinecraft.getFontRenderer().drawString(text, x, y, color, shadow);
        //#endif
        if (stack != UNIT_STACK) GL.popMatrix();
        //#endif
        //#endif
        //#endif
    }

    @Deprecated // Pass UMatrixStack as first arg, required for 1.17+
    public static void drawString(String text, float x, float y, int color, int shadowColor) {
        drawString(UNIT_STACK, text, x, y, color, shadowColor);
    }

    public static void drawString(UMatrixStack stack, String text, float x, float y, int color, int shadowColor) {
        if ((color >> 24 & 255) <= 10) return;
        String shadowText = ChatColor.Companion.stripColorCodes(text);
        //#if STANDALONE
        //$$ MC_FONT.drawString(stack, shadowText, new Color(shadowColor), x + 1f, y + 1f, 10f, 1f, false, null);
        //$$ MC_FONT.drawString(stack, text, new Color(color), x, y, 10f, 1f, false, null);
        //#else
        //#if MC>=12106
        //$$ GlyphDrawerImpl drawerImpl = new GlyphDrawerImpl(stack.peek().getModel());
        //$$ UMinecraft.getFontRenderer().prepare(shadowText, x + 1f, y + 1f, shadowColor, false, 0).draw(drawerImpl);
        //$$ drawerImpl.matrix = drawerImpl.matrix.translate(0f, 0f, TextRenderer.FORWARD_SHIFT, new org.joml.Matrix4f());
        //$$ UMinecraft.getFontRenderer().prepare(text, x, y, color, false, 0).draw(drawerImpl);
        //$$ drawerImpl.flush();
        //#else
        //#if MC>=11602
        //#if MC>=12100
        //$$ VertexConsumerProvider.Immediate irendertypebuffer$impl = UMinecraft.getMinecraft().getBufferBuilders().getEntityVertexConsumers();
        //#else
        //$$ IRenderTypeBuffer.Impl irendertypebuffer$impl = IRenderTypeBuffer.getImpl(Tessellator.getInstance().getBuffer());
        //#endif
        //$$ UMinecraft.getFontRenderer().renderString(shadowText, x + 1f, y + 1f, shadowColor, false, stack.peek().getModel(), irendertypebuffer$impl, TEXT_LAYER_TYPE, 0, 15728880);
        //$$ UMinecraft.getFontRenderer().renderString(text, x, y, color, false, stack.peek().getModel(), irendertypebuffer$impl, TEXT_LAYER_TYPE, 0, 15728880);
        //$$ irendertypebuffer$impl.finish();
        //#else
        if (stack != UNIT_STACK) GL.pushMatrix();
        if (stack != UNIT_STACK) stack.applyToGlobalState();
        //#if MC>=11502
        //$$ UMinecraft.getFontRenderer().drawString(shadowText, x + 1f, y + 1f, shadowColor);
        //$$ UMinecraft.getFontRenderer().drawString(text, x, y, color);
        //#else
        UMinecraft.getFontRenderer().drawString(shadowText, x + 1f, y + 1f, shadowColor, false);
        UMinecraft.getFontRenderer().drawString(text, x, y, color, false);
        //#endif
        if (stack != UNIT_STACK) GL.popMatrix();
        //#endif
        //#endif
        //#endif
    }

    //#if MC>=12106 && !STANDALONE
    //$$ private static class GlyphDrawerImpl implements TextRenderer.GlyphDrawer {
        //#if MC >= 26.1
        //$$ private final GpuTextureView lightTexture = Minecraft.getInstance().gameRenderer.lightmap();
        //$$ // lightmap() may return either the uiLightmap, which is 1x1, or the regular lightmap (like pre-26.1)
        //$$ // and the text shader uses texelFetch which ignores the wrapping mode, so we need to pass it 0/0 as the
        //$$ // light coord or it will simply return 0 as the color.
        //$$ // for the 0, see GlyphRenderState.buildVertices
        //$$ // for the 0x00F0_00F0, see e.g. DrawableGizmoPrimitives.Group.renderTexts
        //$$ private final int light = lightTexture.getWidth(0) == 1 ? 0 : 0x00F0_00F0;
        //#else
        //$$ private final GpuTextureView lightTexture = MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().getGlTextureView();
        //$$ private final int light = 0x00F0_00F0; // see GlyphGuiElementRenderState.setupVertices
        //#endif
    //$$     private org.joml.Matrix4f matrix;
    //$$     private RenderPipeline pipeline;
    //$$     private GpuTextureView texture;
    //$$     private BufferBuilder bufferBuilder;
    //$$
    //$$     private GlyphDrawerImpl(org.joml.Matrix4f matrix) {
    //$$         this.matrix = matrix;
    //$$     }
    //$$
    //$$     public void flush() {
    //$$         if (bufferBuilder == null) {
    //$$             return;
    //$$         }
    //$$         RenderPipeline pipeline = this.pipeline;
    //$$         GpuTextureView texture = this.texture;
    //$$         BufferBuilder bufferBuilder = this.bufferBuilder;
    //$$         this.pipeline = null;
    //$$         this.texture = null;
    //$$         this.bufferBuilder = null;
    //$$
    //$$         try (BuiltBuffer builtBuffer = bufferBuilder.endNullable()) {
    //$$             if (builtBuffer == null) return;
                //#if MC >= 26.1
                //$$ GpuTextureView lightTexture = Minecraft.getInstance().gameRenderer.lightmap();
                //#else
                //$$ GpuTextureView lightTexture = MinecraftClient.getInstance().gameRenderer.getLightmapTextureManager().getGlTextureView();
                //#endif
    //$$             try (URenderPass renderPass = new URenderPass()) {
    //$$                 renderPass.draw(UBuiltBuffer.wrap(builtBuffer), URenderPipeline.wrap(pipeline), builder -> {
    //$$                     RenderPass mcRenderPass = ((URenderPass.DrawCallBuilderImpl) builder).getMc();
                        //#if MC>=12111
                        //$$ mcRenderPass.bindTexture("Sampler0", texture, RenderSystem.getSamplerCache().get(FilterMode.NEAREST));
                        //$$ mcRenderPass.bindTexture("Sampler2", lightTexture, RenderSystem.getSamplerCache().get(FilterMode.LINEAR));
                        //#else
                        //$$ mcRenderPass.bindSampler("Sampler0", texture);
                        //$$ mcRenderPass.bindSampler("Sampler2", lightTexture);
                        //#endif
    //$$                     return kotlin.Unit.INSTANCE;
    //$$                 });
    //$$             }
    //$$         }
    //$$     }
    //$$
    //#if MC>=12109
    //$$     private void draw(TextDrawable drawable) {
    //$$         if (pipeline != drawable.getPipeline() || texture != drawable.textureView()) {
    //$$             flush();
    //$$             pipeline = drawable.getPipeline();
    //$$             texture = drawable.textureView();
    //$$             bufferBuilder = Tessellator.getInstance().begin(pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
    //$$         }
    //$$         drawable.render(matrix, bufferBuilder, light, false);
    //$$     }
    //#if MC>=12111
    //$$     @Override public void drawGlyph(TextDrawable.DrawnGlyphRect glyph) { draw(glyph); }
    //#else
    //$$     @Override public void drawGlyph(TextDrawable drawable) { draw(drawable); }
    //#endif
    //$$     @Override public void drawRectangle(TextDrawable drawable) { draw(drawable); }
    //#else
    //$$     private void setupBuffer(BakedGlyph bakedGlyph) {
    //$$         if (pipeline == bakedGlyph.getPipeline() && texture == bakedGlyph.getTexture()) {
    //$$             return;
    //$$         }
    //$$         flush();
    //$$         pipeline = bakedGlyph.getPipeline();
    //$$         texture = bakedGlyph.getTexture();
    //$$         bufferBuilder = Tessellator.getInstance().begin(pipeline.getVertexFormatMode(), pipeline.getVertexFormat());
    //$$     }
    //$$
    //$$     @Override
    //$$     public void drawGlyph(BakedGlyph.DrawnGlyph drawnGlyph) {
    //$$         BakedGlyph bakedGlyph = drawnGlyph.glyph();
    //$$         if (bakedGlyph.getTexture() == null) return;
    //$$         setupBuffer(bakedGlyph);
    //$$         bakedGlyph.draw(drawnGlyph, matrix, bufferBuilder, light, false);
    //$$     }
    //$$
    //$$     @Override
    //$$     public void drawRectangle(BakedGlyph bakedGlyph, BakedGlyph.Rectangle rectangle) {
    //$$         if (bakedGlyph.getTexture() == null) return;
    //$$         bakedGlyph.drawRectangle(rectangle, matrix, bufferBuilder, light, false);
    //$$     }
    //#endif
    //$$ }
    //#endif

    public static List<String> listFormattedStringToWidth(String str, int wrapWidth) {
        return listFormattedStringToWidth(str, wrapWidth, true);
    }

    public static List<String> listFormattedStringToWidth(String str, int wrapWidth, boolean safe) {
        if (safe) {
            String tmp = formattingCodePattern.matcher(str).replaceAll("");
            int max = 0;
            for (String s : tmp.split(" "))
                max = Math.max(max, getStringWidth(s));
            wrapWidth = Math.max(max, wrapWidth);
        }

        //#if STANDALONE
        //$$ throw new UnsupportedOperationException("not implemented");
        //#else
        //#if MC>=11602
        //$$ // TODO: Validate this code
        //$$ List<String> strings = new ArrayList<>();
        //$$
        //$$ CharacterManager charManager = UMinecraft.getFontRenderer().getCharacterManager();
        //#if MC>=11900
        //$$ StringVisitable properties = charManager.trimToWidth(Text.literal(str).fillStyle(EMPTY_WITH_FONT_ID), wrapWidth, Style.EMPTY);
        //#else
        //$$ ITextProperties properties = charManager.func_238358_a_(new StringTextComponent(str).mergeStyle(EMPTY_WITH_FONT_ID), wrapWidth, Style.EMPTY);
        //#endif
        //$$ // From net.minecraft.util.text.ITextProperties line 88
        //$$ properties.getComponent(string -> {
        //$$     strings.add(string);
        //$$     return Optional.empty();
        //$$ });
        //$$ return strings;
        //#else
        return UMinecraft.getFontRenderer().listFormattedStringToWidth(str, wrapWidth);
        //#endif
        //#endif
    }

    public static float getCharWidth(char character) {
        //#if STANDALONE
        //$$ return MC_FONT.getStringWidth(String.valueOf(character), 10f);
        //#elseif MC>=11602
        //$$ return getStringWidth(String.valueOf(character));
        //#else
        return UMinecraft.getFontRenderer().getCharWidth(character); // float because its a float in 1.15+
        //#endif
    }

    public static void clearColor(float r, float g, float b, float a) {
        //#if MC>=12105
        //$$ GL11.glClearColor(r, g, b, a);
        //#else
        GlStateManager.clearColor(r, g, b, a);
        //#endif
    }

    public static void clearDepth(double depth) {
        //#if MC>=12105
        //$$ GL11.glClearDepth(depth);
        //#else
        GlStateManager.clearDepth(depth);
        //#endif
    }

    public static void glClear(int mode) {
        GL11.glClear(mode);
    }

    public static void glClearStencil(int mode) {
        GL11.glClearStencil(mode);
    }

    public static ReleasedDynamicTexture getTexture(InputStream stream) {
        try {
            //#if MC>=11502 && !STANDALONE
            //$$ return new ReleasedDynamicTexture(NativeImage.read(stream));
            //#else
            return new ReleasedDynamicTexture(ImageIO.read(stream));
            //#endif
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("Failed to read image");
    }

    public static ReleasedDynamicTexture getTexture(BufferedImage img) {
        //#if MC>=11502 && !STANDALONE
        //$$ try {
        //$$     ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //$$     ImageIO.write(img, "png", baos );
        //$$     return new ReleasedDynamicTexture(NativeImage.read(new ByteArrayInputStream(baos.toByteArray())));
        //$$ } catch (IOException e) {
        //$$     e.printStackTrace();
        //$$ }
        //$$ throw new IllegalStateException("Failed to create texture");
        //#else
        return new ReleasedDynamicTexture(img);
        //#endif
    }

    public static ReleasedDynamicTexture getEmptyTexture() {
        return new ReleasedDynamicTexture(0, 0);
    }

    public static void glUseProgram(int program) {
        //#if STANDALONE
        //$$ GL20.glUseProgram(program);
        //#elseif MC>=11502
        //$$ GlStateManager.useProgram(program);
        //#else
        OpenGlHelper.glUseProgram(program);
        //#endif
    }

    public static boolean isOpenGl21Supported() {
        //#if MC>=11502
        //$$ return true;
        //#else
        return OpenGlHelper.openGL21;
        //#endif
    }

    public static boolean areShadersSupported() {
        //#if MC>=11502
        //$$ return true;
        //#else
        return OpenGlHelper.areShadersSupported();
        //#endif
    }

    public static int glCreateProgram() {
        //#if STANDALONE
        //$$ return GL20.glCreateProgram();
        //#elseif MC>=11502
        //$$ return GlStateManager.createProgram();
        //#else
        return OpenGlHelper.glCreateProgram();
        //#endif
    }

    public static int glCreateShader(int type) {
        //#if STANDALONE
        //$$ return GL20.glCreateShader(type);
        //#elseif MC>=11502
        //$$ return GlStateManager.createShader(type);
        //#else
        return OpenGlHelper.glCreateShader(type);
        //#endif
    }

    public static void glCompileShader(int shaderIn) {
        //#if STANDALONE
        //$$ GL20.glCompileShader(shaderIn);
        //#elseif MC>=11502
        //$$ GlStateManager.compileShader(shaderIn);
        //#else
        OpenGlHelper.glCompileShader(shaderIn);
        //#endif
    }

    public static int glGetShaderi(int shaderIn, int pname) {
        //#if STANDALONE
        //$$ return GL20.glGetShaderi(shaderIn, pname);
        //#elseif MC>=11502
        //$$ return GlStateManager.getShader(shaderIn,pname);
        //#else
        return OpenGlHelper.glGetShaderi(shaderIn, pname);
        //#endif
    }

    public static String glGetShaderInfoLog(int shader, int maxLen) {
        //#if STANDALONE
        //$$ return GL20.glGetShaderInfoLog(shader, maxLen);
        //#elseif MC>=11502
        //$$ return GlStateManager.getShaderInfoLog( shader,maxLen);
        //#else
        return OpenGlHelper.glGetShaderInfoLog(shader, maxLen);
        //#endif
    }

    public static void glAttachShader(int program, int shaderIn) {
        //#if STANDALONE
        //$$ GL20.glAttachShader(program, shaderIn);
        //#elseif MC>=11502
        //$$ GlStateManager.attachShader(program,shaderIn);
        //#else
        OpenGlHelper.glAttachShader(program, shaderIn);
        //#endif
    }

    public static void glLinkProgram(int program) {
        //#if STANDALONE
        //$$ GL20.glLinkProgram(program);
        //#elseif MC>=11502
        //$$ GlStateManager.linkProgram(program);
        //#else
        OpenGlHelper.glLinkProgram(program);
        //#endif
    }

    public static int glGetProgrami(int program, int pname) {
        //#if STANDALONE
        //$$ return GL20.glGetProgrami(program, pname);
        //#elseif MC>=11502
        //$$ return GlStateManager.getProgram(program,pname);
        //#else
        return OpenGlHelper.glGetProgrami(program, pname);
        //#endif
    }

    public static String glGetProgramInfoLog(int program, int maxLen) {
        //#if STANDALONE
        //$$ return GL20.glGetProgramInfoLog(program, maxLen);
        //#elseif MC>=11502
        //$$ return GlStateManager.getProgramInfoLog(program, maxLen);
        //#else
        return OpenGlHelper.glGetProgramInfoLog(program, maxLen);
        //#endif
    }

    public static void color4f(float red, float green, float blue, float alpha) {
        //#if STANDALONE
        //$$ throw new UnsupportedOperationException();
        //#elseif MC<12106
        GlStateManager.color(red, green, blue, alpha);
        //#endif
    }

    public static void directColor3f(float red, float green, float blue) {
        //#if MC>=11700
        //$$ color4f(red, green, blue, 1f);
        //#else
        GlStateManager.color(red, green, blue);
        //#endif
    }

    @Deprecated // see UGraphics.Globals
    public static void enableDepth() {
        //#if STANDALONE
        //$$ glEnable(GL_DEPTH_TEST);
        //#elseif MC<12105
        GlStateManager.enableDepth();
        //#endif
    }

    @Deprecated // see UGraphics.Globals
    public static void depthFunc(int mode) {
        //#if STANDALONE
        //$$ glDepthFunc(mode);
        //#elseif MC<12105
        GlStateManager.depthFunc(mode);
        //#endif
    }

    @Deprecated // see UGraphics.Globals
    public static void depthMask(boolean flag) {
        //#if STANDALONE
        //$$ glDepthMask(flag);
        //#elseif MC<12105
        GlStateManager.depthMask(flag);
        //#endif
    }

    @Deprecated // see UGraphics.Globals
    public static void disableDepth() {
        //#if STANDALONE
        //$$ glDisable(GL_DEPTH_TEST);
        //#elseif MC<12105
        GlStateManager.disableDepth();
        //#endif
    }

    public static void enableScissor(int x, int y, int width, int height) {
        new ScissorState(true, x, y, width, height).activate();
    }

    public static void disableScissor() {
        new ScissorState(false, 0, 0, 0, 0).activate();
    }

    //#if MC>=11700 && MC<12105 && !STANDALONE
    //$$ @Deprecated // see UGraphics.Globals
    //$$ public static void setShader(Supplier<Shader> shader) {
        //#if MC>=12102
        //$$ RenderSystem.setShader(shader.get());
        //#else
        //$$ RenderSystem.setShader(shader);
        //#endif
    //$$ }
    //#endif

    public enum DrawMode {
        LINES(GL11.GL_LINES),
        /**
         * @deprecated No longer properly supported as of 1.21.11, use {@link #LINES} instead
         */
        @Deprecated
        LINE_STRIP(GL11.GL_LINE_STRIP),
        TRIANGLES(GL11.GL_TRIANGLES),
        TRIANGLE_STRIP(GL11.GL_TRIANGLE_STRIP),
        TRIANGLE_FAN(GL11.GL_TRIANGLE_FAN),
        QUADS(GL11.GL_QUADS),
        ;

        public final int glMode;
        //#if !STANDALONE
        //#if MC>=11700
        //$$ public final VertexFormat.DrawMode mcMode;
        //#else
        public final int mcMode;
        //#endif
        //#endif

        DrawMode(int glMode) {
            this.glMode = glMode;
            //#if !STANDALONE
            //#if MC>=11700
            //$$ this.mcMode = glToMcDrawMode(glMode);
            //#else
            this.mcMode = glMode;
            //#endif
            //#endif
        }

        //#if MC>=11700 && !STANDALONE
        //$$ private static VertexFormat.DrawMode glToMcDrawMode(int glMode) {
        //$$     switch (glMode) {
        //$$         case GL11.GL_LINES: return VertexFormat.DrawMode.LINES;
                //#if MC>=12111
                //$$ case GL11.GL_LINE_STRIP: return VertexFormat.DrawMode.DEBUG_LINE_STRIP;
                //#else
                //$$ case GL11.GL_LINE_STRIP: return VertexFormat.DrawMode.LINE_STRIP;
                //#endif
        //$$         case GL11.GL_TRIANGLES: return VertexFormat.DrawMode.TRIANGLES;
        //$$         case GL11.GL_TRIANGLE_STRIP: return VertexFormat.DrawMode.TRIANGLE_STRIP;
        //$$         case GL11.GL_TRIANGLE_FAN: return VertexFormat.DrawMode.TRIANGLE_FAN;
        //$$         case GL11.GL_QUADS: return VertexFormat.DrawMode.QUADS;
        //$$         default: throw new IllegalArgumentException("Unsupported draw mode " + glMode);
        //$$     }
        //$$ }
        //$$
        //$$ private static DrawMode fromMc(VertexFormat.DrawMode mcMode) {
        //$$     switch (mcMode) {
        //$$         case LINES: return DrawMode.LINES;
                //#if MC>=12111
                //$$ case DEBUG_LINE_STRIP: return DrawMode.LINE_STRIP;
                //#else
                //$$ case LINE_STRIP: return DrawMode.LINE_STRIP;
                //#endif
        //$$         case TRIANGLES: return DrawMode.TRIANGLES;
        //$$         case TRIANGLE_STRIP: return DrawMode.TRIANGLE_STRIP;
        //$$         case TRIANGLE_FAN: return DrawMode.TRIANGLE_FAN;
        //$$         case QUADS: return DrawMode.QUADS;
        //$$         default: throw new IllegalArgumentException("Unsupported draw mode " + mcMode);
        //$$     }
        //$$ }
        //#endif

        public static DrawMode fromGl(int glMode) {
            switch (glMode) {
                case GL11.GL_LINES: return LINES;
                case GL11.GL_LINE_STRIP: return LINE_STRIP;
                case GL11.GL_TRIANGLES: return TRIANGLES;
                case GL11.GL_TRIANGLE_STRIP: return TRIANGLE_STRIP;
                case GL11.GL_TRIANGLE_FAN: return TRIANGLE_FAN;
                case GL11.GL_QUADS: return QUADS;
                default: throw new IllegalArgumentException("Unsupported draw mode " + glMode);
            }
        }

        //#if MC>=11600 && !STANDALONE
        //$$ public static DrawMode fromRenderLayer(RenderType renderLayer) {
            //#if MC>=11700
            //$$ return fromMc(renderLayer.getDrawMode());
            //#else
            //$$ return fromGl(renderLayer.getDrawMode());
            //#endif
        //$$ }
        //#endif
    }

    public enum CommonVertexFormats {
        POSITION(DefaultVertexFormats.POSITION),
        POSITION_COLOR(DefaultVertexFormats.POSITION_COLOR),
        POSITION_TEXTURE(DefaultVertexFormats.POSITION_TEX),
        POSITION_TEXTURE_COLOR(DefaultVertexFormats.POSITION_TEX_COLOR),
        POSITION_COLOR_TEXTURE_LIGHT(DefaultVertexFormats.BLOCK),
        /**
         * @deprecated Minecraft removed the built-in shader for this vertex format in 1.20.5, so it is no
         * longer universal across all versions.
         */
        @Deprecated
        POSITION_TEXTURE_LIGHT_COLOR(DefaultVertexFormats.POSITION_TEX_LMAP_COLOR),
        POSITION_TEXTURE_COLOR_LIGHT(DefaultVertexFormats.PARTICLE_POSITION_TEX_COLOR_LMAP),
        /**
         * @deprecated Minecraft removed the built-in shader for this vertex format in 1.20.5, so it is no
         * longer universal across all versions.
         */
        @Deprecated
        POSITION_TEXTURE_COLOR_NORMAL(DefaultVertexFormats.POSITION_TEX_COLOR_NORMAL),
        ;

        public final VertexFormat mc;

        CommonVertexFormats(VertexFormat mc) {
            this.mc = mc;
        }
    }

    @Deprecated // see UGraphics.Globals
    public UGraphics beginWithActiveShader(DrawMode mode, CommonVertexFormats format) {
        //#if STANDALONE
        //$$ vertexFormat = format;
        //$$ drawMode = mode;
        //$$ instance = new BufferBuilder(format.mc.getParts());
        //$$ shader = null;
        //$$ return this;
        //#else
        return beginWithActiveShader(mode, format.mc);
        //#endif
    }

    //#if !STANDALONE
    @Deprecated // see UGraphics.Globals
    public UGraphics beginWithActiveShader(DrawMode mode, VertexFormat format) {
        return beginInternal(mode, format);
    }
    private UGraphics beginInternal(DrawMode mode, VertexFormat format) {
        vertexFormat = format;
        //#if MC>=12100
        //$$ instance = getTessellator().begin(mode.mcMode, format);
        //#else
        instance.begin(mode.mcMode, format);
        //#endif
        return this;
    }
    //#endif

    //#if STANDALONE
    //#elseif MC>=12105
    //$$ @ApiStatus.Internal
    //$$ public static final Map<VertexFormat, String> DEFAULT_SHADERS = new HashMap<>();
    //$$ static {
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR_NORMAL, "core/rendertype_lines");
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, "core/particle");
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION, "core/position");
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR, "core/position_color");
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR_LIGHT, "core/position_color_lightmap");
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE, "core/position_tex");
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE_COLOR, "core/position_tex_color");
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, "core/position_color_tex_lightmap");
    //$$ }
    //#elseif MC>=12102
    //$$ @ApiStatus.Internal
    //$$ public static final Map<VertexFormat, ShaderProgramKey> DEFAULT_SHADERS = new HashMap<>();
    //$$ static {
    //$$     DEFAULT_SHADERS.put(VertexFormats.LINES, ShaderProgramKeys.RENDERTYPE_LINES);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, ShaderProgramKeys.PARTICLE);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION, ShaderProgramKeys.POSITION);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR, ShaderProgramKeys.POSITION_COLOR);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR_LIGHT, ShaderProgramKeys.POSITION_COLOR_LIGHTMAP);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE, ShaderProgramKeys.POSITION_TEX);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE_COLOR, ShaderProgramKeys.POSITION_TEX_COLOR);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, ShaderProgramKeys.POSITION_COLOR_TEX_LIGHTMAP);
    //$$ }
    //#elseif MC>=11700
    //$$ // Note: Needs to be an Identity hash map because VertexFormat's equals method is broken (compares via its
    //$$ //       component Map but order very much matters for VertexFormat) as of 1.17
    //$$ @ApiStatus.Internal
    //$$ public static final Map<VertexFormat, Supplier<Shader>> DEFAULT_SHADERS = new IdentityHashMap<>();
    //$$ static {
    //$$     DEFAULT_SHADERS.put(VertexFormats.LINES, GameRenderer::getRenderTypeLinesShader);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE_COLOR_LIGHT, GameRenderer::getParticleShader);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION, GameRenderer::getPositionShader);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR, GameRenderer::getPositionColorShader);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR_LIGHT, GameRenderer::getPositionColorLightmapShader);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE, GameRenderer::getPositionTexShader);
    //#if MC>=12100
    //$$     // Shader for this format is no longer provided.
    //#else
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR_TEXTURE, GameRenderer::getPositionColorTexShader);
    //#endif
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE_COLOR, GameRenderer::getPositionTexColorShader);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_COLOR_TEXTURE_LIGHT, GameRenderer::getPositionColorTexLightmapShader);
    //#if MC>=12005
    //$$     // Shaders for these formats are no longer provided.
    //#else
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE_LIGHT_COLOR, GameRenderer::getPositionTexLightmapColorShader);
    //$$     DEFAULT_SHADERS.put(VertexFormats.POSITION_TEXTURE_COLOR_NORMAL, GameRenderer::getPositionTexColorNormalShader);
    //#endif
    //$$ }
    //#endif

    @Deprecated // see UGraphics.Globals
    public UGraphics beginWithDefaultShader(DrawMode mode, CommonVertexFormats format) {
        //#if STANDALONE
        //$$ beginWithActiveShader(mode, format);
        //$$ shader = DefaultShader.Companion.get(format.mc.getParts());
        //$$ return this;
        //#else
        return beginWithDefaultShader(mode, format.mc);
        //#endif
    }

    //#if !STANDALONE
    @Deprecated // see UGraphics.Globals
    public UGraphics beginWithDefaultShader(DrawMode mode, VertexFormat format) {
        //#if MC>=11700 && MC<12105
        //#if MC>=12102
        //$$ ShaderProgramKey shader = DEFAULT_SHADERS.get(format);
        //#else
        //$$ Supplier<Shader> shader = DEFAULT_SHADERS.get(format);
        //#endif
        //$$ if (shader == null) {
        //$$     throw new IllegalArgumentException("No default shader for " + format + ". Bind your own and use beginWithActiveShader instead.");
        //$$ }
        //$$
        //$$ RenderSystem.setShader(shader);
        //#endif
        return beginWithActiveShader(mode, format);
    }

    //#if MC>=11600
    //$$ private RenderType renderLayer;
    //$$ public UGraphics beginRenderLayer(RenderType renderLayer) {
    //$$     this.renderLayer = renderLayer;
    //$$     beginInternal(DrawMode.fromRenderLayer(renderLayer), renderLayer.getVertexFormat());
    //$$     return this;
    //$$ }
    //#endif

    @Deprecated // use `beginWithDefaultShader` or `beginWithActiveShader` or `beginRenderLayer` instead
    public UGraphics begin(int glMode, CommonVertexFormats format) {
        return begin(glMode, format.mc);
    }

    @Deprecated // use `beginWithDefaultShader` or `beginWithActiveShader` or `beginRenderLayer` instead
    public UGraphics begin(int glMode, VertexFormat format) {
        //#if MC>=11700
        //$$ beginWithDefaultShader(DrawMode.fromGl(glMode), format);
        //#else
        instance.begin(glMode, format);
        //#endif
        return this;
    }
    //#endif

    public void drawDirect() {
        //#if STANDALONE
        //$$ doDraw();
        //#else
        //#if MC>=12100
        //$$ BuiltBuffer builtBuffer = instance.endNullable();
        //$$ if (builtBuffer == null) return;
        //#endif
        //#if MC>=11600
        //$$ if (renderLayer != null) {
            //#if MC>=12100
            //$$ renderLayer.draw(builtBuffer);
            //#elseif MC>=12000
            //$$ renderLayer.draw(instance, RenderSystem.getVertexSorting());
            //#else
            //$$ renderLayer.finish(instance, 0, 0, 0);
            //#endif
        //$$     return;
        //$$ }
        //#endif
        doDraw(
            //#if MC>=12100
            //$$ builtBuffer
            //#endif
        );
        //#endif
    }

    public void drawSorted(int cameraX, int cameraY, int cameraZ) {
        //#if STANDALONE
        //$$ // TODO sorting, if we ever need it
        //$$ doDraw();
        //#else
        //#if MC>=12100
        //$$ BuiltBuffer builtBuffer = instance.endNullable();
        //$$ if (builtBuffer == null) return;
        //#if MC>=12102
        //$$ builtBuffer.sortQuads(SORTED_QUADS_ALLOCATOR, RenderSystem.getProjectionType().getVertexSorter());
        //#else
        //$$ builtBuffer.sortQuads(SORTED_QUADS_ALLOCATOR, RenderSystem.getVertexSorting());
        //#endif
        //#endif
        //#if MC>=11600
        //$$ if (renderLayer != null) {
            //#if MC>=12100
            //$$ renderLayer.draw(builtBuffer);
            //#elseif MC>=12000
            //$$ renderLayer.draw(instance, RenderSystem.getVertexSorting());
            //#else
            //$$ renderLayer.finish(instance, cameraX, cameraY, cameraZ);
            //#endif
        //$$     return;
        //$$ }
        //#endif
        //#if MC>=12100
        //$$ // Sorting handled above.
        //#elseif MC>=12000
        //$$ instance.setSorter(RenderSystem.getVertexSorting());
        //#elseif MC>=11700
        //$$ instance.setCameraPosition(cameraX, cameraY, cameraZ);
        //#else
        instance.sortVertexData(cameraX, cameraY, cameraZ);
        //#endif
        doDraw(
            //#if MC>=12100
            //$$ builtBuffer
            //#endif
        );
        //#endif
    }

    //#if MC<11700
    private static boolean[] getDesiredTextureUnitState(VertexFormat vertexFormat) {
        // Vanilla only ever has two UV elements, so we can assume the remainder to be disabled by default and don't
        // need to check them unless we want them enabled.
        boolean[] wantEnabled = new boolean[2];
        for (VertexFormatElement element : vertexFormat.getElements()) {
            if (element.getUsage() == VertexFormatElement.EnumUsage.UV) {
                int index = element.getIndex();
                if (wantEnabled.length <= index) {
                    wantEnabled = Arrays.copyOf(wantEnabled, index + 1);
                }
                wantEnabled[index] = true;
            }
        }
        return wantEnabled;
    }
    //#endif

    //#if STANDALONE
    //$$ private void doDraw() {
    //$$     CommonVertexFormats vertexFormat = this.vertexFormat;
    //$$     if (vertexFormat == null) {
    //$$         throw new IllegalStateException("Must call `begin` before `draw`.");
    //$$     }
    //$$     RENDERER.draw(instance, drawMode, shader);
    //$$ }
    //#else
    private void doDraw(
        //#if MC>=12100
        //$$ BuiltBuffer builtBuffer
        //#endif
    ) {
        //#if MC>=12105 && !STANDALONE
        //$$ throw new UnsupportedOperationException("Drawing via UGraphics on 1.21.5+ is only supported via `beginRenderLayer`. Use that or `UBufferBulider`/`URenderPipeline` instead.");
        //#else
        VertexFormat vertexFormat = this.vertexFormat;
        if (vertexFormat == null) {
            //#if MC>=12100
            //$$ BufferRenderer.drawWithGlobalProgram(builtBuffer);
            //#else
            getTessellator().draw();
            //#endif
            return;
        }

        //#if MC<11700
        boolean[] wantEnabledStates = getDesiredTextureUnitState(vertexFormat);
        boolean[] wasEnabledStates = new boolean[wantEnabledStates.length];
        for (int i = 0; i < wasEnabledStates.length; i++) {
            final int index = i;
            configureTextureUnit(index, () -> {
                boolean isEnabled = wasEnabledStates[index] = GL11.glIsEnabled(GL11.GL_TEXTURE_2D);
                boolean wantEnabled = wantEnabledStates[index];
                if (isEnabled != wantEnabled) {
                    if (wantEnabled) {
                        GlStateManager.enableTexture2D();
                    } else {
                        GlStateManager.disableTexture2D();
                    }
                }
            });
        }
        //#endif

        //#if MC>=12100
        //$$ BufferRenderer.drawWithGlobalProgram(builtBuffer);
        //#else
        getTessellator().draw();
        //#endif

        //#if MC<11700
        for (int i = 0; i < wasEnabledStates.length; i++) {
            if (wasEnabledStates[i] == wantEnabledStates[i]) {
                continue;
            }
            if (wasEnabledStates[i]) {
                configureTextureUnit(i, GlStateManager::enableTexture2D);
            } else {
                configureTextureUnit(i, GlStateManager::disableTexture2D);
            }
        }
        //#endif
        //#endif
    }
    //#endif

    @Deprecated // Pass UMatrixStack as first arg, required for 1.17+
    public UGraphics pos(double x, double y, double z) {
        return pos(UNIT_STACK, x, y, z);
    }

    public UGraphics pos(UMatrixStack stack, double x, double y, double z) {
        //#if STANDALONE
        //$$ asUVertexConsumer().pos(stack, x, y, z);
        //#else
        if (stack == UNIT_STACK) {
            //#if MC>=12100
            //$$ instance.vertex((float) x, (float) y, (float) z);
            //#else
            instance.pos(x, y, z);
            //#endif
        } else {
            //#if MC>=11602
            //$$ instance.pos(stack.peek().getModel(), (float) x, (float) y, (float) z);
            //#else
            Vector4f vec = new Vector4f((float) x, (float) y, (float) z, 1f);
            //#if MC>=11400
            //$$ vec.transform(stack.peek().getModel());
            //#else
            Matrix4f.transform(stack.peek().getModel(), vec, vec);
            //#endif
            instance.pos(vec.getX(), vec.getY(), vec.getZ());
            //#endif
        }
        //#endif
        return this;
    }

    @Deprecated // Pass UMatrixStack as first arg, required for 1.17+
    public UGraphics norm(float x, float y, float z) {
        return norm(UNIT_STACK, x, y, z);
    }

    public UGraphics norm(UMatrixStack stack, float x, float y, float z) {
        //#if STANDALONE
        //$$ asUVertexConsumer().norm(stack, x, y, z);
        //#else
        if (stack == UNIT_STACK) {
            instance.normal(x, y, z);
        } else {
            //#if MC>=12005
            //$$ Vector3f normal = stack.peek().getNormal().transform(x, y, z, new Vector3f());
            //$$ instance.normal(normal.x(), normal.y(), normal.z());
            //#elseif MC>=11602
            //$$ instance.normal(stack.peek().getNormal(), x, y, z);
            //#else
            Vector3f vec = new Vector3f(x, y, z);
            //#if MC>=11400
            //$$ vec.transform(stack.peek().getNormal());
            //#else
            Matrix3f.transform(stack.peek().getNormal(), vec, vec);
            //#endif
            instance.normal(vec.getX(), vec.getY(), vec.getZ());
            //#endif
        }
        //#endif
        return this;
    }

    public UGraphics color(int red, int green, int blue, int alpha) {
        return color(red / 255f, green / 255f, blue / 255f, alpha / 255f);
    }

    public UGraphics color(float red, float green, float blue, float alpha) {
        //#if STANDALONE
        //$$ asUVertexConsumer().color(red, green, blue, alpha);
        //#else
        instance.color(red, green, blue, alpha);
        //#endif
        return this;
    }

    public UGraphics color(Color color) {
        return color(color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public UGraphics endVertex() {
        //#if STANDALONE
        //$$ instance.endVertex();
        //#elseif MC<12100
        instance.endVertex();
        //#endif
        return this;
    }

    public UGraphics tex(double u, double v) {
        //#if MC>=11502 && !STANDALONE
        //$$ instance.tex((float)u,(float)v);
        //#else
        instance.tex(u, v);
        //#endif
        return this;
    }

    public UGraphics overlay(int u, int v) {
        //#if MC>=11502 && !STANDALONE
        //$$ instance.overlay(u, v);
        //#else
        instance.tex(u, v);
        //#endif
        return this;
    }

    public UGraphics light(int u, int v) {
        //#if STANDALONE
        //$$ asUVertexConsumer().light(u, v);
        //#else
        instance.lightmap(u, v);
        //#endif
        return this;
    }

    //#if MC>=12105 && MC<12111
    //$$ private static class UnownedGlTexture extends GlTexture {
    //$$     public UnownedGlTexture(int glId) {
            //#if MC>=12106
            //$$ super(USAGE_TEXTURE_BINDING, "", TextureFormat.RGBA8, 0, 0, 0, 1, glId);
            //#else
            //$$ super("", TextureFormat.RGBA8, 0, 0, 0, glId);
            //#endif
    //$$         this.needsReinit = false;
    //$$     }
    //$$ }
    //#endif

    /**
     * Using UMatrixStack should be preferred for all versions as direct GL transforms will break in 1.17.
     *
     * These methods are no different than transformation methods in the UGraphics class except they are not deprecated
     * and as such can be used in version-specific code.
     */
    //#if MC<11700
    public static class GL {
        public static void pushMatrix() {
            GlStateManager.pushMatrix();
        }

        public static void popMatrix() {
            GlStateManager.popMatrix();
        }

        public static void translate(float x, float y, float z) {
            GlStateManager.translate(x, y, z);
        }

        public static void translate(double x, double y, double z) {
            GlStateManager.translate(x, y, z);
        }

        public static void rotate(float angle, float x, float y, float z) {
            GlStateManager.rotate(angle, x, y, z);
        }

        public static void scale(float x, float y, float z) {
            GlStateManager.scale(x, y, z);
        }

        public static void scale(double x, double y, double z) {
            GlStateManager.scale(x, y, z);
        }
    }
    //#endif

    /**
     * Minecraft 1.21.5 switches to a more Vulkan-style rendering, that is, almost all state is now specified directly
     * as part of the draw call and most global OpenGL state is no longer useful because it is overwritten right before
     * each draw call.
     * <p>
     * The recommended replacement is to use {@link gg.essential.universal.render.URenderPipeline}
     * (via {@link gg.essential.universal.vertex.UBufferBuilder}) instead, which provides the same
     * all-state-is-declared-explicitly system for all versions.<br>
     * Note that unlike the vanilla {@code RenderLayer} system, which still uses the global
     * {@code RenderSystem.setShaderTexture}, but just like the vanilla {@code RenderPipeline},
     * {@code URenderPipeline} also requires textures to be set explicitly, despite {@code UGraphics.bindTexture} not
     * yet being deprecated (because it's still used for {@code RenderLayer}).
     * <br>
     * Update: As of 1.21.11, {@code UGraphics.bindTexture} is now deprecated too.
     * <p>
     * If you need to still use the old global state on versions prior to 1.21.5, you may use the methods declared in
     * this class. They are functionally identical to the ones in UGraphics but are not deprecated with the
     * understanding that they will only be used in explicitly version-dependent code.
     */
    public static class Globals {
        //#if MC<12105 || STANDALONE
        public static void cullFace(int mode) {
            UGraphics.cullFace(mode);
        }

        public static void enableBlend() {
            UGraphics.enableBlend();
        }

        public static void disableBlend() {
            UGraphics.disableBlend();
        }

        public static void blendEquation(int equation) {
            UGraphics.blendEquation(equation);
        }

        public static void blendFunc(int srcFactor, int dstFactor, int srcFactorAlpha, int dstFactorAlpha) {
            UGraphics.tryBlendFuncSeparate(srcFactor, dstFactor, srcFactorAlpha, dstFactorAlpha);
        }

        public static void blendState(BlendState state) {
            state.activate();
        }

        public static void enableDepth() {
            UGraphics.enableDepth();
        }

        public static void disableDepth() {
            UGraphics.disableDepth();
        }

        public static void depthFunc(int mode) {
            UGraphics.depthFunc(mode);
        }

        public static void depthMask(boolean flag) {
            UGraphics.depthMask(flag);
        }

        //#if MC>=11700 && !STANDALONE
        //$$ public static void setShader(Supplier<Shader> shader) {
            //#if MC>=12102
            //$$ RenderSystem.setShader(shader.get());
            //#else
            //$$ RenderSystem.setShader(shader);
            //#endif
        //$$ }
        //#endif

        public static UGraphics beginWithActiveShader(DrawMode mode, CommonVertexFormats format) {
            UGraphics instance = UGraphics.getFromTessellator();
            instance.beginWithActiveShader(mode, format);
            return instance;
        }

        //#if !STANDALONE
        public static UGraphics beginWithActiveShader(DrawMode mode, VertexFormat format) {
            UGraphics instance = UGraphics.getFromTessellator();
            instance.beginWithActiveShader(mode, format);
            return instance;
        }
        //#endif

        public static UGraphics beginWithDefaultShader(DrawMode mode, CommonVertexFormats format) {
            UGraphics instance = UGraphics.getFromTessellator();
            instance.beginWithDefaultShader(mode, format);
            return instance;
        }

        //#if !STANDALONE
        public UGraphics beginWithDefaultShader(DrawMode mode, VertexFormat format) {
            UGraphics instance = UGraphics.getFromTessellator();
            instance.beginWithDefaultShader(mode, format);
            return instance;
        }
        //#endif
        //#endif

        //#if MC<12111 || STANDALONE
        public static void bindTexture(int index, int glTextureId) {
            UGraphics.bindTexture(index, glTextureId);
        }

        //#if !STANDALONE
        public static void bindTexture(int index, ResourceLocation resourceLocation) {
            UGraphics.bindTexture(index, resourceLocation);
        }
        //#endif
        //#endif
    }
}
