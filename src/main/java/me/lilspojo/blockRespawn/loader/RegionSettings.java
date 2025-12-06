package me.lilspojo.blockRespawn.loader;

import java.util.ArrayList;
import java.util.List;

public class RegionSettings {
    public boolean preventMiningNonRespawnable;
    public boolean preventBlockPhysics;
    public List<BlockRule> rules = new ArrayList<>();
}

