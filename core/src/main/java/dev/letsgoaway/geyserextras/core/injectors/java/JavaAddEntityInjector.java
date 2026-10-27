package dev.letsgoaway.geyserextras.core.injectors.java;

import dev.letsgoaway.geyserextras.core.ExtrasPlayer;
import dev.letsgoaway.geyserextras.core.injectors.GeyserHandler;
import org.geysermc.geyser.session.GeyserSession;
import org.geysermc.geyser.entity.spawn.EntitySpawnContext;
import dev.letsgoaway.geyserextras.core.parity.java.blockdisplay.BlockDisplayEntity;
import static dev.letsgoaway.geyserextras.core.GeyserExtras.GE;
import org.geysermc.geyser.translator.protocol.Translator;
import org.geysermc.geyser.translator.protocol.java.entity.JavaAddEntityTranslator;
import org.geysermc.mcprotocollib.protocol.data.game.entity.type.EntityType;
import org.geysermc.mcprotocollib.protocol.packet.ingame.clientbound.entity.ClientboundAddEntityPacket;

@Translator(packet = ClientboundAddEntityPacket.class)
public class JavaAddEntityInjector extends JavaAddEntityTranslator {
    @Override
    public void translate(GeyserSession session, ClientboundAddEntityPacket packet) {
        if (packet.getType() == EntityType.BLOCK_DISPLAY && GE.getConfig().isEnableBlockDisplayWorkaround()) {
            var definition = BlockDisplayEntity.buildEntityDef();
            var context = EntitySpawnContext.fromPacket(session, definition, packet);
            if (context.callServerSpawnEvent()) {
                var entity = definition.factory().create(context);
                if (context.consumers() != null) {
                    context.consumers().forEach(consumer -> consumer.accept(entity));
                }
                session.getEntityCache().spawnEntity(entity);
            }
            return;
        }
        super.translate(session, packet);
        if (packet.getType() == EntityType.PLAYER) {
            ExtrasPlayer player = ExtrasPlayer.get(session);
            player.getPlayerDimensionsMap().put(packet.getEntityId(), session.getDimensionType());
        }
    }
}
