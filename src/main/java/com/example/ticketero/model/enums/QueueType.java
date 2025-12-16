package com.example.ticketero.model.enums;

public enum QueueType {
    CAJA("Caja", 5, 1, "C", 45),
    PERSONAL_BANKER("Personal Banker", 15, 2, "P", 60),
    EMPRESAS("Empresas", 20, 3, "E", 75),
    GERENCIA("Gerencia", 30, 4, "G", 90);

    private final String displayName;
    private final int averageTimeMinutes;
    private final int priority;
    private final String prefix;
    private final int maxWaitTimeMinutes;

    QueueType(String displayName, int averageTimeMinutes, int priority, String prefix, int maxWaitTimeMinutes) {
        this.displayName = displayName;
        this.averageTimeMinutes = averageTimeMinutes;
        this.priority = priority;
        this.prefix = prefix;
        this.maxWaitTimeMinutes = maxWaitTimeMinutes;
    }

    public String getDisplayName() { return displayName; }
    public int getAverageTimeMinutes() { return averageTimeMinutes; }
    public int getPriority() { return priority; }
    public String getPrefix() { return prefix; }
    public int getMaxWaitTimeMinutes() { return maxWaitTimeMinutes; }
}