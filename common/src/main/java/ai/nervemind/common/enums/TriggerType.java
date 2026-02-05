package ai.nervemind.common.enums;

/**
 * Types of workflow triggers.
 */
public enum TriggerType {
    /** Manual execution by user. */
    MANUAL("Manual", "Triggered manually by user"),
    /** Scheduled execution (cron). */
    SCHEDULE("Schedule", "Triggered on a schedule (cron)"),
    /** Triggered by incoming HTTP webhook. */
    WEBHOOK("Webhook", "Triggered by HTTP webhook"),
    /** Triggered by internal application event. */
    EVENT("Event", "Triggered by application event"),
    /** Triggered by file system events. */
    FILE_EVENT("File Event", "Triggered when files are created, modified, or deleted in a watched directory");

    private final String displayName;
    private final String description;

    /**
     * Constructs a new TriggerType.
     * 
     * @param displayName the display name
     * @param description the description
     */
    TriggerType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Gets the display name of the trigger type.
     * 
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the detailed description of the trigger type.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
