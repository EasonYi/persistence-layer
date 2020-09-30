package com.kenshoo.pl.simulation;

import com.kenshoo.pl.entity.*;
import com.kenshoo.pl.entity.internal.EntitiesFetcher;
import com.kenshoo.pl.simulation.internal.*;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.jooq.lambda.Seq.seq;


/**
 * This utility helps to migrate from an old mutation code to Persistence Layer by running them side-by-side and
 * compare their results.
 *
 * <b>Terminology</b>
 * + ActualMutator - This is the old code.
 * + Simulated - The persistence layer commands.
 *
 * <b>Implementation</b>
 * This simulator runs both the old mutator along with the simulated commands (PL). However, the simulated commands
 * run without DbOutputGenerator, meaning, they don't really affect the database. PL simulated commands only run
 * enrichers, validators, etc.
 * After the real (old) mutator changed the DB, we fetch the final state of the entities to compare the actual
 * modification with the content of the simulated commands.
 *
 * <b>Limitations</b>
 * + One-to-many relations: Child commands are not supported.
 *
 * @param <E> EntityType
 */
public class DualRunSimulator<E extends EntityType<E>> {

    private final Collection<EntityField<E, ?>> inspectedFields;
    private final ChangeFlowConfig.Builder<E> flowToSimulate;
    private final PersistenceLayer<E> pl;
    private final ActualResultFetcher<E> actualResultFetcher;
    private final ResultComparator<E> resultComparator;

    public DualRunSimulator(PLContext plContext, ChangeFlowConfig.Builder<E> flowToSimulate, Collection<EntityField<E, ?>> inspectedFields) {
        this.inspectedFields = inspectedFields;
        this.flowToSimulate = flowToSimulate;
        this.actualResultFetcher = new ActualResultFetcher<>(new EntitiesFetcher(plContext.dslContext()), inspectedFields);
        this.pl = new PersistenceLayer<>(plContext);
        this.resultComparator = new ResultComparator<>(inspectedFields);
    }

    /**
     * Runs the real mutator and compares the actual DB affect with the values in the simulated commands.
     * Correlating each simulated command to an actual affect is done by a unique key.
     * The key doesn't have to be the primary key. It could be whatever unique key you want. But it must be
     * populated in the simulation commands.
     *
     * @param uniqueKey
     * @param databaseMutator
     * @param commandsToSimulate - must contain values for the unique key.
     * @return
     */
    public List<ComparisonMismatch<E>> runCreation(
            UniqueKey<E> uniqueKey,
            ActualDatabaseMutator<E> databaseMutator,
            Collection<? extends CreateEntityCommand<E>> commandsToSimulate) {

        final var plFlow = flowToSimulate
                .withoutOutputGenerators()
                .withOutputGenerator(new FakeAutoIncGenerator<>(uniqueKey.getEntityType()))
                .build();

        final var simulatedResults = seq(pl.create(commandsToSimulate, plFlow, uniqueKey).getChangeResults())
                .map(res -> new SimulatedResult<>(res.getCommand(), uniqueKey.createValue(res.getCommand()), res.getErrors()))
                .collect(toList());

        final var actualErrors = seq(databaseMutator.run()).toMap(__ -> __.getId());

        final var actualResults = actualResultFetcher.fetch(idsOf(simulatedResults), actualErrors, this::emptyOriginalState);

        return resultComparator.findMismatches(simulatedResults, actualResults);
    }

    public <ID extends Identifier<E>> List<ComparisonMismatch<E>> runUpdate(
            ActualDatabaseMutator<E> databaseMutator,
            Collection<? extends UpdateEntityCommand<E, ID>> commandsToSimulate) {

        final var originalStateRecorder = new InitialStateRecorder<>(inspectedFields);

        final var plFlow = flowToSimulate
                .withoutOutputGenerators()
                .withOutputGenerator(originalStateRecorder)
                .build();

        final var simulatedResults = seq(pl.update(commandsToSimulate, plFlow).getChangeResults())
                .map(r -> new SimulatedResult<>(r.getCommand(), r.getIdentifier(), r.getErrors()))
                .collect(toList());

        final var actualErrors = seq(databaseMutator.run()).toMap(__ -> __.getId());

        final var actualResults = actualResultFetcher.fetch(idsOf(simulatedResults), actualErrors, originalStateRecorder::get);

        return resultComparator.findMismatches(simulatedResults, actualResults);
    }

    private List<Identifier<E>> idsOf(Collection<SimulatedResult<E>> simulatedResults) {
        return seq(simulatedResults).map(SimulatedResult::getId).toList();
    }

    private Entity emptyState() {
        return new Entity() {
            @Override
            public boolean containsField(EntityField<?, ?> field) {
                return false;
            }

            @Override
            public <T> T get(EntityField<?, T> field) {
                return null;
            }
        };
    }

    private Entity emptyOriginalState(Identifier<E> id) {
        return emptyState();
    }
}
