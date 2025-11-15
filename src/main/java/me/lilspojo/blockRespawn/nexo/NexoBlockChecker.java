package me.lilspojo.blockRespawn.nexo;

import com.nexomc.nexo.api.NexoItems;
import com.nexomc.nexo.items.ItemBuilder;

public class NexoBlockChecker {

    public boolean isNexoBlock(String configBlockId) {
        ItemBuilder itemBuilder = NexoItems.itemFromId(configBlockId);
        return itemBuilder != null;
    }
}
