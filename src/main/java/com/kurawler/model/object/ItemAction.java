package com.kurawler.model.object;

/**
 * A player verb available for an {@link Item} (floor click menu or inventory).
 * {@code typeId} is consumed by {@link com.kurawler.ui.GameView#executeInteraction}.
 */
public final class ItemAction {

    public static final ItemAction TAKE = new ItemAction("TAKE", "take");
    public static final ItemAction EAT = new ItemAction("EAT", "eat");
    public static final ItemAction WEAR = new ItemAction("WEAR", "wear");
    public static final ItemAction READ = new ItemAction("READ", "read");
    public static final ItemAction EQUIP = new ItemAction("EQUIP", "equip");
    public static final ItemAction BREAK = new ItemAction("BREAK", "break_item");
    public static final ItemAction OPEN = new ItemAction("OPEN", "open_item");

    private final String label;
    private final String typeId;

    public ItemAction(String label, String typeId) {
        this.label = label;
        this.typeId = typeId;
    }

    public String getLabel() {
        return label;
    }

    public String getTypeId() {
        return typeId;
    }

    /** Menu row: [verb label, target name, interaction type id]. */
    public String[] toMenuRow(String targetName) {
        return new String[]{label, targetName, typeId};
    }
}
