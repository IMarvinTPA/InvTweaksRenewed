package invtweaks.events;

import invtweaks.InvTweaksMod;
import invtweaks.config.InvTweaksConfig;
import invtweaks.network.NetworkDispatcher;
import invtweaks.util.ClientUtils;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@Mod.EventBusSubscriber(modid = InvTweaksMod.MODID)
public class ServerEvents {
    private ServerEvents() {
        // nothing to do
    }

    private static final Map<Player, EnumMap<InteractionHand, Item>> itemsCache = new WeakHashMap<>();
    private static final Map<Player, Object2IntMap<Item>> usedCache = new WeakHashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (event.side == LogicalSide.SERVER) {
            if (!InvTweaksConfig.getPlayerAutoRefill(event.player)) {
                return;
            }
            EnumMap<InteractionHand, Item> cached = itemsCache.computeIfAbsent(event.player, k -> new EnumMap<>(InteractionHand.class));
            Object2IntMap<Item> ucached = usedCache.computeIfAbsent(event.player, k -> new Object2IntOpenHashMap<>());
            for (InteractionHand hand : InteractionHand.values()) {
                if (cached.get(hand) != null && event.player.getItemInHand(hand).isEmpty() && ((ServerPlayer) event.player).getStats().getValue(Stats.ITEM_USED.get(cached.get(hand))) > ucached.getOrDefault(cached.get(hand), Integer.MAX_VALUE)) {
                    searchForSubstitute(event.player, hand, cached.get(hand));
                }
                ItemStack held = event.player.getItemInHand(hand);
                cached.put(hand, held.isEmpty() ? null : held.getItem());
                if (!held.isEmpty()) {
                    ucached.put(held.getItem(), ((ServerPlayer) event.player).getStats().getValue(Stats.ITEM_USED.get(held.getItem())));
                }
            }
        } else {
            if (InvTweaksConfig.isDirty()) {
                if (ClientUtils.serverConnectionExists()) {
                    NetworkDispatcher.INSTANCE.sendToServer(InvTweaksConfig.getSyncPacket());
                } else if (!InvTweaksConfig.getLastItemId().isEmpty()) {
                    NetworkDispatcher.INSTANCE.sendToServer(InvTweaksConfig.getNextSyncPacket());
                }
                InvTweaksConfig.setDirty(false);
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide) {
            if (event.getEntity() == ClientUtils.safeGetPlayer()) {
                InvTweaksConfig.setDirty(true);
            }
        }
    }

    private static void searchForSubstitute(Player ent, InteractionHand hand, Item item) {
        IntList frozen = Optional.ofNullable(InvTweaksConfig.getPlayerRules(ent).catToInventorySlots("/FROZEN"))
                        .map(IntArrayList::new) // prevent modification
                        .orElseGet(IntArrayList::new);

        frozen.sort(null);

        if (Collections.binarySearch(frozen, ent.getInventory().selected) >= 0) {
            return; // ignore frozen slot
        }

        TagKey<Item> altTag = null;
        if (item instanceof TieredItem) {
            if (item instanceof SwordItem) {
                altTag = ItemTags.SWORDS;
            } else if (item instanceof PickaxeItem) {
                altTag = ItemTags.PICKAXES;
            } else if (item instanceof AxeItem) {
                altTag = ItemTags.AXES;
            } else if (item instanceof ShovelItem) {
                altTag = ItemTags.SHOVELS;
            } else if (item instanceof HoeItem) {
                altTag = ItemTags.HOES;
            }
        }

        TagKey<Item> finalAltTag = altTag;
        ent.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.UP).ifPresent(cap -> {
            int alternativeSlot = -1;
            for (int i = 0; i < cap.getSlots(); ++i) {
                if (Collections.binarySearch(frozen, i) >= 0) {
                    continue; // ignore frozen slot
                }
                ItemStack candidate = cap.extractItem(i, Integer.MAX_VALUE, true).copy();
                if (candidate.is(item)) {
                    cap.extractItem(i, Integer.MAX_VALUE, false);
                    ent.setItemInHand(hand, candidate);
                    break;
                }
                if (finalAltTag != null && candidate.is(finalAltTag)) {
                    alternativeSlot = i;
                }
            }
            if (alternativeSlot >= 0 && ent.getItemInHand(hand).isEmpty()) {
                ent.setItemInHand(hand, cap.extractItem(alternativeSlot, Integer.MAX_VALUE, false));
            }
        });
    }
}
