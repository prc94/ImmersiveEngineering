/*
 * BluSunrize
 * Copyright (c) 2017
 *
 * This code is licensed under "Blu's License of Common Sense"
 * Details can be found in the license file in the root folder of this project
 */

package blusunrize.immersiveengineering.client.render.tile;

import blusunrize.immersiveengineering.api.crafting.MetalPressRecipe;
import blusunrize.immersiveengineering.client.ClientUtils;
import blusunrize.immersiveengineering.common.blocks.IEBlocks.Multiblocks;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity.MultiblockProcess;
import blusunrize.immersiveengineering.common.blocks.generic.PoweredMultiblockTileEntity.MultiblockProcessInWorld;
import blusunrize.immersiveengineering.common.blocks.metal.MetalPressTileEntity;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.tileentity.TileEntityRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.EmptyModelData;

import java.util.List;

public class MetalPressRenderer extends TileEntityRenderer<MetalPressTileEntity>
{
	public static DynamicModel<Void> PISTON;

	public MetalPressRenderer(TileEntityRendererDispatcher rendererDispatcherIn)
	{
		super(rendererDispatcherIn);
	}

	@Override
	public void render(MetalPressTileEntity te, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn)
	{
		if(!te.formed||te.isDummy()||!te.getWorldNonnull().isBlockLoaded(te.getPos()))
			return;

		final BlockRendererDispatcher blockRenderer = Minecraft.getInstance().getBlockRendererDispatcher();
		BlockPos blockPos = te.getPos();
		BlockState state = te.getWorld().getBlockState(blockPos);
		if(state.getBlock()!=Multiblocks.metalPress)
			return;
		IBakedModel model = PISTON.get(null);

		matrixStack.push();
		matrixStack.translate(.5, .5, .5);
		float piston = 0;
		float[] shift = new float[te.processQueue.size()];
		for(int i = 0; i < shift.length; i++)
		{
			MultiblockProcess<MetalPressRecipe> process = te.processQueue.get(i);
			if(process==null)
				continue;
			float transportTime = 52.5f/120f;
			float pressTime = 3.75f/120f;
			float fProcess = (process.processTick+(te.shouldRenderAsActive()?partialTicks: 0))/(float)process.maxTicks;

			if(fProcess < transportTime)
				shift[i] = fProcess/transportTime*.5f;
			else if(fProcess < (1-transportTime))
				shift[i] = .5f;
			else
				shift[i] = .5f+(fProcess-(1-transportTime))/transportTime*.5f;
			if(!te.mold.isEmpty())
				if(fProcess >= transportTime&&fProcess < (1-transportTime))
				{
					if(fProcess < (transportTime+pressTime))
						piston = (fProcess-transportTime)/pressTime;
					else if(fProcess < (1-transportTime-pressTime))
						piston = 1;
					else
						piston = 1-(fProcess-(1-transportTime-pressTime))/pressTime;
				}
		}

		matrixStack.rotate(new Quaternion(new Vector3f(0, 1, 0), te.getFacing()==Direction.SOUTH?180: te.getFacing()==Direction.WEST?90: te.getFacing()==Direction.EAST?-90: 0, true));
		matrixStack.push();
		matrixStack.translate(0, -piston*.6875f, 0);
		matrixStack.push();
		matrixStack.translate(-0.5, -0.5, -0.5);
		blockRenderer.getBlockModelRenderer().renderModel(te.getWorldNonnull(), model, state, blockPos, matrixStack,
				bufferIn.getBuffer(RenderType.getSolid()), true,
				te.getWorld().rand, 0, combinedOverlayIn, EmptyModelData.INSTANCE);
		matrixStack.pop();

		if(!te.mold.isEmpty())
		{
			matrixStack.translate(0, .34, 0);
			matrixStack.rotate(new Quaternion(new Vector3f(1, 0, 0), -90, true));
			float scale = .75f;
			matrixStack.scale(scale, scale, 1);
			ClientUtils.mc().getItemRenderer().renderItem(te.mold, TransformType.FIXED, combinedLightIn, combinedOverlayIn,
					matrixStack, bufferIn);
		}
		matrixStack.pop();
		matrixStack.translate(0, -.35, 1.25);
		for(int i = 0; i < shift.length; i++)
		{
			MultiblockProcess process = te.processQueue.get(i);
			if(!(process instanceof PoweredMultiblockTileEntity.MultiblockProcessInWorld))
				continue;
			matrixStack.push();
			matrixStack.translate(0, 0, -2.5*shift[i]);
			if(piston > .92)
				matrixStack.translate(0, .92-piston, 0);

			List<ItemStack> displays = ((MultiblockProcessInWorld<?>)process).getDisplayItem();
			if(!displays.isEmpty())
			{
				matrixStack.rotate(new Quaternion(new Vector3f(1, 0, 0), -90, true));
				float scale = .625f;
				matrixStack.scale(scale, scale, 1);
				ClientUtils.mc().getItemRenderer().renderItem(displays.get(0), TransformType.FIXED, combinedLightIn, combinedOverlayIn,
						matrixStack, bufferIn);
				matrixStack.pop();
			}
		}
		matrixStack.pop();
	}
}