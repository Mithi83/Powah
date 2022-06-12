package owmii.powah.block.transmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import owmii.powah.config.v2.types.ChargingConfig;
import owmii.powah.lib.block.AbstractEnergyStorage;
import owmii.powah.lib.block.IInventoryHolder;
import owmii.powah.lib.logistics.energy.Energy;
import owmii.powah.lib.util.Player;
import owmii.powah.lib.util.Stack;
import owmii.powah.block.Tier;
import owmii.powah.block.Tiles;
import owmii.powah.item.BindingCardItem;

import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class PlayerTransmitterTile extends AbstractEnergyStorage<ChargingConfig, PlayerTransmitterBlock> implements IInventoryHolder {

    public PlayerTransmitterTile(BlockPos pos, BlockState state, Tier variant) {
        super(Tiles.PLAYER_TRANSMITTER.get(), pos, state, variant);
        this.inv.add(1);
    }

    public PlayerTransmitterTile(BlockPos pos, BlockState state) {
        this(pos, state, Tier.STARTER);
    }

    @Override
    protected int postTick(Level world) {
        long extracted = 0;
        if (world instanceof ServerLevel serverLevel && checkRedstone()) {
            ItemStack stack = this.inv.getFirst();
            if (stack.getItem() instanceof BindingCardItem card) {
                Optional<ServerPlayer> op = card.getPlayer(serverLevel, stack);
                if (op.isPresent()) {
                    ServerPlayer player = op.get();
                    if (card.isMultiDim(stack) || player.level.dimensionType().equals(world.dimensionType())) {
                        long charging = getConfig().getChargingSpeed(this.variant);
                        for (ItemStack stack1 : Player.invStacks(player)) {
                            if (stack1.isEmpty() || !Energy.chargeable(stack1)) continue;
                            long amount = Math.min(charging, getEnergy().getStored());
                            int received = Energy.receive(stack1, amount, false);
                            extracted += extractEnergy(received, false, null);
                        }
                        /* TODO ARCH
                        for (ItemStack stack1 : CuriosCompat.getAllStacks(player)) {
                            if (stack1.isEmpty() || !Energy.chargeable(stack1)) continue;
                            long amount = Math.min(charging, getEnergy().getStored());
                            int received = Energy.receive(stack1, amount, false);
                            extracted += extractEnergy(received, false, null);
                        }
                         */
                    }
                }
            }
        }
        return extracted > 0 ? 5 : -1;
    }

    @Override
    public int getSlotLimit(int slot) {
        return 1;
    }

    @Override
    public boolean canInsert(int slot, ItemStack stack) {
        return Stack.getTagOrEmpty(stack).hasUUID("bound_player_id");
    }

    @Override
    public boolean canExtract(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean keepEnergy() {
        return true;
    }
}