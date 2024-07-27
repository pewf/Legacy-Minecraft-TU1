package wily.legacy.client.screen;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.navigation.ScreenDirection;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.*;
import org.jetbrains.annotations.Nullable;
import wily.legacy.Legacy4JPlatform;
import wily.legacy.client.CommonColor;
import wily.legacy.client.ControlType;
import wily.legacy.client.LegacyCraftingTabListing;
import wily.legacy.client.controller.ControllerBinding;
import wily.legacy.init.LegacyRegistries;
import wily.legacy.util.*;
import wily.legacy.client.controller.BindingState;
import wily.legacy.client.controller.Controller;
import wily.legacy.inventory.LegacyCraftingMenu;
import wily.legacy.inventory.RecipeMenu;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static wily.legacy.util.LegacySprites.SMALL_ARROW;
import static wily.legacy.client.screen.ControlTooltip.*;
import static wily.legacy.client.screen.RecipeIconHolder.getActualItem;

public class LegacyCraftingScreen extends AbstractContainerScreen<LegacyCraftingMenu> implements Controller.Event,ControlTooltip.Event {
    public static final Offset CRAFTING_OFFSET = new Offset(0.5,0.5,0);
    private final Inventory inventory;
    protected final List<ItemStack> compactItemStackList = new ArrayList<>();
    private final boolean is2x2;
    private final int gridDimension;
    private boolean onlyCraftableRecipes = false;
    protected Stocker.Sizeable infoType = new Stocker.Sizeable(0,2);
    protected final List<Ingredient> ingredientsGrid;
    protected ItemStack resultStack = ItemStack.EMPTY;
    public static final Component INGREDIENTS = Component.translatable("legacy.container.ingredients");
    public static final Component COLOR_TAB = Component.translatable("legacy.container.tab.color");
    public static final Component SHAPE_TAB = Component.translatable("legacy.container.tab.shape");
    public static final Component EFFECT_TAB = Component.translatable("legacy.container.tab.effect");
    public static final Component SELECT_STAR_TAB = Component.translatable("legacy.container.tab.select_star");
    public static final Component ADD_FADE_TAB = Component.translatable("legacy.container.tab.add_fade");
    public static final Component ADD_POWER_TAB = Component.translatable("legacy.container.tab.add_power");
    public static final Component SELECT_SHIELD_BANNER = Component.translatable("legacy.container.tab.select_shield_banner");
    public static final Component COPY_BANNER = Component.translatable("legacy.container.tab.copy_banner");
    public static final Component ADD_SHERD = Component.translatable("legacy.container.tab.add_pottery_sherd");
    protected final List<RecipeIconHolder<CraftingRecipe>> craftingButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> dyeItemButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> dyeArmorButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> dyeBannerButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> decoratedPotButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkStarButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkStarFadeButtons = new ArrayList<>();
    protected final List<CustomRecipeIconHolder> fireworkButtons = new ArrayList<>();
    protected final List<List<List<RecipeHolder<CraftingRecipe>>>> recipesByTab = new ArrayList<>();
    protected List<List<RecipeHolder<CraftingRecipe>>> filteredRecipesByGroup = Collections.emptyList();
    protected final Stocker.Sizeable page =  new Stocker.Sizeable(0);
    protected final Stocker.Sizeable craftingButtonsOffset =  new Stocker.Sizeable(0);
    protected final TabList craftingTabList;
    protected final TabList fireworkTabList = new TabList();
    protected final TabList dyeTabList = new TabList();
    protected final TabList groupTabList = new TabList().add(0,0,42, 42, 4,LegacyTabButton.iconOf(Items.CRAFTING_TABLE),Component.empty(),null,b->repositionElements()).add(0,0,42, 42, 4, LegacyTabButton.iconOf(Items.FIREWORK_ROCKET),Component.empty(),null,b->resetElements()).add(0,0,42, 42, 4, LegacyTabButton.iconOf(Items.CYAN_DYE),Component.empty(),null,b->resetElements());
    protected final LegacyScrollRenderer scrollRenderer = new LegacyScrollRenderer();
    private final boolean[] warningSlots;
    protected final ContainerListener listener = new ContainerListener() {
        public void slotChanged(AbstractContainerMenu abstractContainerMenu, int i, ItemStack itemStack) {
            if (onlyCraftableRecipes && groupTabList.selectedTab == 0) {
                filteredRecipesByGroup = recipesByTab.get(craftingTabList.selectedTab).stream().map(l -> l.stream().filter(r -> RecipeMenu.canCraft(r.value().getIngredients(), inventory,abstractContainerMenu.getCarried())).toList()).filter(l -> !l.isEmpty()).toList();
                craftingButtons.get(selectedCraftingButton).updateRecipeDisplay();
            }else {
                if (getCraftingButtons().size() > selectedCraftingButton && getCraftingButtons().get(selectedCraftingButton) instanceof CustomCraftingIconHolder h) h.updateRecipe();
            }
        }

        public void dataChanged(AbstractContainerMenu abstractContainerMenu, int i, int j) {

        }
    };
    protected int selectedCraftingButton;
    public static final Item[] VANILLA_CATEGORY_ICONS = new Item[]{Items.BRICKS,Items.REDSTONE,Items.GOLDEN_SWORD,Items.LAVA_BUCKET};
    protected RecipeManager manager;
    public static LegacyCraftingScreen craftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component){
        return new LegacyCraftingScreen(abstractContainerMenu,inventory,component,false);
    }
    public static LegacyCraftingScreen playerCraftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component){
        return new LegacyCraftingScreen(abstractContainerMenu,inventory,component,true);
    }
    protected boolean inited = false;
    public LegacyCraftingScreen(LegacyCraftingMenu abstractContainerMenu, Inventory inventory, Component component, boolean is2x2) {
        super(abstractContainerMenu, inventory, component);
        craftingTabList = new TabList(new PagedList<>(page,is2x2 ? 6 : 7));
        this.inventory = inventory;
        this.is2x2 = menu.is2x2 = is2x2;
        gridDimension = is2x2 ? 2 : 3;
        ingredientsGrid = new ArrayList<>(Collections.nCopies(gridDimension * gridDimension,Ingredient.EMPTY));
        warningSlots = new boolean[gridDimension * gridDimension];
        if (Minecraft.getInstance().level == null) return;
        manager = Minecraft.getInstance().level.getRecipeManager();
        for (LegacyCraftingTabListing listing : LegacyCraftingTabListing.list) {
            if (!listing.isValid()) continue;
            List<List<RecipeHolder<CraftingRecipe>>> groups = new ArrayList<>();
            listing.craftings.values().forEach(l->{
                if (l.isEmpty()) return;
                List<RecipeHolder<CraftingRecipe>> group = new ArrayList<>();
                l.forEach(v->v.addRecipes(RecipeType.CRAFTING,manager,group,r-> !r.value().getIngredients().isEmpty() && (!is2x2 || is2x2Recipe(r.value()))));
                if (!group.isEmpty()) groups.add(group);
            });
            if (groups.isEmpty()) continue;

            recipesByTab.add(groups);

            craftingTabList.addTabButton(43,0,listing.icon,listing.displayName, t->resetElements());

        }
        if (ScreenUtil.getLegacyOptions().vanillaTabs().get()) manager.getAllRecipesFor(RecipeType.CRAFTING).stream().collect(Collectors.groupingBy(h->h.value().category(),()->new TreeMap<>(Comparator.comparingInt(Enum::ordinal)),Collectors.groupingBy(h->h.value().getGroup().isEmpty() ? h.id().toString() : h.value().getGroup()))).forEach((category, m)->{
            if (m.isEmpty()) return;
            List<List<RecipeHolder<CraftingRecipe>>> groups = new ArrayList<>();
            m.values().forEach(l->{
                List<RecipeHolder<CraftingRecipe>> group = l.stream().filter(h->!(h.value() instanceof CustomRecipe) && (!is2x2 || is2x2Recipe(h.value()))).collect(Collectors.toList());
                if (!group.isEmpty()) groups.add(group);
            });
            if (groups.isEmpty()) return;
            recipesByTab.add(groups);
            craftingTabList.addTabButton(43,0,LegacyTabButton.iconOf(VANILLA_CATEGORY_ICONS[category.ordinal()]), getTitle(), t->resetElements());
        });
        craftingTabList.resetSelectedTab();
        inited = true;
        addCraftingButtons();
    }

    @Override
    public void addControlTooltips(Renderer renderer) {
        Event.super.addControlTooltips(renderer);
        renderer.
                set(0,create(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_RETURN) : ControllerBinding.DOWN_BUTTON.bindingState.getIcon(),()->getFocused() instanceof RecipeIconHolder<?> h && h.canCraft() && h.isValidIndex() || getFocused() instanceof CustomCraftingIconHolder c && c.canCraft() ? getAction("legacy.action.create") : null)).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_X) : ControllerBinding.LEFT_BUTTON.bindingState.getIcon(), ()-> groupTabList.selectedTab == 0 ? getAction("legacy.action.info") : getFocused() instanceof CustomCraftingIconHolder h && h.addedIngredientsItems != null && !h.addedIngredientsItems.isEmpty() ? getAction("legacy.action.remove") : null).
                add(()-> ControlType.getActiveType().isKbm() ? getKeyIcon(InputConstants.KEY_O) : ControllerBinding.UP_BUTTON.bindingState.getIcon(), ()-> groupTabList.selectedTab == 0 ? getAction(onlyCraftableRecipes ? "legacy.action.all_recipes" : "legacy.action.show_craftable_recipes") : getFocused() instanceof CustomCraftingIconHolder h && h.canAddIngredient() ? getAction("legacy.action.add") : null).
                addCompound(()-> new Icon[]{ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_LBRACKET) : ControllerBinding.LEFT_BUMPER.bindingState.getIcon(),ControlTooltip.SPACE_ICON, ControlType.getActiveType().isKbm() ? ControlTooltip.getKeyIcon(InputConstants.KEY_RBRACKET) : ControllerBinding.RIGHT_BUMPER.bindingState.getIcon()},()->getAction("legacy.action.group")).
                add(()-> page.max > 0 && groupTabList.selectedTab == 0 ? ControlType.getActiveType().isKbm() ? COMPOUND_ICON_FUNCTION.apply(new Icon[]{ControlTooltip.getKeyIcon(InputConstants.KEY_LSHIFT),ControlTooltip.PLUS_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_LEFT),ControlTooltip.SPACE_ICON,ControlTooltip.getKeyIcon(InputConstants.KEY_RIGHT)}) : ControllerBinding.RIGHT_STICK.bindingState.getIcon() : null,()->getAction("legacy.action.page"));
    }


    public void resetElements(){
        listener.slotChanged(menu,-1,ItemStack.EMPTY);
        selectedCraftingButton = 0;
        infoType.set(0);
        craftingButtonsOffset.set(0);
        if (inited) repositionElements();
    }
    public static boolean itemHasPatterns(ItemStack stack){
        CompoundTag beTag = stack.getTagElement("BlockEntityTag");
        return stack.getItem() instanceof BannerItem && (beTag != null && beTag.contains("Patterns") && !beTag.getList("Patterns",10).isEmpty());
    }
    protected CustomCraftingIconHolder craftingButtonByList(Component displayName, List<ItemStack> itemStacks, Consumer<CustomCraftingIconHolder> updateRecipe){
        return new CustomCraftingIconHolder(itemStacks.get(0)){
            public Component getDisplayName() {
                return displayName;
            }

            ItemStack nextItem() {
                return nextItem(itemStacks);
            }
            ItemStack previousItem() {
                return previousItem(itemStacks);
            }

            int findInventoryMatchSlot() {
                for (int i = 0; i < menu.slots.size(); i++)
                    if (menu.slots.get(i).getItem().is(itemIcon.getItem())) return i;
                return 0;
            }
            void updateRecipe() {
                updateRecipe.accept(this);
            }
        };
    }
    protected CustomCraftingIconHolder craftingButtonByPredicate(Component displayName, Predicate<ItemStack> isValid, Consumer<CustomCraftingIconHolder> updateRecipe){
        return new CustomCraftingIconHolder(){
            public Component getDisplayName() {
                return displayName;
            }

            ItemStack nextItem() {
                return nextItem(inventory,isValid);
            }
            ItemStack previousItem() {
                return previousItem(inventory,isValid);
            }
            public boolean applyNextItemIfAbsent() {
                return true;
            }

            int findInventoryMatchSlot() {
                for (int i = 0; i < menu.slots.size(); i++)
                    if (menu.slots.get(i).getItem() == itemIcon) return i;
                itemIcon = nextItem;
                return itemIcon.isEmpty() ? 0 : findInventoryMatchSlot();
            }
            void updateRecipe() {
                updateRecipe.accept(this);
            }

        };
    }
    @Override
    public void setFocused(@Nullable GuiEventListener guiEventListener) {
        if (guiEventListener instanceof TabList) return;
        super.setFocused(guiEventListener);
    }

    public static boolean is2x2Recipe(CraftingRecipe recipe){
        return (!(recipe instanceof ShapedRecipe rcp) || Math.max(rcp.getHeight(), rcp.getWidth()) < 3) && ((recipe instanceof ShapedRecipe s) || recipe.getIngredients().size() <= 4);
    }

    /*
        renders crafting recipes and bottom right labels (inventory label, ingredients, item description)
     */
    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int i, int j) {
        Component title = getTabList() == craftingTabList ? getTabList().tabButtons.get(getTabList().selectedTab).getMessage() : getFocused() instanceof CustomCraftingIconHolder h ? h.getDisplayName() : Component.empty();
        guiGraphics.drawString(this.font, title,((groupTabList.selectedTab == 0 ? imageWidth : imageWidth / 2) - font.width(title)) / 2,17, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        if (infoType.get() <= 0) guiGraphics.drawString(this.font, this.playerInventoryTitle, imageWidth / 2 + (imageWidth / 2 - font.width(playerInventoryTitle)) / 2 - (is2x2 ? 10 : 0), 118, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
        else {
            if (selectedCraftingButton < getCraftingButtons().size() && getCraftingButtons().get(selectedCraftingButton) instanceof RecipeIconHolder<?> h) {
                if (infoType.get() == 1 && ScreenUtil.hasTip(h.getFocusedResult())) {
                    List<FormattedCharSequence> l = font.split(ScreenUtil.getTip(h.getFocusedResult()), 152);
                    for (int i1 = 0; i1 < l.size(); i1++) {
                        if (i1 > 7) break;
                        guiGraphics.drawString(font, l.get(i1), 181, 108 + i1 * 12, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    }

                } else if (infoType.get() == 2) {
                    guiGraphics.drawString(this.font, INGREDIENTS, imageWidth / 2 + (imageWidth / 2 - font.width(INGREDIENTS)) / 2 - (is2x2 ? 10 : 0), 110, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                    if (h.getFocusedRecipe() != null) {
                        compactItemStackList.clear();
                        RecipeMenu.handleCompactItemStackList(compactItemStackList, () -> h.getFocusedRecipe().value().getIngredients().stream().map(RecipeIconHolder::getActualItem).iterator());
                        for (int i1 = 0; i1 < compactItemStackList.size(); i1++) {
                            if (i1 > 4) break;
                            ItemStack ing = compactItemStackList.get(i1);
                            ScreenUtil.iconHolderRenderer.itemHolder(180, 124 + 15 * i1, 14, 14, ing, false, Offset.ZERO).render(guiGraphics, i, j, 0);
                            guiGraphics.pose().pushPose();
                            guiGraphics.pose().translate(198, 128 + 15 * i1, 0);
                            guiGraphics.pose().scale(2 / 3f, 2 / 3f, 2 / 3f);
                            guiGraphics.drawString(font, ing.getHoverName(), 0, 0, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                            guiGraphics.pose().popPose();
                        }
                    }
                }
            }
        }
        guiGraphics.pose().translate(-leftPos,-topPos,0);
        getCraftingButtons().forEach(b-> b.render(guiGraphics,i,j,0));
        if (selectedCraftingButton < getCraftingButtons().size()) getCraftingButtons().get(selectedCraftingButton).renderSelection(guiGraphics, i, j, 0);
        guiGraphics.pose().translate(leftPos,topPos,0);
    }

    @Override
    public void bindingStateTick(BindingState state) {
        if (state.pressed && state.canClick()){
            if (state.is(ControllerBinding.LEFT_TRIGGER) || state.is(ControllerBinding.RIGHT_TRIGGER)) groupTabList.controlTab(state.is(ControllerBinding.LEFT_TRIGGER),state.is(ControllerBinding.RIGHT_TRIGGER));
            if (state.is(ControllerBinding.RIGHT_STICK) && state instanceof BindingState.Axis s) controlPage(s.x < 0 && -s.x > Math.abs(s.y),s.x > 0 && s.x > Math.abs(s.y));
        }
    }
    @Override
    protected void init() {
        resultStack = ItemStack.EMPTY;
        imageWidth =  is2x2 ? 300 : 348;
        imageHeight = 215;
        super.init();
        topPos+=18;
        menu.addSlotListener(listener);
        menu.inventoryActive = infoType.get() <= 0;
       if (selectedCraftingButton < getCraftingButtons().size()) setFocused(getCraftingButtons().get(selectedCraftingButton));
       if (groupTabList.selectedTab == 0) {
            craftingButtonsOffset.max = Math.max(0,recipesByTab.get(page.get() * 7 + craftingTabList.selectedTab).size() - 12);
            craftingButtons.forEach(b->{
                b.setPos(leftPos + (is2x2 ? 16 : 13) + craftingButtons.indexOf(b) * 27,topPos + 38);
                addWidget(b);
            });
        }else {
            int size = getCraftingButtons().size();
            getCraftingButtons().forEach(b->{
                b.setPos(leftPos + (size == 1 ? 77 : size ==2 ? 52 : size ==3 ? 21 : 8) + getCraftingButtons().indexOf(b) * (size ==2 ? 62 : size ==3 ? 55 : 45),topPos + 39);
                if (size == 3) b.offset = new Offset(0.5 + getCraftingButtons().indexOf(b) * 0.5,0,0);
                b.init();
                addWidget(b);
            });
        }

        addWidget(getTabList());
        getTabList().init(leftPos, topPos - 37, imageWidth, (t, i) -> {
            int index = getTabList().tabButtons.indexOf(t);
            t.type = index == 0 ? 0 : index >= (is2x2 ? 6 : 7) - 1 ? 2 : 1;
            t.setWidth(51);
            t.offset = (t1) -> new Offset( (is2x2 ? -1.2 : -1.5) * getTabList().tabButtons.indexOf(t), t1.selected ? 0 : 1.5, 0);
        });
    }
    protected TabList getTabList(){
        return craftingTabList;
    }
    protected boolean canCraft(List<Ingredient> ingredients, boolean isFocused) {
        compactItemStackList.clear();
        RecipeMenu.handleCompactInventoryList(compactItemStackList,inventory,menu.getCarried());
        return canCraft(compactItemStackList, isFocused ? ingredientsGrid : ingredients, isFocused ? warningSlots : null);
    }
    public static boolean canCraft(List<ItemStack> compactItemStackList, List<Ingredient> ings, boolean[] warningSlots) {
        boolean canCraft = true;
        for (int i1 = 0; i1 < ings.size(); i1++) {
            Ingredient ing = ings.get(i1);
            if (ing.isEmpty()) continue;
            Optional<ItemStack> match = compactItemStackList.stream().filter(i-> !i.isEmpty() && ing.test(i.copyWithCount(1))).findFirst();
            if (match.isPresent()) {
                match.get().shrink(1);
                if (warningSlots != null) warningSlots[i1] = false;
            } else {
                canCraft = false;
                if (warningSlots == null) break;
                else warningSlots[i1] = true;
            }
        }
        return canCraft;
    }
    protected void addCraftingButtons(){
        for (int i = 0; i < (is2x2 ? 10 : 12); i++) {
            int index = i;

            RecipeIconHolder<CraftingRecipe> h;
            craftingButtons.add(h = new RecipeIconHolder<>(leftPos + 13 + i * 27, topPos + 38) {
                @Override
                public void render(GuiGraphics graphics, int i, int j, float f) {
                    if (isFocused()) selectedCraftingButton = index;
                    super.render(graphics, i, j, f);
                }

                protected boolean canCraft(RecipeHolder<CraftingRecipe> rcp) {
                    if (rcp == null || onlyCraftableRecipes) return true;
                    return LegacyCraftingScreen.this.canCraft(rcp.value().getIngredients(),isFocused() && getFocusedRecipe() == rcp);
                }

                protected List<RecipeHolder<CraftingRecipe>> getRecipes() {
                    List<List<RecipeHolder<CraftingRecipe>>> list = onlyCraftableRecipes ? filteredRecipesByGroup : recipesByTab.get(page.get() * 7 + craftingTabList.selectedTab);
                    return list.size() <= craftingButtonsOffset.get() + index ? Collections.emptyList() : list.get(craftingButtonsOffset.get() + index);
                }

                @Override
                protected void toggleCraftableRecipes() {
                    onlyCraftableRecipes = !onlyCraftableRecipes;
                    listener.slotChanged(menu, 0, ItemStack.EMPTY);
                }

                @Override
                public boolean keyPressed(int i, int j, int k) {
                    if (controlCyclicNavigation(i, index, craftingButtons, craftingButtonsOffset, scrollRenderer, LegacyCraftingScreen.this))
                        return true;
                    if (i == InputConstants.KEY_X && groupTabList.selectedTab == 0){
                        infoType.add(1,true);
                        menu.inventoryActive = infoType.get() <= 0;
                        ScreenUtil.playSimpleUISound(LegacyRegistries.FOCUS.get(),1.0f);
                        return true;
                    }
                    return super.keyPressed(i, j, k);
                }

                protected void updateRecipeDisplay(RecipeHolder<CraftingRecipe> rcp) {
                    resultStack = getFocusedResult();
                    clearIngredients(ingredientsGrid);
                    if (rcp == null) return;
                    if (!(rcp.value() instanceof ShapedRecipe r)) {
                        for (int i = 0; i < rcp.value().getIngredients().size(); i++)
                            ingredientsGrid.set(i, rcp.value().getIngredients().get(i));
                        return;
                    }
                    LegacyCraftingMenu.updateShapedIngredients(ingredientsGrid,r.getIngredients(),gridDimension,Math.max(r.getHeight(), r.getWidth()),r.getWidth(),r.getHeight());
                }

                @Override
                public void craft() {
                    ScreenUtil.playSimpleUISound(SoundEvents.ITEM_PICKUP,1.0f);
                    super.craft();
                }
            });
            h.offset = CRAFTING_OFFSET;
        }
    }

    public static void clearIngredients(List<Ingredient> ingredientsGrid){
        for (int i = 0; i < ingredientsGrid.size(); i++) {
            if (!ingredientsGrid.get(i).isEmpty()) ingredientsGrid.set(i, Ingredient.EMPTY);
        }
    }

    protected abstract class CustomCraftingIconHolder extends CustomRecipeIconHolder{
        public CustomCraftingIconHolder(ItemStack itemStack) {
            super(itemStack);
        }

        public CustomCraftingIconHolder() {
            super();
        }
        LegacyScrollRenderer getScrollRenderer() {
            return scrollRenderer;
        }
        public boolean canCraft() {
            return LegacyCraftingScreen.this.canCraft(getIngredientsGrid(),false);
        }
        public List<Ingredient> getIngredientsGrid() {
            return ingredientsGrid;
        }
        public ItemStack getResultStack() {
            return resultStack;
        }
        @Override
        public void render(GuiGraphics graphics, int i, int j, float f) {
            if (isFocused()) selectedCraftingButton = getCraftingButtons().indexOf(this);
            super.render(graphics, i, j, f);
        }
    }

    @Override
    public boolean mouseScrolled(double d, double e, double f, double g) {
        if (super.mouseScrolled(d, e, f, g)) return true;
        int scroll = (int)Math.signum(g);
        if (((craftingButtonsOffset.get() > 0 && scroll < 0) || (scroll > 0 && craftingButtonsOffset.max > 0)) && craftingButtonsOffset.add(scroll,false) != 0){
            repositionElements();
            return true;
        }
        return false;
    }

    @Override
    public void renderBackground(GuiGraphics guiGraphics, int i, int j, float f) {
        renderBg(guiGraphics, f, i, j);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float f, int i, int j) {
        getTabList().render(guiGraphics, i, j, f);
        guiGraphics.blitSprite(LegacySprites.SMALL_PANEL, leftPos, topPos, imageWidth, imageHeight);
        guiGraphics.blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 9, topPos + 109, imageWidth / 2 - 11 - (is2x2 ? 10 : 0), 99);
        guiGraphics.blitSprite(LegacySprites.SQUARE_RECESSED_PANEL,leftPos + imageWidth / 2  + 2 - (is2x2 ? 10 : 0), topPos + 109, imageWidth / 2 - 11 + (is2x2 ? 10 : 0), 99);
        if (groupTabList.selectedTab != 0) guiGraphics.blitSprite(LegacySprites.SQUARE_RECESSED_PANEL, leftPos + 176, topPos + 8, 163, 93);
        guiGraphics.blitSprite(SMALL_ARROW, leftPos + (is2x2 ? 69 : 97), topPos + 161, 16, 13);
        if (groupTabList.selectedTab == 0) {
            if (craftingButtonsOffset.get() > 0)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.LEFT, leftPos + 5, topPos + 45);
            if (craftingButtonsOffset.max > 0 && craftingButtonsOffset.get() < craftingButtonsOffset.max)
                scrollRenderer.renderScroll(guiGraphics, ScreenDirection.RIGHT, leftPos + 337, topPos + 45);
        }
    }

    @Override
    public boolean disableCursorOnInit() {
        return true;
    }

    @Override
    public boolean onceClickBindings() {
        return false;
    }

    @Override
    public boolean keyPressed(int i, int j, int k) {
        getTabList().controlTab(i);
        return super.keyPressed(i, j, k);
    }
    protected boolean controlPage(boolean left, boolean right){
        if ((left || right) && page.max > 0 && groupTabList.selectedTab == 0){
            int lastPage = page.get();
            page.add(left ? -1 : 1);
            if (lastPage != page.get()) {
                craftingTabList.resetSelectedTab();
                rebuildWidgets();
                return true;
            }
        }return false;
    }
    public List<? extends LegacyIconHolder> getCraftingButtons(){
        return craftingButtons;
    }
    @Override
    public void render(GuiGraphics guiGraphics, int i, int j, float f) {

        renderBackground(guiGraphics, i, j, f);
        super.render(guiGraphics, i, j, f);

        for (int index = 0; index < ingredientsGrid.size(); index++)
            ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 15 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23, 23, 23, getActualItem(ingredientsGrid.get(index)), (!onlyCraftableRecipes || groupTabList.selectedTab != 0) && !getActualItem(ingredientsGrid.get(index)).isEmpty() &&  warningSlots[index], new Offset(0.5, is2x2 ? 0 : 0.5, 0)).render(guiGraphics, i, j, f);;
        ScreenUtil.iconHolderRenderer.itemHolder(leftPos + (is2x2 ? 100 : 124), topPos + 151, 36, 36, resultStack, (!onlyCraftableRecipes || groupTabList.selectedTab != 0) && ingredientsGrid.stream().anyMatch(ing-> !ing.isEmpty()) && !canCraft(ingredientsGrid,false), new Offset(0.5, 0, 0)).render(guiGraphics, i, j, f);
        if (!resultStack.isEmpty()) {
            Component resultName = resultStack.getHoverName();
            ScreenUtil.renderScrollingString(guiGraphics, font, resultName, leftPos + (imageWidth / 2 - font.width(resultName)) / 2, topPos + 116, leftPos + 170, topPos + 125, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
            if (groupTabList.selectedTab != 0){
                List<Component> list = resultStack.getTooltipLines(minecraft.player, TooltipFlag.NORMAL);
                for (int i1 = 0; i1 < list.size(); i1++) {
                    if (26 + i1 * 13 >= 93) break;
                    Component c = list.get(i1);
                    ScreenUtil.renderScrollingString(guiGraphics, font, c.copy().withColor(CommonColor.INVENTORY_GRAY_TEXT.get()), leftPos + 180, topPos + 15 + i1 * 13, leftPos + 335, topPos + 26 + i1 * 13, CommonColor.INVENTORY_GRAY_TEXT.get(), false);
                }
            }
            if (ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 113 : 124), topPos + 151,36,36)) guiGraphics.renderTooltip(font, resultStack,i,j);
        }
        for (int index = 0; index < ingredientsGrid.size(); index++) if (!getActualItem(ingredientsGrid.get(index)).isEmpty() && ScreenUtil.isMouseOver(i,j,leftPos + (is2x2 ? 33 : 21) + index % gridDimension * 23, topPos + (is2x2 ? 145 : 133) + index / gridDimension * 23,23,23)) guiGraphics.renderTooltip(font,getActualItem(ingredientsGrid.get(index)),i,j);
        getCraftingButtons().forEach(h -> h.renderTooltip(minecraft, guiGraphics, i, j));

        renderTooltip(guiGraphics, i, j);
    }
}
