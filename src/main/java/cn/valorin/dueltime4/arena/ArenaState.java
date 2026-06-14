package cn.valorin.dueltime4.arena;

public enum ArenaState {
    WAITING,
    STARTING,
    IN_PROGRESS,
    ENDING,
    DISABLED;

    public boolean canTransitionTo(ArenaState next) {
        return switch (this) {
            case WAITING   -> next == STARTING || next == DISABLED;
            case STARTING  -> next == IN_PROGRESS;
            case IN_PROGRESS -> next == ENDING;
            case ENDING    -> next == WAITING || next == DISABLED;
            case DISABLED  -> next == WAITING;
        };
    }
}
