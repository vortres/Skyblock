package net.swofty.types.generic.item.items.vanilla.blocks.stone.quartz;

import net.swofty.types.generic.block.BlockType;
import net.swofty.types.generic.item.ItemType;
import net.swofty.types.generic.item.MaterialQuantifiable;
import net.swofty.types.generic.item.SkyBlockItem;
import net.swofty.types.generic.item.impl.*;
import net.swofty.types.generic.item.impl.recipes.ShapedRecipe;
import net.swofty.types.generic.user.statistics.ItemStatistics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuartzSlab implements PlaceableCustomSkyBlockItem, MultiDefaultCraftable, Sellable {
    @Override
    public List<SkyBlockRecipe<?>> getRecipes() {
        List<SkyBlockRecipe<?>> recipes = new ArrayList<>();

        Map<Character, MaterialQuantifiable> ingredientMap1 = new HashMap<>();
        ingredientMap1.put('A', new MaterialQuantifiable(ItemType.CHISELED_QUARTZ_BLOCK, 1));
        List<String> pattern1 = List.of(
                "AAA");
        recipes.add(new ShapedRecipe(SkyBlockRecipe.RecipeType.NONE, new SkyBlockItem(ItemType.QUARTZ_SLAB, 6), ingredientMap1, pattern1));

        Map<Character, MaterialQuantifiable> ingredientMap2 = new HashMap<>();
        ingredientMap2.put('A', new MaterialQuantifiable(ItemType.QUARTZ_BLOCK, 1));
        List<String> pattern2 = List.of(
                "AAA");
        recipes.add(new ShapedRecipe(SkyBlockRecipe.RecipeType.NONE, new SkyBlockItem(ItemType.QUARTZ_SLAB, 6), ingredientMap2, pattern2));
        return recipes;
    }

    @Override
    public ItemStatistics getStatistics(SkyBlockItem instance) {
        return ItemStatistics.empty();
    }

    @Override
    public @Nullable BlockType getAssociatedBlockType() {
        return null;
    }

    @Override
    public double getSellValue() {
        return 1;
    }
}
