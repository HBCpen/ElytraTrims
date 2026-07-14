package dev.kikugie.elytratrims.api.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import java.util.concurrent.atomic.AtomicInteger;

public class ETRenderParameters implements Cloneable {
    public final AtomicInteger order;

    public Model<Object> model;
    public Object state;
    public ItemStack stack;
    public PoseStack matrices;
    public RenderType renderType;
    public Identifier baseTexture;
    public int lightCoords;
    public int tintedColor;
    public int outlineColor;

    // =#= Unstable territory =#=
    //~ if >=26.3 TextureAtlasSprite -> UvMapping
    public net.minecraft.client.renderer.texture.UvMapping sprite;
    //? if >=26.3
    public net.minecraft.client.resources.palette.PalettedTextureManager.Handle trimTextureHandle;

    public ETRenderParameters(int order) {
        this.order = new AtomicInteger(order);
    }

    @Override
    public ETRenderParameters clone() {
        try {
            ETRenderParameters clone = (ETRenderParameters) super.clone();
            clone.model = this.model;
            clone.state = this.state;
            clone.matrices = this.matrices;
            clone.renderType = this.renderType;
            clone.baseTexture = this.baseTexture;
            clone.lightCoords = this.lightCoords;
            clone.tintedColor = this.tintedColor;
            clone.outlineColor = this.outlineColor;
            clone.sprite = this.sprite;
            //? if >=26.3
            clone.trimTextureHandle = this.trimTextureHandle;
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
