package dev.letsgoaway.geyserextras.core.injectors.bedrock;

import dev.letsgoaway.geyserextras.core.ExtrasPlayer;
import dev.letsgoaway.geyserextras.core.preferences.bindings.Action;
import dev.letsgoaway.geyserextras.core.preferences.bindings.Remappable;
import org.cloudburstmc.protocol.bedrock.packet.InteractPacket;
import org.geysermc.geyser.entity.type.Entity;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.protocol.bedrock.entity.player.BedrockInteractTranslator;

import java.util.concurrent.TimeUnit;

import static dev.letsgoaway.geyserextras.core.GeyserExtras.GE;
import static dev.letsgoaway.geyserextras.core.GeyserExtras.SERVER;

@Translator(packet = InteractPacket.class)
public class BedrockInteractInjector extends BedrockInteractTranslator {
    @Override
    public void translate(GeyserSession session, InteractPacket packet) {
        if (packet.getAction() != InteractPacket.Action.OPEN_INVENTORY) {
            super.translate(session, packet);
            return;
        }
        ExtrasPlayer player = ExtrasPlayer.get(session);
        if (player == null) {
            super.translate(session, packet);
            return;
        }
        long now = TimeUnit.NANOSECONDS.toMillis(System.nanoTime());
        if (GE.getConfig().isDebugMode()) {
            long previousClick = player.getLastInventoryClickTime();
            String interval = previousClick == 0 ? "none" : Long.toString(now - previousClick);
            SERVER.debug("Inventory shortcut: enabled=" + player.getPreferences().isEnableDoubleClickShortcut()
                    + ", windowMs=" + player.getPreferences().getDoubleClickMS()
                    + ", intervalMs=" + interval
                    + ", formQueued=" + session.hasFormOpen()
                    + ", closingInventory=" + session.isClosingInventory());
        }
        if (now < player.getInventoryShortcutSuppressUntil()) {
            return;
        }
        if (session.hasFormOpen()) {
            // OPEN_INVENTORY is an explicit request to leave the form/settings UI.
            // Queued forms are not proof that the user can actually see one.
            // In particular settings forms may remain cached after leaving settings.
            session.closeForm();
        }
        {
            if ((player.getPreferences().isEnableDoubleClickShortcut() && GE.getConfig().isEnableGeyserExtrasMenu())
                    && !(session.isSneaking() && !player.getPreferences().isDefault(Remappable.SNEAK_INVENTORY))) {
                var pending = player.getDoubleClickShortcutFuture();
                if (pending != null && !pending.isCancelled() && !pending.isDone()) {
                    long elapsed = now - player.getLastInventoryClickTime();
                    if (elapsed >= 0 && elapsed <= player.getPreferences().getDoubleClickMS()) {
                        player.cancelInventoryShortcut();
                        Action.OPEN_GE_MENU.run(player);
                        return;
                    }
                    // A delayed event loop must not leave two single-click tasks.
                    player.cancelInventoryShortcut();
                    player.getPreferences().runAction(Remappable.OPEN_INVENTORY);
                }
                player.setLastInventoryClickTime(now);
                player.setDoubleClickShortcutFuture(session.scheduleInEventLoop(() -> {
                    player.setDoubleClickShortcutFuture(null);
                    player.setLastInventoryClickTime(0);
                    // A form sent through ExtrasPlayer cancels this task. Do not
                    // silently discard input because an unrelated settings form is cached.
                    if (session.hasFormOpen()) {
                        session.closeForm();
                    }
                    player.getPreferences().runAction(Remappable.OPEN_INVENTORY);
                }, player.getPreferences().getDoubleClickMS() + 20, TimeUnit.MILLISECONDS));
                return;
            }
            Remappable bind = player.getSession().isSneaking() ? Remappable.SNEAK_INVENTORY : Remappable.OPEN_INVENTORY;
            if (player.getPreferences().isDefault(bind) || player.getPreferences().getAction(bind).equals(Action.OPEN_INVENTORY)) {
                super.translate(session, packet);
                return;
            }
            Entity entity;
            if (packet.getRuntimeEntityId() == session.getPlayerEntity().geyserId()) {
                //Player is not in entity cache
                entity = session.getPlayerEntity();
            } else {
                entity = session.getEntityCache().getEntityByGeyserId(packet.getRuntimeEntityId());
            }
            if (entity == null)
                return;
            player.getPreferences().getAction(bind).run(player);
        }
    }
}
