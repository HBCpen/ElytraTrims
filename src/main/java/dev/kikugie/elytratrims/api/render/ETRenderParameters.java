package dev.kikugie.elytratrims.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicInteger;

public class ETRenderParameters implements Cloneable {
    public AtomicInteger order;

    public Model<Object> model;
    public Object state;
    public ItemStack stack;
    public PoseStack matrices;
    public RenderType renderType;
    public Identifier baseTexture;
    public int lightCoords;
    public int overlayCoords = OverlayTexture.NO_OVERLAY;
    public int tintedColor = -1;
    public int outlineColor;

    // =#= Unstable territory =#=
    //~ if >=26.3 TextureAtlasSprite -> UvMapping
    public net.minecraft.client.renderer.texture.UvMapping sprite;
    //? if >=26.3
    public net.minecraft.client.resources.palette.PalettedTextureManager.Handle trimTextureHandle;

    public ETRenderParameters(int order) {
        this.order = new AtomicInteger(order);
    }

    @Deprecated // The new API modifies provided parameters in-place
    public ETRenderParameters(
        Model<Object> model, Object state, ItemStack stack, PoseStack matrices, RenderType renderType, Identifier baseTexture,
        int lightCoords, int tintedColor, int overlayCoords, int outlineColor, AtomicInteger order
    ) {
        this.model = model;
        this.state = state;
        this.stack = stack;
        this.matrices = matrices;
        this.renderType = renderType;
        this.baseTexture = baseTexture;
        this.lightCoords = lightCoords;
        this.tintedColor = tintedColor;
        this.overlayCoords = overlayCoords;
        this.outlineColor = outlineColor;
        this.order = order;
    }

    @Override
    public ETRenderParameters clone() {
        try {
            ETRenderParameters clone = (ETRenderParameters) super.clone();
            clone.copyFrom(this);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public void copyFrom(ETRenderParameters other) {
        order = other.order;
        model = other.model;
        state = other.state;
        matrices = other.matrices;
        renderType = other.renderType;
        baseTexture = other.baseTexture;
        lightCoords = other.lightCoords;
        overlayCoords = other.overlayCoords;
        tintedColor = other.tintedColor;
        outlineColor = other.outlineColor;
        sprite = other.sprite;
        //? if >=26.3
        trimTextureHandle = other.trimTextureHandle;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public Model<Object> model() {
        return model;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public Object object() {
        return state;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public ItemStack stack() {
        return stack;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public PoseStack matrices() {
        return matrices;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public RenderType render() {
        return renderType;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public Identifier texture() {
        return baseTexture;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public int light() {
        return lightCoords;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public int color() {
        return tintedColor;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public int overlay() {
        return overlayCoords;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public int outline() {
        return outlineColor;
    }

    @Deprecated // The new API modifies provided parameters in-place
    public AtomicInteger order() {
        return order;
    }
}
