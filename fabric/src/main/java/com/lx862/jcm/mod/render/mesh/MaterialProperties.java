package com.lx862.jcm.mod.render.mesh;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * Properties regarding a mesh material. Set during model building; the MTR
 * 4.1 engine uses {@link ShaderType#toRenderStage()} plus the texture to
 * batch uploaded meshes, so this remains the mesh map key of a
 * {@link RawModel}.
 */
public final class MaterialProperties {

        /**
         * The texture to use. Null disables texture.
         */
        private ResourceLocation texture;

        public final ShaderType shaderType;

        @Nullable public Integer color;

        public final boolean translucent;

        /**
         * Whether depth buffer should be written to. False for beacon_beam-like
         * translucent glowing materials, true for everything else.
         */
        public final boolean writeDepthBuf;

        public MaterialProperties(ShaderType shaderType, ResourceLocation texture, @Nullable Integer color) {
                this.shaderType = shaderType;
                this.texture = texture;
                this.color = color;
                this.translucent = shaderType.isTranslucent();
                this.writeDepthBuf = shaderType != ShaderType.TRANSLUCENT_GLOWING;
        }

        public ResourceLocation getTexture() {
                return texture;
        }

        public void setTexture(ResourceLocation texture) {
                this.texture = texture;
        }

        @Nullable
        public Integer getColor() {
                return color;
        }

        public void setColor(@Nullable Integer color) {
                this.color = color;
        }

        @Override
        public boolean equals(Object object) {
                if (this == object) {
                        return true;
                }
                if (!(object instanceof MaterialProperties)) {
                        return false;
                }
                final MaterialProperties materialProperties = (MaterialProperties) object;
                return shaderType == materialProperties.shaderType &&
                                Objects.equals(texture, materialProperties.texture) &&
                                Objects.equals(color, materialProperties.color) &&
                                translucent == materialProperties.translucent &&
                                writeDepthBuf == materialProperties.writeDepthBuf;
        }

        @Override
        public int hashCode() {
                return Objects.hash(shaderType, texture, color, translucent, writeDepthBuf);
        }
}
