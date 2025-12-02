package ua.fiv.borukva_inventory_backup.actor;

import org.apache.pekko.actor.typed.Behavior;
import org.apache.pekko.actor.typed.SupervisorStrategy;
import org.apache.pekko.actor.typed.javadsl.Behaviors;

import java.time.Duration;

public class DatabaseManagerSupervisor {

    public static Behavior<BActorMessages.Command> create() {
        return Behaviors.supervise(DatabaseManagerActor.create())
                .onFailure(SQLExceptionWrapper.class,
                        SupervisorStrategy.restartWithBackoff(
                                Duration.ofMillis(200), // min backoff
                                Duration.ofSeconds(10), // max backoff
                                0.1 // random factor
                        ).withMaxRestarts(5)
                );
    }
}