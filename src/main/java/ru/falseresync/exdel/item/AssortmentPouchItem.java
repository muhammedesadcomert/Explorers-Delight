package ru.falseresync.exdel.item;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import ru.falseresync.exdel.ExplorersDelight;

import java.util.Set;

public class AssortmentPouchItem extends Item {
    public static final int INV_SIZE = 45;

    public AssortmentPouchItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        var stack = user.getStackInHand(hand);
        if (!world.isClient) {
            user.openHandledScreen(new AssortmentScreenHandlerFactory(getName(stack), new AssortmentInventory(INV_SIZE, stack)));
        }
        return TypedActionResult.success(stack);
    }

    protected record AssortmentScreenHandlerFactory(Text displayName, Inventory inventory) implements NamedScreenHandlerFactory {
        @Override
        public Text getDisplayName() {
            return displayName;
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
            return new AssortmentScreenHandler(syncId, playerInventory, inventory);
        }
    }

    public static class AssortmentScreenHandler extends ScreenHandler {
        private final Inventory inventory;

        public AssortmentScreenHandler(int syncId, PlayerInventory playerInventory) {
            this(syncId, playerInventory, new AssortmentInventory(INV_SIZE, ItemStack.EMPTY.copy()));
        }

        public AssortmentScreenHandler(int syncId, PlayerInventory playerInventory, Inventory inventory) {
            super(ExplorersDelight.ASSORTMENT_SCREEN_HANDLER, syncId);
            checkSize(inventory, INV_SIZE);
            this.inventory = inventory;
            inventory.onOpen(playerInventory.player);

            int i = 18;

            int n;
            int m;
            for (n = 0; n < 5; ++n) {
                for (m = 0; m < 9; ++m) {
                    addSlot(new AssortmentSlot(inventory, m + n * 9, 8 + m * 18, 18 + n * 18));
                }
            }

            for (n = 0; n < 3; ++n) {
                for (m = 0; m < 9; ++m) {
                    addSlot(new AssortmentSlot(playerInventory, m + n * 9 + 9, 8 + m * 18, 103 + n * 18 + i));
                }
            }

            for (n = 0; n < 9; ++n) {
                addSlot(new AssortmentSlot(playerInventory, n, 8 + n * 18, 161 + i));
            }
        }

        @Override
        public boolean canUse(PlayerEntity player) {
            return true;
        }

        @Override
        public void close(PlayerEntity player) {
            super.close(player);
            this.inventory.onClose(player);
        }
    }

    public static class AssortmentSlot extends Slot {
        public AssortmentSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return inventory.isValid(getIndex(), stack);
        }
    }

    @Environment(EnvType.CLIENT)
    public static class AssortmentScreen extends HandledScreen<AssortmentScreenHandler> {
        private static final Identifier TEXTURE = new Identifier("textures/gui/container/generic_54.png");

        public AssortmentScreen(AssortmentScreenHandler handler, PlayerInventory inventory, Text title) {
            super(handler, inventory, title);
            passEvents = false;
            backgroundHeight = 114 + 5 * 18;
            playerInventoryTitleY = backgroundHeight - 94;
        }

        @Override
        public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
            super.render(matrices, mouseX, mouseY, delta);
            drawMouseoverTooltip(matrices, mouseX, mouseY);
        }

        @Override
        protected void drawBackground(MatrixStack matrices, float delta, int mouseX, int mouseY) {
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.setShaderTexture(0, TEXTURE);
            int i = (width - backgroundWidth) / 2;
            int j = (height - backgroundHeight) / 2;
            drawTexture(matrices, i, j, 0, 0, backgroundWidth, 5 * 18 + 17);
            drawTexture(matrices, i, j + 5 * 18 + 17, 0, 126, backgroundWidth, 96);
        }
    }

    protected static class AssortmentInventory implements Inventory {
        private final int size;
        private final ItemStack parent;
        private final DefaultedList<ItemStack> stacks;

        public AssortmentInventory(int size, ItemStack parent) {
            this.size = size;
            this.parent = parent;
            this.stacks = DefaultedList.ofSize(size, ItemStack.EMPTY);
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public boolean isEmpty() {
            return stacks.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getStack(int slot) {
            return stacks.get(slot);
        }

        @Override
        public ItemStack removeStack(int slot, int amount) {
            return removeStack(slot);
        }

        @Override
        public ItemStack removeStack(int slot) {
            return stacks.set(slot, ItemStack.EMPTY);
        }

        @Override
        public void setStack(int slot, ItemStack stack) {
            if (slot >= 0 && slot < size) {
                stacks.set(slot, stack);
                stack.setCount(getMaxCountPerStack());
            }
        }

        @Override
        public int getMaxCountPerStack() {
            return 1;
        }

        @Override
        public void markDirty() {

        }

        @Override
        public boolean canPlayerUse(PlayerEntity player) {
            return true;
        }

        @Override
        public void onOpen(PlayerEntity player) {
            Inventory.super.onOpen(player);
            Inventories.readNbt(parent.getOrCreateNbt(), stacks);
        }

        @Override
        public void onClose(PlayerEntity player) {
            Inventory.super.onClose(player);
            Inventories.writeNbt(parent.getOrCreateNbt(), stacks);
        }

        @Override
        public boolean isValid(int slot, ItemStack stack) {
            return !stack.isOf(ExplorersDelight.ASSORTMENT_POUCH);
        }

        @Override
        public int count(Item item) {
            return (int) stacks.stream().filter(stack -> stack.isOf(item)).count();
        }

        @Override
        public boolean containsAny(Set<Item> items) {
            return stacks.stream().anyMatch(stack -> items.contains(stack.getItem()));
        }

        @Override
        public void clear() {
            stacks.clear();
        }
    }
}