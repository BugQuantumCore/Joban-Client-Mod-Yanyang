package com.lx862.jcm.mixin.mtrscripting;

import com.lx862.mtrscripting.mod.impl.mtr.MTRContentScripting;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.EyeCandyScriptContext;
import com.lx862.mtrscripting.mod.impl.mtr.eyecandy.event.BlockUseEvent;
import com.lx862.mtrscripting.core.primitive.ScriptInstance;
import com.lx862.mtrscripting.core.primitive.UniqueKey;
import org.mtr.registry.Items;
import org.mtr.block.BlockEyeCandy;
import org.mtr.block.BlockWaterloggable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

@Mixin(value = BlockEyeCandy.class, remap = false)
public class BlockEyecandyMixin extends BlockWaterloggable {

    public BlockEyecandyMixin(BlockBehaviour.Properties blockSettings) {
        super(blockSettings);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null && blockEntity instanceof BlockEyeCandy.BlockEntity) {
            if(blockEntity.getWorld() != null && blockEntity.getWorld().isClient()) {
                /* We should check with ShapeContext, but isHolding is not mapped */
                /* Need replacing in architectury branch */
                boolean notHoldingBrush = Minecraft.getInstance().player != null && !Minecraft.getInstance().player.isHolding(Items.BRUSH.get());

                if(notHoldingBrush) {
                    VoxelShape nativeShape=com.lx862.jcm.nativeapi.NativeContentDriver.shape((BlockEyeCandy.BlockEntity)blockEntity,false);
                    if(nativeShape!=null) return nativeShape;
                    ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", ((BlockEyeCandy.BlockEntity) blockEntity).getModelId(), pos.getX(), pos.getY(), pos.getZ()));
                    if (scriptInstance != null && scriptInstance.getContextObject() instanceof EyeCandyScriptContext) {
                        VoxelShape shape = ((EyeCandyScriptContext) scriptInstance.getContextObject()).getOutlineShape();
                        if(shape != null) {
                            return shape;
                        }
                    }
                }
            }
        }
        return VoxelShapes.fullCube();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        net.minecraft.world.level.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null && blockEntity instanceof BlockEyeCandy.BlockEntity) {
            if(blockEntity.getWorld() != null && blockEntity.getWorld().isClient()) {
                /* We should check with ShapeContext, but isHolding is not mapped */
                /* Need replacing in architectury branch */
                boolean notHoldingBrush = Minecraft.getInstance().player != null && !Minecraft.getInstance().player.isHolding(Items.BRUSH.get());

                if(notHoldingBrush) {
                    VoxelShape nativeShape=com.lx862.jcm.nativeapi.NativeContentDriver.shape((BlockEyeCandy.BlockEntity)blockEntity,true);
                    if(nativeShape!=null) return nativeShape;
                    ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", ((BlockEyeCandy.BlockEntity) blockEntity).getModelId(), pos.getX(), pos.getY(), pos.getZ()));
                    if (scriptInstance != null && scriptInstance.getContextObject() instanceof EyeCandyScriptContext) {
                        VoxelShape shape = ((EyeCandyScriptContext) scriptInstance.getContextObject()).getCollisionShape();
                        if(shape != null) {
                            return shape;
                        }
                    }
                }
            }
        }
        return VoxelShapes.empty();
    }

    @Inject(method = "onUse2", at = @At("RETURN"), cancellable = true)
    public void jcm$onEyecandyBlockUse(BlockState state, Level world, BlockPos pos, Player player, Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
        ActionResult returnValue = cir.getReturnValue();
        if(returnValue == ActionResult.SUCCESS) return;

        if(world.isClient()) {
            if(!player.isHolding(Items.BRUSH.get())) {
                net.minecraft.world.level.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
                if (blockEntity != null && blockEntity instanceof BlockEyeCandy.BlockEntity) {
                    ScriptInstance<?> scriptInstance = MTRContentScripting.getScriptManager().getInstanceManager().getInstance(new UniqueKey("eyecandy", ((BlockEyeCandy.BlockEntity)blockEntity).getModelId(), pos.getX(), pos.getY(), pos.getZ()));
                    /* Yanyang: mixed entries (JS + C++ under one script id) get
                       the use event on BOTH sides; the native modules are
                       triggered inside blockUse, the JS part below. */
                    boolean handled = com.lx862.jcm.nativeapi.NativeContentDriver.blockUse((BlockEyeCandy.BlockEntity)blockEntity,new BlockUseEvent(player,player.getItemInHand(hand)));
                    if(scriptInstance != null && scriptInstance.getContextObject() instanceof EyeCandyScriptContext) {
                        ((EyeCandyScriptContext)scriptInstance.getContextObject()).events().onBlockUse.trigger(new BlockUseEvent(player, player.getItemInHand(hand)));
                        handled = true;
                    }
                    if(handled) {
                        cir.setReturnValue(ActionResult.SUCCESS);
                    }
                }
            }
        } else {
            cir.setReturnValue(ActionResult.SUCCESS); // Don't allow block placing by default, we don't know what scripts do with it in the client.
        }
    }
}
