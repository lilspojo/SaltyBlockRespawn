package me.lilspojo.blockRespawn.loader;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.List;

public class BlockRule {

    // Acceptable TYPE for matching
    public boolean isNexo;
    public String nexoId;

    public List<Material> materials;
    public BlockData blockData;

    // Replace info
    public boolean replaceIsNexo;
    public String replaceNexoId;

    public Material replaceMaterial;
    public BlockData replaceBlockData;

    // Delay / config
    public int delay;
    public boolean checkReplacement;
}


