package net.blay09.mods.twitchintegration.compat.vanilla;

import net.minecraft.util.text.ITextComponent;

public class VanillaChatMessage {
    private final int id;
    private final ITextComponent textComponent;

    public VanillaChatMessage(int id, ITextComponent textComponent) {
        this.id = id;
        this.textComponent = textComponent;
    }

    public int getId() {
        return id;
    }

    public ITextComponent getTextComponent() {
        return textComponent;
    }
}
