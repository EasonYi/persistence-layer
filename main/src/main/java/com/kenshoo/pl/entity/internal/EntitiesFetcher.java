package com.kenshoo.pl.entity.internal;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.kenshoo.jooq.DataTable;
import com.kenshoo.jooq.QueryExtension;
import com.kenshoo.jooq.TempTableHelper;
import com.kenshoo.jooq.TempTableResource;
import com.kenshoo.pl.data.ImpersonatorTable;
import com.kenshoo.pl.entity.UniqueKey;
import com.kenshoo.pl.entity.*;
import com.kenshoo.pl.entity.internal.fetch.*;
import org.jooq.*;
import org.jooq.lambda.Seq;

import java.util.*;
import java.util.function.Consumer;

import static com.kenshoo.pl.entity.Feature.FetchMany;
import static org.jooq.lambda.Seq.seq;


public class EntitiesFetcher {

    private final DSLContext dslContext;
    private final FeatureSet features;
    private final OldEntityFetcher oldEntityFetcher;

    public EntitiesFetcher(DSLContext dslContext) {
        this(dslContext, FeatureSet.EMPTY);
    }

    public EntitiesFetcher(DSLContext dslContext, FeatureSet features) {
        this.dslContext = dslContext;
        this.features = features;
        this.oldEntityFetcher = new OldEntityFetcher(dslContext);
    }

    public <E extends EntityType<E>> Map<Identifier<E>, CurrentEntityState> fetchEntitiesByIds(final Collection<? extends Identifier<E>> ids,
                                                                                               final EntityField<?, ?>... fieldsToFetchArgs) {
        return fetchEntitiesByIds(ids, ImmutableList.copyOf(fieldsToFetchArgs));
    }

    public <E extends EntityType<E>> Map<Identifier<E>, CurrentEntityState> fetchEntitiesByIds(final Collection<? extends Identifier<E>> ids,
                                                                                               final Collection<? extends EntityField<?, ?>> fieldsToFetch) {
        if (!features.isEnabled(FetchMany)) {
            return oldEntityFetcher.fetchEntitiesByIds(ids, fieldsToFetch);
        }

        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }

        final UniqueKey<E> uniqueKey = ids.iterator().next().getUniqueKey();
        final AliasedKey<E> aliasedKey = new AliasedKey<>(uniqueKey);

        return fetchEntities(uniqueKey.getEntityType().getPrimaryTable(), aliasedKey, fieldsToFetch, query -> query.whereIdsIn(ids));
    }

    public List<CurrentEntityState> fetch(final EntityType<?> entityType,
                                          final PLCondition plCondition,
                                          final EntityField<?, ?>... fieldsToFetch) {
        return oldEntityFetcher.fetch(entityType, plCondition, fieldsToFetch);
    }

    public List<CurrentEntityState> fetch(final EntityType<?> entityType,
                                          final Collection<? extends Identifier<?>> ids,
                                          final PLCondition plCondition,
                                          final EntityField<?, ?>... fieldsToFetch) {
        return oldEntityFetcher.fetch(entityType, ids, plCondition, fieldsToFetch);
    }

    public <E extends EntityType<E>> Map<Identifier<E>, CurrentEntityState> fetchEntitiesByForeignKeys(E entityType, UniqueKey<E> foreignUniqueKey, Collection<? extends Identifier<E>> keys, Collection<EntityField<?, ?>> fieldsToFetch) {
        if (!features.isEnabled(FetchMany)) {
            return oldEntityFetcher.fetchEntitiesByForeignKeys(entityType, foreignUniqueKey, keys, fieldsToFetch);
        }

        try (final TempTableResource<ImpersonatorTable> foreignKeysTable = createForeignKeysTable(entityType.getPrimaryTable(), foreignUniqueKey, keys)) {
            final AliasedKey<E> aliasedKey = new AliasedKey<>(foreignUniqueKey, foreignKeysTable);
            final DataTable startingTable = foreignKeysTable.getTable();

            return fetchEntities(startingTable, aliasedKey, fieldsToFetch, noMoreConditions());

        }
    }

    private <E extends EntityType<E>> Map<Identifier<E>, CurrentEntityState> fetchEntities(
            final DataTable startingTable,
            final AliasedKey<E> aliasedKey,
            final Collection<? extends EntityField<?, ?>> fieldsToFetch,
            Consumer<QueryBuilder<E>> queryModifier
    ) {
        final ExecutionPlan executionPlan = new ExecutionPlan(startingTable, fieldsToFetch);
        final ExecutionPlan.OneToOnePlan oneToOnePlan = executionPlan.getOneToOnePlan();

        final QueryBuilder<E> mainQueryBuilder = new QueryBuilder<E>(dslContext).selecting(selectFieldsOf(oneToOnePlan.getFields(), aliasedKey))
                .from(startingTable)
                .innerJoin(oneToOnePlan.getPaths())
                .leftJoin(oneToOnePlan.getSecondaryTableRelations());
        queryModifier.accept(mainQueryBuilder);

        final Map<Identifier<E>, CurrentEntityState> entities = fetchMainEntities(aliasedKey, oneToOnePlan, mainQueryBuilder);

        executionPlan.getManyToOnePlans().forEach(plan -> {
            final QueryBuilder<E> subQueryBuilder = new QueryBuilder<E>(dslContext).selecting(selectFieldsOf(plan.getFields(), aliasedKey))
                    .from(startingTable)
                    .innerJoin(plan.getPath());
            queryModifier.accept(subQueryBuilder);

            fetchAndPopulateSubEntities(aliasedKey, entities, plan, subQueryBuilder);
        });

        return entities;
    }

    public <E extends EntityType<E>, PE extends PartialEntity, ID extends Identifier<E>> Map<ID, PE> fetchPartialEntities(E entityType, Collection<ID> keys, final Class<PE> entityIface) {
        return oldEntityFetcher.fetchPartialEntities(entityType, keys, entityIface);
    }

    public <E extends EntityType<E>, PE extends PartialEntity> List<PE> fetchByCondition(E entityType, Condition condition, final Class<PE> entityIface) {
        return oldEntityFetcher.fetchByCondition(entityType, condition, entityIface);
    }

    private <E extends EntityType<E>, SUB extends EntityType<SUB>> void fetchAndPopulateSubEntities(AliasedKey<E> aliasedKey, Map<Identifier<E>, CurrentEntityState> entities, ExecutionPlan.ManyToOnePlan<SUB> plan, QueryBuilder<E> queryBuilder) {
        try (QueryExtension<SelectJoinStep<Record>> queryExtender = queryBuilder.build()) {
            Map<Identifier<E>, List<FieldsValueMap<SUB>>> multiValuesMap = fetchMultiValuesMap(queryExtender.getQuery(), aliasedKey, plan.getFields());
            multiValuesMap.forEach((Identifier<E> id, List<FieldsValueMap<SUB>> multiValues) -> {
                final SUB subEntityType = entityTypeOf(plan.getFields());
                ((CurrentEntityState) entities.get(id)).add(subEntityType, multiValues);
            });
        }
    }

    private <E extends EntityType<E>> Map<Identifier<E>, CurrentEntityState> fetchMainEntities(AliasedKey<E> aliasedKey, ExecutionPlan.OneToOnePlan oneToOnePlan, QueryBuilder<E> queryBuilder) {
        try (QueryExtension<SelectJoinStep<Record>> queryExtender = queryBuilder.build()) {
            return fetchEntitiesMap(queryExtender.getQuery(), aliasedKey, oneToOnePlan.getFields());
        }
    }

    private <E extends EntityType<E>> Map<Identifier<E>, CurrentEntityState> fetchEntitiesMap(ResultQuery<Record> query, AliasedKey<E> aliasedKey, List<? extends EntityField<?, ?>> fields) {
        return query.fetchMap(record -> RecordReader.createKey(record, aliasedKey), record -> RecordReader.createEntity(record, fields));
    }

    private <MAIN extends EntityType<MAIN>, SUB extends EntityType<SUB>> Map<Identifier<MAIN>, List<FieldsValueMap<SUB>>> fetchMultiValuesMap(ResultQuery<Record> query, AliasedKey<MAIN> aliasedKey, List<? extends EntityField<SUB, ?>> fields) {
        final Map<Identifier<MAIN>, List<FieldsValueMap<SUB>>> multiValuesMap = new HashMap<>();
        query.fetchInto(record -> {
            Identifier<MAIN> key = RecordReader.createKey(record, aliasedKey);
            multiValuesMap.computeIfAbsent(key, k -> Lists.newArrayList());
            multiValuesMap.get(key).add(RecordReader.createFieldsValueMap(record, fields));
        });
        return multiValuesMap;
    }

    public <E extends EntityType<E>> TempTableResource<ImpersonatorTable> createForeignKeysTable(final DataTable primaryTable, final UniqueKey<E> foreignUniqueKey, final Collection<? extends Identifier<E>> keys) {
        ImpersonatorTable impersonatorTable = new ImpersonatorTable(primaryTable);
        foreignUniqueKey.getTableFields().forEach(impersonatorTable::createField);

        return TempTableHelper.tempInMemoryTable(dslContext, impersonatorTable, batchBindStep -> {
                    for (Identifier<E> key : keys) {
                        EntityField<E, ?>[] keyFields = foreignUniqueKey.getFields();
                        List<Object> values = new ArrayList<>();
                        for (EntityField<E, ?> field : keyFields) {
                            addToValues(key, field, values);
                        }
                        batchBindStep.bind(values.toArray());
                    }
                }
        );
    }

    private <E extends EntityType<E>, T> void addToValues(Identifier<E> key, EntityField<E, T> field, List<Object> values) {
        field.getDbAdapter().getDbValues(key.get(field)).forEach(values::add);
    }

    private <E extends EntityType<E>> List<SelectField<?>> selectFieldsOf(Collection<? extends EntityField<?, ?>> fields, AliasedKey<E> aliasedKey) {
        return dbFieldsOf(fields).concat(aliasedKey.aliasedFields()).toList();
    }

    private Seq<SelectField<?>> dbFieldsOf(Collection<? extends EntityField<?, ?>> fields) {
        return seq(fields).flatMap(field -> field.getDbAdapter().getTableFields());
    }

    private <E extends EntityType<E>> E entityTypeOf(Collection<? extends EntityField<E, ?>> fields) {
        return (E) seq(fields).findFirst().get().getEntityType();
    }

    private <E extends EntityType<E>> Consumer<QueryBuilder<E>> noMoreConditions() {
        return __ -> {
        };
    }
}